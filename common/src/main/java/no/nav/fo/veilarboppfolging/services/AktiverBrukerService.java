package no.nav.fo.veilarboppfolging.services;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.config.RemoteFeatureConfig;
import no.nav.fo.veilarboppfolging.db.NyeBrukereFeedRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingRepository;
import no.nav.fo.veilarboppfolging.domain.AktiverArbeidssokerData;
import no.nav.fo.veilarboppfolging.domain.AktiverBrukerResponseStatus;
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

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAuthorizedException;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.Predicates.instanceOf;
import static java.util.Optional.ofNullable;
import static no.nav.fo.veilarboppfolging.domain.AktiverBrukerResponseStatus.Status.*;

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
    public AktiverBrukerResponseStatus aktiverBruker(AktiverArbeidssokerData bruker) {
        String fnr = ofNullable(bruker.getFnr())
                .map(f -> f.getFnr())
                .orElse("");

        pepClient.sjekkLeseTilgangTilFnr(fnr);

        if (!registreringFeature.erAktiv()) {
            throw new RuntimeException("Tjenesten er togglet av.");
        }

        AktorId aktorId = FnrUtils.getAktorIdOrElseThrow(aktorService, fnr);

        return aktiverBrukerOgOppfolging(fnr, aktorId, bruker.getSelvgaende());
    }

    private AktiverBrukerResponseStatus aktiverBrukerOgOppfolging(String fnr, AktorId aktorId, boolean selvgaende) {

        oppfolgingRepository.startOppfolgingHvisIkkeAlleredeStartet(
                Oppfolgingsbruker.builder()
                        .aktoerId(aktorId.getAktorId())
                        .selvgaende(selvgaende)
                        .build()
        );

        String kvalifiseringsgruppekodeSelvgaende = selvgaende ? KVALIFISERINGSGRUPPEKODE_SELVGAENDE : "";

        AktiverBrukerResponseStatus aktiverBrukerResponseStatus = new AktiverBrukerResponseStatus(INGEN_STATUS);
        if (opprettBrukerIArenaFeature.erAktiv()) {
            aktiverBrukerResponseStatus = opprettBrukerIArena(fnr, kvalifiseringsgruppekodeSelvgaende);
        }

        nyeBrukereFeedRepository.tryLeggTilFeedIdPaAlleElementerUtenFeedId();
        return aktiverBrukerResponseStatus;
    }

    @SuppressWarnings({"unchecked"})
    private AktiverBrukerResponseStatus opprettBrukerIArena(String fnr, String kvalifiseringsgruppekode) {
        Brukerident brukerident = new Brukerident();
        brukerident.setBrukerident(fnr);
        AktiverBrukerRequest request = new AktiverBrukerRequest();
        request.setIdent(brukerident);
        request.setKvalifiseringsgruppekode(kvalifiseringsgruppekode);

        Timer timer = MetricsFactory.createTimer("registrering.i.arena").start();
        AktiverBrukerResponseStatus aktiverBrukerResponseStatus =
                Try.of(() -> aktiverBrukerIArenaMedRespons(request))
                .onFailure((t) -> {
                    timer.stop()
                            .setFailed()
                            .addTagToReport("aarsak",  t.getClass().getSimpleName())
                            .report();
                    log.warn("Feil ved aktivering av bruker i arena", t);
                })
                .recover(AktiverBrukerBrukerFinnesIkke.class, new AktiverBrukerResponseStatus(BRUKER_ER_UKJENT))
                .recover(AktiverBrukerBrukerIkkeReaktivert.class, new AktiverBrukerResponseStatus(BRUKER_KAN_IKKE_REAKTIVERES))
                .recover(AktiverBrukerBrukerKanIkkeAktiveres.class, new AktiverBrukerResponseStatus(BRUKER_ER_DOD_UTVANDRET_ELLER_FORSVUNNET))
                .recover(AktiverBrukerBrukerManglerArbeidstillatelse.class, new AktiverBrukerResponseStatus(BRUKER_MANGLER_ARBEIDSTILLATELSE))
                .mapFailure(
                        Case($(instanceOf(AktiverBrukerSikkerhetsbegrensning.class)), (t) -> new NotAuthorizedException(t)),
                        Case($(instanceOf(AktiverBrukerUgyldigInput.class)), (t) -> new BadRequestException(t)),
                        Case($(), (t) -> new InternalServerErrorException(t))
                )
                .onSuccess((event) -> timer.stop().report())
                .get();

        return aktiverBrukerResponseStatus;
    }

    private AktiverBrukerResponseStatus aktiverBrukerIArenaMedRespons(AktiverBrukerRequest request) throws Exception{
        behandleArbeidssoekerV1.aktiverBruker(request);
        return new AktiverBrukerResponseStatus(STATUS_SUKSESS);
    }

}

