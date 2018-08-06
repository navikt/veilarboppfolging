package no.nav.fo.veilarboppfolging.services;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.feil.Feil;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.config.RemoteFeatureConfig;
import no.nav.fo.veilarboppfolging.db.NyeBrukereFeedRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingRepository;
import no.nav.fo.veilarboppfolging.domain.*;
import no.nav.fo.veilarboppfolging.utils.FnrUtils;
import no.nav.metrics.MetricsFactory;
import no.nav.metrics.Timer;
import no.nav.tjeneste.virksomhet.behandlearbeidssoeker.v1.binding.*;
import no.nav.tjeneste.virksomhet.behandlearbeidssoeker.v1.informasjon.Brukerident;
import no.nav.tjeneste.virksomhet.behandlearbeidssoeker.v1.meldinger.AktiverBrukerRequest;
import no.nav.tjeneste.virksomhet.behandlearbeidssoeker.v1.meldinger.ReaktiverBrukerForenkletRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.core.Response;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.Predicates.instanceOf;
import static java.util.Optional.ofNullable;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static no.nav.fo.veilarboppfolging.domain.Innsatsgruppe.STANDARD_INNSATS;

@Slf4j
@Component
public class AktiverBrukerService {


    private AktorService aktorService;
    private final BehandleArbeidssoekerV1 behandleArbeidssoekerV1;
    private RemoteFeatureConfig.RegistreringFeature registreringFeature;
    private RemoteFeatureConfig.OpprettBrukerIArenaFeature opprettBrukerIArenaFeature;

    private OppfolgingRepository oppfolgingRepository;
    private NyeBrukereFeedRepository nyeBrukereFeedRepository;
    private PepClient pepClient;


    public AktiverBrukerService(OppfolgingRepository oppfolgingRepository,
                                AktorService aktorService,
                                BehandleArbeidssoekerV1 BehandleArbeidssoekerV1,
                                RemoteFeatureConfig.OpprettBrukerIArenaFeature opprettBrukerIArenaFeature,
                                RemoteFeatureConfig.RegistreringFeature registreringFeature,
                                NyeBrukereFeedRepository nyeBrukereFeedRepository,
                                PepClient pepClient
    ) {
        this.aktorService = aktorService;
        this.behandleArbeidssoekerV1 = BehandleArbeidssoekerV1;
        this.opprettBrukerIArenaFeature = opprettBrukerIArenaFeature;
        this.registreringFeature = registreringFeature;
        this.oppfolgingRepository = oppfolgingRepository;
        this.nyeBrukereFeedRepository = nyeBrukereFeedRepository;
        this.pepClient = pepClient;
    }

    @Transactional
    public void aktiverBruker(AktiverArbeidssokerData bruker) {
        String fnr = ofNullable(bruker.getFnr())
                .map(f -> f.getFnr())
                .orElse("");

        pepClient.sjekkLeseTilgangTilFnr(fnr);

        if (!registreringFeature.erAktiv()) {
            throw new RuntimeException("Tjenesten er togglet av.");
        }

        AktorId aktorId = FnrUtils.getAktorIdOrElseThrow(aktorService, fnr);

        aktiverBrukerOgOppfolging(fnr, aktorId, hentInnsatsGruppeBakoverKompatibel(bruker));
    }

    @Transactional
    public void reaktiverBruker(Fnr fnr) {
        pepClient.sjekkLeseTilgangTilFnr(fnr.getFnr());

        if (!registreringFeature.erAktiv()) {
            throw new RuntimeException("Tjenesten er togglet av.");
        }

        AktorId aktorId = FnrUtils.getAktorIdOrElseThrow(aktorService, fnr.getFnr());

        startReaktiveringAvBrukerOgOppfolging(fnr, aktorId);

    }

    private void startReaktiveringAvBrukerOgOppfolging(Fnr fnr, AktorId aktorId) {
        oppfolgingRepository.startOppfolgingHvisIkkeAlleredeStartet(
                Oppfolgingsbruker.builder()
                        .aktoerId(aktorId.getAktorId())
                        .build()
        );

        if (opprettBrukerIArenaFeature.erAktiv()) {
            reaktiverBrukerIArena(fnr);
        }
    }


    //Todo: fjerne denne metoden når FO-1123 går over i Test1.
    private Innsatsgruppe hentInnsatsGruppeBakoverKompatibel(AktiverArbeidssokerData aktiverArbeidssokerData) {
        return ofNullable(aktiverArbeidssokerData.getInnsatsgruppe()).orElse(STANDARD_INNSATS);
    }

    private void aktiverBrukerOgOppfolging(String fnr, AktorId aktorId, Innsatsgruppe innsatsgruppe) {

        oppfolgingRepository.startOppfolgingHvisIkkeAlleredeStartet(
                Oppfolgingsbruker.builder()
                        .aktoerId(aktorId.getAktorId())
                        .innsatsgruppe(innsatsgruppe)
                        .build()
        );

        if (opprettBrukerIArenaFeature.erAktiv()) {
            opprettBrukerIArena(fnr, innsatsgruppe);
        }

        nyeBrukereFeedRepository.tryLeggTilFeedIdPaAlleElementerUtenFeedId();
    }

