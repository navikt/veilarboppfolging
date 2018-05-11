package no.nav.fo.veilarboppfolging.services;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.config.RemoteFeatureConfig;
import no.nav.fo.veilarboppfolging.db.NyeBrukereFeedRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingRepository;
import no.nav.fo.veilarboppfolging.domain.AktiverArbeidssokerData;
import no.nav.fo.veilarboppfolging.domain.AktorId;
import no.nav.fo.veilarboppfolging.domain.Oppfolgingsbruker;
import no.nav.fo.veilarboppfolging.utils.FnrUtils;
import no.nav.metrics.MetricsFactory;
import no.nav.metrics.Timer;
import no.nav.tjeneste.virksomhet.behandlearbeidssoeker.v1.binding.*;
import no.nav.tjeneste.virksomhet.behandlearbeidssoeker.v1.informasjon.Brukerident;
import no.nav.tjeneste.virksomhet.behandlearbeidssoeker.v1.meldinger.AktiverBrukerRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.Predicates.instanceOf;
import static java.util.Optional.ofNullable;

@Slf4j
@Component
public class AktiverBrukerService {


    private static final String KVALIFISERINGSGRUPPEKODE_SELVGAENDE = "IKVAL";
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

        aktiverBrukerOgOppfolging(fnr, aktorId, bruker.getSelvgaende());
    }

    private void aktiverBrukerOgOppfolging(String fnr, AktorId aktorId, boolean selvgaende) {
        oppfolgingRepository.startOppfolgingHvisIkkeAlleredeStartet(
                Oppfolgingsbruker.builder()
                        .aktoerId(aktorId.getAktorId())
                        .selvgaende(selvgaende)
                        .build()
        );

        String kvalifiseringsgruppekodeSelvgaende = selvgaende ? KVALIFISERINGSGRUPPEKODE_SELVGAENDE : "";

        if (opprettBrukerIArenaFeature.erAktiv()) {
            opprettBrukerIArena(fnr, kvalifiseringsgruppekodeSelvgaende);
        }

        nyeBrukereFeedRepository.tryLeggTilFeedIdPaAlleElementerUtenFeedId();
    }

    @SuppressWarnings({"unchecked"})
    private void opprettBrukerIArena(String fnr, String kvalifiseringsgruppekode) {
        Brukerident brukerident = new Brukerident();
        brukerident.setBrukerident(fnr);
        AktiverBrukerRequest request = new AktiverBrukerRequest();
        request.setIdent(brukerident);
        request.setKvalifiseringsgruppekode(kvalifiseringsgruppekode);

        Timer timer = MetricsFactory.createTimer("registrering.i.arena").start();
        Try.run(() -> behandleArbeidssoekerV1.aktiverBruker(request))
                .onFailure((t) -> {
                    timer.stop()
                            .setFailed()
                            .addTagToReport("aarsak", t.getClass().getSimpleName())
                            .report();
                    log.warn("Feil ved aktivering av bruker i arena", t);
                })
                .mapFailure(
                        Case($(instanceOf(AktiverBrukerBrukerFinnesIkke.class)), (t) -> new NotFoundException(t)),
                        Case($(instanceOf(AktiverBrukerBrukerIkkeReaktivert.class)), (t) -> new ServerErrorException(Response.Status.BAD_GATEWAY, t)),
                        Case($(instanceOf(AktiverBrukerBrukerKanIkkeAktiveres.class)), (t) -> new ServerErrorException(Response.Status.BAD_GATEWAY, t)),
                        Case($(instanceOf(AktiverBrukerBrukerManglerArbeidstillatelse.class)), (t) -> new ServerErrorException(Response.Status.BAD_GATEWAY, t)),
                        Case($(instanceOf(AktiverBrukerSikkerhetsbegrensning.class)), (t) -> new NotAuthorizedException(t)),
                        Case($(instanceOf(AktiverBrukerUgyldigInput.class)), (t) -> new BadRequestException(t)),
                        Case($(), (t) -> new InternalServerErrorException(t))
                )
                .onSuccess((event) -> timer.stop().report())
                .get();
    }

}

