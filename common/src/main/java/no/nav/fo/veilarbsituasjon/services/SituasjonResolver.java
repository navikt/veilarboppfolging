package no.nav.fo.veilarbsituasjon.services;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.apiapp.feil.UlovligHandling;
import no.nav.apiapp.security.PepClient;
import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.fo.veilarbaktivitet.domain.arena.ArenaAktivitetDTO;
import no.nav.fo.veilarbsituasjon.db.SituasjonRepository;
import no.nav.fo.veilarbsituasjon.domain.*;
import no.nav.fo.veilarbsituasjon.utils.DateUtils;
import no.nav.fo.veilarbsituasjon.utils.StringUtils;
import no.nav.fo.veilarbsituasjon.vilkar.VilkarService;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.DigitalKontaktinformasjonV1;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.HentDigitalKontaktinformasjonKontaktinformasjonIkkeFunnet;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.HentDigitalKontaktinformasjonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.WSKontaktinformasjon;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.WSHentDigitalKontaktinformasjonRequest;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.WSHentDigitalKontaktinformasjonResponse;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingsstatusRequest;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingsstatusResponse;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.YtelseskontraktV3;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.informasjon.ytelseskontrakt.WSYtelseskontrakt;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.WSHentYtelseskontraktListeRequest;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.WSHentYtelseskontraktListeResponse;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
    private HentOppfoelgingsstatusResponse statusIArena;
    private Boolean reservertIKrr;
    private WSHentYtelseskontraktListeResponse ytelser;
    private List<ArenaAktivitetDTO> arenaAktiviteter;

    SituasjonResolver(String fnr, SituasjonResolverDependencies deps) {
        deps.getPepClient().sjekkTilgangTilFnr(fnr);

        this.fnr = fnr;
        this.deps = deps;

        this.aktorId = ofNullable(deps.getAktoerIdService().findAktoerId(fnr))
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke aktør for fnr: " + fnr));
        this.situasjon = hentSituasjon();
    }

    void reloadSituasjon() {
        situasjon = hentSituasjon();
    }

    @Transactional
    void sjekkStatusIArenaOgOppdaterSituasjon() {
        if (!situasjon.isOppfolging()) {
            hentOppfolgingstatusFraArena();
            if(erUnderOppfolging(statusIArena.getFormidlingsgruppeKode(), statusIArena.getServicegruppeKode())){
                deps.getSituasjonRepository().startOppfolgingHvisIkkeAlleredeStartet(aktorId);
                reloadSituasjon();
            }
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

    void slettMal() {
        // https://confluence.adeo.no/pages/viewpage.action?pageId=229941929
        Situasjon situasjon = getSituasjon();
        if (situasjon.isOppfolging()) {
            throw new UlovligHandling();
        } else {
            Date sisteSluttDatoEller1970 = situasjon
                    .getOppfolgingsperioder()
                    .stream()
                    .map(Oppfolgingsperiode::getSluttDato)
                    .filter(Objects::nonNull)
                    .max(Date::compareTo)
                    .orElseGet(() -> new Date(0));
            deps.getSituasjonRepository().slettMalForAktorEtter(aktorId, sisteSluttDatoEller1970);
        }
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
            hentOppfolgingstatusFraArena();
        }
        return kanSettesUnderOppfolging(statusIArena.getFormidlingsgruppeKode(), statusIArena.getServicegruppeKode());
    }

    @Transactional
    void startOppfolging() {
        deps.getSituasjonRepository().startOppfolgingHvisIkkeAlleredeStartet(aktorId);
        situasjon = hentSituasjon();
    }

    boolean erUnderOppfolgingIArena() {
        if (statusIArena == null) {
            hentOppfolgingstatusFraArena();
        }
        return erUnderOppfolging(statusIArena.getFormidlingsgruppeKode(), statusIArena.getServicegruppeKode());
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
        if (statusIArena == null) {
            hentOppfolgingstatusFraArena();
        }

        return DateUtils.getDate(statusIArena.getInaktiveringsdato());
    }

    void avsluttOppfolging(String veileder, String begrunnelse) {
        if (!kanAvslutteOppfolging()) {
            return;
        }

        if(Optional.ofNullable(situasjon.getGjeldendeEskaleringsvarsel()).isPresent()){
            stoppEskalering("Eskalering avsluttet fordi oppfølging ble avsluttet");
        }

        deps.getSituasjonRepository().avsluttOppfolging(aktorId, veileder, begrunnelse);
    }

    private Situasjon hentSituasjon() {
        return deps.getSituasjonRepository().hentSituasjon(aktorId)
            .orElseGet(() -> deps.getSituasjonRepository().opprettSituasjon(aktorId));
    }

    void startEskalering(String begrunnelse, long tilhorendeDialogId, String dialogUrl){
        String veilederId = SubjectHandler.getSubjectHandler().getUid();
        deps.getSituasjonRepository().startEskalering(aktorId, veilederId, begrunnelse, tilhorendeDialogId);
        deps.getEskaleringsvarselService().sendEskaleringsvarsel(aktorId, dialogUrl);
    }

    void stoppEskalering(String begrunnelse) {
        String veilederId = SubjectHandler.getSubjectHandler().getUid();
        deps.getSituasjonRepository().stoppEskalering(aktorId, veilederId, begrunnelse);
    }

    @SneakyThrows
    private void hentOppfolgingstatusFraArena() {
        val hentOppfolgingstatusRequest = new HentOppfoelgingsstatusRequest();
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
                        "Reservert og under oppfølging",
                        KodeverkBruker.SYSTEM,
                        null
                    )
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
        private PepClient pepClient;

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

        @Inject
        private EskaleringsvarselService eskaleringsvarselService;
    }
}