    @SuppressWarnings({"unchecked"})
    private void opprettBrukerIArena(String fnr, Innsatsgruppe innsatsgruppe) {
        Brukerident brukerident = new Brukerident();
        brukerident.setBrukerident(fnr);
        AktiverBrukerRequest request = new AktiverBrukerRequest();
        request.setIdent(brukerident);
        request.setKvalifiseringsgruppekode(innsatsgruppe.getKode());

        Timer timer = MetricsFactory.createTimer("registrering.i.arena").start();
                Try.run(() -> behandleArbeidssoekerV1.aktiverBruker(request))
                .onFailure((t) -> {
                    timer.stop().setFailed().addTagToReport("aarsak",  t.getClass().getSimpleName()).report();
                    log.warn("Feil ved aktivering av bruker i arena", t);
                })
                .mapFailure(
                        Case($(instanceOf(AktiverBrukerBrukerFinnesIkke.class)), (t) -> new Feil(new ArenaFeilType("BRUKER_ER_UKJENT"))),
                        Case($(instanceOf(AktiverBrukerBrukerIkkeReaktivert.class)), (t) -> new Feil(new ArenaFeilType("BRUKER_KAN_IKKE_REAKTIVERES"))),
                        Case($(instanceOf(AktiverBrukerBrukerKanIkkeAktiveres.class)), (t) -> new Feil(new ArenaFeilType("BRUKER_ER_DOD_UTVANDRET_ELLER_FORSVUNNET"))),
                        Case($(instanceOf(AktiverBrukerBrukerManglerArbeidstillatelse.class)), (t) -> new Feil(new ArenaFeilType("BRUKER_MANGLER_ARBEIDSTILLATELSE"))),
                        Case($(instanceOf(AktiverBrukerSikkerhetsbegrensning.class)), (t) -> new NotAuthorizedException(t)),
                        Case($(instanceOf(AktiverBrukerUgyldigInput.class)), (t) -> new BadRequestException(t)),
                        Case($(), (t) -> new InternalServerErrorException(t))
                )
                .onSuccess((event) -> timer.stop().report())
                .get();
    }

    private void reaktiverBrukerIArena(Fnr fnr) {
        Brukerident brukerident = new Brukerident();
        brukerident.setBrukerident(fnr.getFnr());
        ReaktiverBrukerForenkletRequest request = new ReaktiverBrukerForenkletRequest();
        request.setIdent(brukerident);

        Timer timer = MetricsFactory.createTimer("reaktivering.i.arena").start();
        Try.run(() -> behandleArbeidssoekerV1.reaktiverBrukerForenklet(request))
                .onFailure((t) -> {
                    timer.stop().setFailed().addTagToReport("aarsak",  t.getClass().getSimpleName()).report();
                    log.warn("Feil ved reaktivering av bruker i arena", t);
                })
                .mapFailure(
                        Case($(instanceOf(ReaktiverBrukerForenkletBrukerFinnesIkke.class)), (t) -> new Feil(new ArenaFeilType("BRUKER_ER_UKJENT"))),
                        Case($(instanceOf(ReaktiverBrukerForenkletBrukerKanIkkeAktiveres.class)), (t) -> new Feil(new ArenaFeilType("BRUKER_ER_DOD_UTVANDRET_ELLER_FORSVUNNET"))),
                        Case($(instanceOf(ReaktiverBrukerForenkletBrukerKanIkkeReaktiveresForenklet.class)), (t) -> new Feil(new ArenaFeilType("BRUKER_KAN_IKKE_REAKTIVERES_FORENKLET"))),
                        Case($(instanceOf(ReaktiverBrukerForenkletBrukerManglerArbeidstillatelse.class)), (t) -> new Feil(new ArenaFeilType("BRUKER_MANGLER_ARBEIDSTILLATELSE"))),
                        Case($(instanceOf(ReaktiverBrukerForenkletSikkerhetsbegrensning.class)), (t) -> new NotAuthorizedException(t)),
                        Case($(instanceOf(ReaktiverBrukerForenkletUgyldigInput.class)), (t) -> new BadRequestException(t)),
                        Case($(), (t) -> new InternalServerErrorException(t))
                )
                .onSuccess((event) -> timer.stop().report())
                .get();
    }

    private class ArenaFeilType implements Feil.Type {
        private String feilType;

        public ArenaFeilType(String feilType) {
            this.feilType = feilType;
        }

        @Override
        public String getName() {
            return feilType;
        }

        @Override
        public Response.Status getStatus() {
            return INTERNAL_SERVER_ERROR;
        }
    }
}

