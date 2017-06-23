package no.nav.fo.veilarbsituasjon.services;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.fo.veilarbaktivitet.domain.arena.ArenaAktivitetDTO;
import no.nav.fo.veilarbsituasjon.db.SituasjonRepository;
import no.nav.fo.veilarbsituasjon.domain.*;
import no.nav.fo.veilarbsituasjon.utils.StringUtils;
import no.nav.fo.veilarbsituasjon.vilkar.VilkarService;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.DigitalKontaktinformasjonV1;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.HentDigitalKontaktinformasjonKontaktinformasjonIkkeFunnet;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.HentDigitalKontaktinformasjonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.WSKontaktinformasjon;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.WSHentDigitalKontaktinformasjonRequest;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.WSHentDigitalKontaktinformasjonResponse;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingsstatusRequest;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingsstatusResponse;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.YtelseskontraktV3;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.informasjon.ytelseskontrakt.WSYtelseskontrakt;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.WSHentYtelseskontraktListeRequest;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.WSHentYtelseskontraktListeResponse;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static java.lang.System.currentTimeMillis;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetStatus.AVBRUTT;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetStatus.FULLFORT;
import static no.nav.fo.veilarbsituasjon.domain.VilkarStatus.GODKJENT;
import static no.nav.fo.veilarbsituasjon.services.ArenaUtils.erUnderOppfolging;
import static no.nav.fo.veilarbsituasjon.services.ArenaUtils.kanSettesUnderOppfolging;
import static no.nav.fo.veilarbsituasjon.vilkar.VilkarService.VilkarType.PRIVAT;
import static no.nav.fo.veilarbsituasjon.vilkar.VilkarService.VilkarType.UNDER_OPPFOLGING;
import static org.slf4j.LoggerFactory.getLogger;

public class SituasjonResolver {

    private static final Logger LOG = getLogger(SituasjonResolver.class);
    private static final String AKTIV_YTELSE_STATUS = "Aktiv";

    private String fnr;
    private SituasjonResolverDependencies deps;

    private String aktorId;
    private Situasjon situasjon;
    private WSHentOppfoelgingsstatusResponse statusIArena;
    private Boolean reservertIKrr;
    private WSHentYtelseskontraktListeResponse ytelser;
    private List<ArenaAktivitetDTO> arenaAktiviteter;

    SituasjonResolver(String fnr, SituasjonResolverDependencies deps) {
        this.fnr = fnr;
        this.deps = deps;

        this.aktorId = ofNullable(deps.getAktoerIdService().findAktoerId(fnr))
                .orElseThrow(() -> new IllegalArgumentException("Fant ikke aktør for fnr: " + fnr));
        this.situasjon = hentSituasjon();
    }

    void sjekkStatusIArenaOgOppdaterSituasjon() {
        if (!situasjon.isOppfolging()) {
            sjekkArena();
            deps.getSituasjonRepository().oppdaterSituasjon(
                    situasjon.setOppfolging(
                            erUnderOppfolging(statusIArena)
                    )
            );
        }
    }

    void sjekkNyesteVilkarOgOppdaterSituasjon(String hash, VilkarStatus vilkarStatus) {
        Brukervilkar gjeldendeVilkar = getNyesteVilkar();
        if (gjeldendeVilkar.getHash().equals(hash)) {
            deps.getSituasjonRepository().opprettBrukervilkar(
                    new Brukervilkar(
                            aktorId,
                            new Timestamp(currentTimeMillis()),
                            vilkarStatus,
                            gjeldendeVilkar.getTekst(),
                            hash
                    ));
        }
    }

    Brukervilkar getNyesteVilkar() {
        String vilkarTekst = deps.getVilkarService().getVilkar(situasjon.isOppfolging() ? UNDER_OPPFOLGING : PRIVAT, null);
        return new Brukervilkar()
                .setTekst(vilkarTekst)
                .setHash(DigestUtils.sha256Hex(vilkarTekst));
    }

    List<Brukervilkar> getHistoriskeVilkar() {
        return deps.getSituasjonRepository().hentHistoriskeVilkar(aktorId);
    }

    boolean maVilkarBesvares() {
        return ofNullable(situasjon.getGjeldendeBrukervilkar())
                .filter(brukervilkar -> GODKJENT.equals(brukervilkar.getVilkarstatus()))
                .map(Brukervilkar::getHash)
                .map(brukerVilkar -> !brukerVilkar.equals(getNyesteVilkar().getHash()))
                .orElse(true);
    }

    List<MalData> getMalList() {
        return deps.getSituasjonRepository().hentMalList(aktorId);
    }

    MalData oppdaterMal(String mal, String endretAv) {
        MalData malData = new MalData()
                .setAktorId(aktorId)
                .setMal(mal)
                .setEndretAv(StringUtils.of(endretAv).orElse(aktorId))
                .setDato(new Timestamp(currentTimeMillis()));
        deps.getSituasjonRepository().opprettMal(malData);
        return hentSituasjon().getGjeldendeMal();
    }

    Situasjon getSituasjon() {
        return situasjon;
    }

    String getAktorId() {
        return aktorId;
    }

    boolean reservertIKrr() {
        if (reservertIKrr == null) {
            sjekkReservasjonIKrrOgOppdaterSituasjon();
        }
        return reservertIKrr;
    }

