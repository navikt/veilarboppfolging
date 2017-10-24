package no.nav.fo.veilarboppfolging.services;

import lombok.*;
import no.nav.apiapp.feil.UlovligHandling;
import no.nav.apiapp.security.PepClient;
import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarbaktivitet.domain.arena.ArenaAktivitetDTO;
import no.nav.sbl.jdbc.Transactor;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.*;
import no.nav.fo.veilarboppfolging.db.OppfolgingRepository;
import no.nav.fo.veilarboppfolging.domain.*;
import no.nav.fo.veilarboppfolging.utils.DateUtils;
import no.nav.fo.veilarboppfolging.utils.StringUtils;
import no.nav.fo.veilarboppfolging.vilkar.VilkarService;
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

import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.*;

import static java.lang.System.currentTimeMillis;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetStatus.AVBRUTT;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetStatus.FULLFORT;
import static no.nav.fo.veilarboppfolging.domain.VilkarStatus.GODKJENT;
import static no.nav.fo.veilarboppfolging.services.ArenaUtils.erUnderOppfolging;
import static no.nav.fo.veilarboppfolging.services.ArenaUtils.kanSettesUnderOppfolging;
import static no.nav.fo.veilarboppfolging.vilkar.VilkarService.VilkarType.PRIVAT;
import static no.nav.fo.veilarboppfolging.vilkar.VilkarService.VilkarType.UNDER_OPPFOLGING;
import static org.slf4j.LoggerFactory.getLogger;

public class OppfolgingResolver {

    private static final Logger LOG = getLogger(OppfolgingResolver.class);
    private static final String AKTIV_YTELSE_STATUS = "Aktiv";

    private String fnr;
    private OppfolgingResolverDependencies deps;

    private String aktorId;
    private Oppfolging oppfolging;
    private HentOppfoelgingsstatusResponse statusIArena;
    private Boolean reservertIKrr;
    private WSHentYtelseskontraktListeResponse ytelser;
    private List<ArenaAktivitetDTO> arenaAktiviteter;

    OppfolgingResolver(OppfolgingResolverDependencies deps) {
        this.deps = deps;
    }


    OppfolgingResolver(String fnr, OppfolgingResolverDependencies deps) {
        deps.getPepClient().sjekkLeseTilgangTilFnr(fnr);

        this.fnr = fnr;
        this.deps = deps;

        this.aktorId = deps.getAktorService().getAktorId(fnr)
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke aktør for fnr: " + fnr));
        this.oppfolging = hentOppfolging();
    }

    void reloadOppfolging() {
        oppfolging = hentOppfolging();
    }

    void sjekkStatusIArenaOgOppdaterOppfolging() {
        if (!oppfolging.isUnderOppfolging()) {
            hentOppfolgingstatusFraArena();
            if(erUnderOppfolging(statusIArena.getFormidlingsgruppeKode(), statusIArena.getServicegruppeKode())){
                deps.getOppfolgingRepository().startOppfolgingHvisIkkeAlleredeStartet(aktorId);
                reloadOppfolging();
            }
        }
    }

    void sjekkNyesteVilkarOgOppdaterOppfolging(String hash, VilkarStatus vilkarStatus) {
        Brukervilkar gjeldendeVilkar = getNyesteVilkar();
        if (gjeldendeVilkar.getHash().equals(hash)) {
            deps.getOppfolgingRepository().opprettBrukervilkar(
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
        String vilkarTekst = deps.getVilkarService().getVilkar(oppfolging.isUnderOppfolging() ? UNDER_OPPFOLGING : PRIVAT, null);
        return new Brukervilkar()
            .setTekst(vilkarTekst)
            .setHash(DigestUtils.sha256Hex(vilkarTekst));
    }

    List<Brukervilkar> getHistoriskeVilkar() {
        return deps.getOppfolgingRepository().hentHistoriskeVilkar(aktorId);
    }

    boolean maVilkarBesvares() {
        return ofNullable(oppfolging.getGjeldendeBrukervilkar())
            .filter(brukervilkar -> GODKJENT.equals(brukervilkar.getVilkarstatus()))
            .map(Brukervilkar::getHash)
            .map(brukerVilkar -> !brukerVilkar.equals(getNyesteVilkar().getHash()))
            .orElse(true);
    }

    List<MalData> getMalList() {
        return deps.getOppfolgingRepository().hentMalList(aktorId);
    }

    MalData oppdaterMal(String mal, String endretAv) {
        MalData malData = new MalData()
            .setAktorId(aktorId)
            .setMal(mal)
            .setEndretAv(StringUtils.of(endretAv).orElse(aktorId))
            .setDato(new Timestamp(currentTimeMillis()));
        deps.getOppfolgingRepository().opprettMal(malData);
        return hentOppfolging().getGjeldendeMal();
    }

    void slettMal() {
        // https://confluence.adeo.no/pages/viewpage.action?pageId=229941929
        Oppfolging oppfolging = getOppfolging();
        if (oppfolging.isUnderOppfolging()) {
            throw new UlovligHandling();
        } else {
            Date sisteSluttDatoEller1970 = oppfolging
                    .getOppfolgingsperioder()
                    .stream()
                    .map(Oppfolgingsperiode::getSluttDato)
                    .filter(Objects::nonNull)
                    .max(Date::compareTo)
                    .orElseGet(() -> new Date(0));
            deps.getOppfolgingRepository().slettMalForAktorEtter(aktorId, sisteSluttDatoEller1970);
        }
    }

    Oppfolging getOppfolging() {
        return oppfolging;
    }

    String getAktorId() {
        return aktorId;
    }

    boolean reservertIKrr() {
        if (reservertIKrr == null) {
            sjekkReservasjonIKrrOgOppdaterOppfolging();
        }
        return reservertIKrr;
    }

    boolean manuell() {
        return ofNullable(oppfolging.getGjeldendeManuellStatus())
            .map(ManuellStatus::isManuell)
            .orElse(false);
    }

    boolean getKanSettesUnderOppfolging() {
        if (oppfolging.isUnderOppfolging()) {
            return false;
        }
        if (statusIArena == null) {
            hentOppfolgingstatusFraArena();
        }
        return kanSettesUnderOppfolging(statusIArena.getFormidlingsgruppeKode(), statusIArena.getServicegruppeKode());
    }

    void startOppfolging() {
        deps.getOppfolgingRepository().startOppfolgingHvisIkkeAlleredeStartet(aktorId);
        oppfolging = hentOppfolging();
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
        return oppfolging.isUnderOppfolging()
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

        if(Optional.ofNullable(oppfolging.getGjeldendeEskaleringsvarsel()).isPresent()){
            stoppEskalering("Eskalering avsluttet fordi oppfølging ble avsluttet");
        }

        deps.getOppfolgingRepository().avsluttOppfolging(aktorId, veileder, begrunnelse);
    }

    private Oppfolging hentOppfolging() {
        return deps.getOppfolgingRepository().hentOppfolging(aktorId)
            .orElseGet(() -> deps.getOppfolgingRepository().opprettOppfolging(aktorId));
    }

    void startEskalering(String begrunnelse, long tilhorendeDialogId){
        String veilederId = SubjectHandler.getSubjectHandler().getUid();
        deps.getTransactor().inTransaction(() -> {
            deps.getOppfolgingRepository().startEskalering(aktorId, veilederId, begrunnelse, tilhorendeDialogId);
            deps.getEskaleringsvarselService().sendEskaleringsvarsel(aktorId, tilhorendeDialogId);
        });
    }

    void stoppEskalering(String begrunnelse) {
        String veilederId = SubjectHandler.getSubjectHandler().getUid();
        deps.getOppfolgingRepository().stoppEskalering(aktorId, veilederId, begrunnelse);
    }

    @SneakyThrows
    private void hentOppfolgingstatusFraArena() {
        val hentOppfolgingstatusRequest = new HentOppfoelgingsstatusRequest();
        hentOppfolgingstatusRequest.setPersonidentifikator(fnr);
        this.statusIArena = deps.getOppfoelgingPortType().hentOppfoelgingsstatus(hentOppfolgingstatusRequest);
    }

    private void sjekkReservasjonIKrrOgOppdaterOppfolging() {
        if (oppfolging.isUnderOppfolging()) {
            this.reservertIKrr = sjekkKrr();
            if (!manuell() && reservertIKrr) {
                deps.getOppfolgingRepository().opprettManuellStatus(
                    new ManuellStatus(
                        oppfolging.getAktorId(),
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
    public static class OppfolgingResolverDependencies {

        @Inject
        private PepClient pepClient;

        @Inject
        private Transactor transactor;

        @Inject
        private AktorService aktorService;

        @Inject
        private OppfolgingRepository oppfolgingRepository;

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