    boolean manuell() {
        return ofNullable(situasjon.getGjeldendeStatus())
                .map(Status::isManuell)
                .orElse(false);
    }

    boolean getKanSettesUnderOppfolging() {
        if (situasjon.isOppfolging()) {
            return false;
        }
        if (statusIArena == null) {
            sjekkArena();
        }
        return kanSettesUnderOppfolging(statusIArena);
    }

    void startOppfolging() {
        deps.getSituasjonRepository().oppdaterSituasjon(situasjon.setOppfolging(true));
        situasjon = hentSituasjon();
    }

    boolean erUnderOppfolgingIArena() {
        if (statusIArena == null) {
            sjekkArena();
        }
        return erUnderOppfolging(statusIArena);
    }

    boolean harPagaendeYtelse() {
        if (ytelser == null) {
            hentYtelseskontrakt();
        }
        return ytelser.getYtelseskontraktListe()
                .stream()
                .map(WSYtelseskontrakt::getStatus)
                .anyMatch(AKTIV_YTELSE_STATUS::equals);
    }

    boolean harAktiveTiltak() {
        if (arenaAktiviteter == null) {
            hentArenaAktiviteter();
        }
        return arenaAktiviteter
                .stream()
                .map(ArenaAktivitetDTO::getStatus)
                .anyMatch(status -> status != AVBRUTT && status != FULLFORT);
    }

    boolean kanAvslutteOppfolging() {
        return situasjon.isOppfolging()
                && !erUnderOppfolgingIArena()
                && !harPagaendeYtelse()
                && !harAktiveTiltak();
    }

    Date getInaktiveringsDato() {
        // TODO: Erstatt dette når inaktiveringsDato finnes i arena
        LocalDate date = LocalDate.now().minusMonths(2);
        Date toManedSiden = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
        return fnr.equals("***REMOVED***") ? toManedSiden : new Date();
    }

    void avsluttOppfolging(Oppfolgingsperiode oppfolgingsperiode) {
        deps.getSituasjonRepository().oppdaterSituasjon(aktorId, false);
        deps.getSituasjonRepository().opprettOppfolgingsperiode(oppfolgingsperiode);
    }

    private Situasjon hentSituasjon() {
        return deps.getSituasjonRepository().hentSituasjon(aktorId)
                .orElseGet(() -> deps.getSituasjonRepository().opprettSituasjon(new Situasjon().setAktorId(aktorId)));
    }

    @SneakyThrows
    private void sjekkArena() {
        val hentOppfolgingstatusRequest = new WSHentOppfoelgingsstatusRequest();
        hentOppfolgingstatusRequest.setPersonidentifikator(fnr);
        this.statusIArena = deps.getOppfoelgingPortType().hentOppfoelgingsstatus(hentOppfolgingstatusRequest);
    }

    private void sjekkReservasjonIKrrOgOppdaterSituasjon() {
        if (situasjon.isOppfolging()) {
            this.reservertIKrr = sjekkKrr();
            if (!manuell() && reservertIKrr) {
                deps.getSituasjonRepository().opprettStatus(
                        new Status(
                                situasjon.getAktorId(),
                                true,
                                new Timestamp(currentTimeMillis()),
                                "Reservert og under oppfølging")
                );
            }
        } else {
            this.reservertIKrr = false;
        }
    }

    @SneakyThrows
    private boolean sjekkKrr() {
        val req = new WSHentDigitalKontaktinformasjonRequest().withPersonident(fnr);
        try {
            return of(deps.getDigitalKontaktinformasjonV1().hentDigitalKontaktinformasjon(req))
                    .map(WSHentDigitalKontaktinformasjonResponse::getDigitalKontaktinformasjon)
                    .map(WSKontaktinformasjon::getReservasjon)
                    .map("true"::equalsIgnoreCase)
                    .orElse(false);
        } catch (HentDigitalKontaktinformasjonKontaktinformasjonIkkeFunnet | HentDigitalKontaktinformasjonPersonIkkeFunnet e) {
            LOG.warn(e.getMessage(), e);
            return true;
        }
    }

    @SneakyThrows
    private void hentYtelseskontrakt() {
        val wsHentYtelseskontraktListeRequest = new WSHentYtelseskontraktListeRequest();
        wsHentYtelseskontraktListeRequest.setPersonidentifikator(fnr);
        this.ytelser = deps.getYtelseskontraktV3().hentYtelseskontraktListe(wsHentYtelseskontraktListeRequest);
    }

    private void hentArenaAktiviteter() {
        this.arenaAktiviteter = deps.getVeilarbaktivtetService().hentArenaAktiviteter(fnr);
    }

    @Component
    @Getter
    public static class SituasjonResolverDependencies {

        @Inject
        private AktoerIdService aktoerIdService;

        @Inject
        private SituasjonRepository situasjonRepository;

        @Inject
        private OppfoelgingPortType oppfoelgingPortType;

        @Inject
        private DigitalKontaktinformasjonV1 digitalKontaktinformasjonV1;

        @Inject
        private VilkarService vilkarService;

        @Inject
        private YtelseskontraktV3 ytelseskontraktV3;

        @Inject
        private VeilarbaktivtetService veilarbaktivtetService;
    }
}
