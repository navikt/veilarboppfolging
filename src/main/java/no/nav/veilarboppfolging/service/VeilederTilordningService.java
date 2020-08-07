package no.nav.veilarboppfolging.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarboppfolging.controller.domain.OppfolgingFeedDTO;
import no.nav.veilarboppfolging.controller.domain.TilordneVeilederResponse;
import no.nav.veilarboppfolging.controller.domain.VeilederTilordning;
import no.nav.veilarboppfolging.domain.AktorId;
import no.nav.veilarboppfolging.domain.Tilordning;
import no.nav.veilarboppfolging.feed.cjm.producer.FeedProducer;
import no.nav.veilarboppfolging.kafka.OppfolgingStatusKafkaProducer;
import no.nav.veilarboppfolging.repository.VeilederHistorikkRepository;
import no.nav.veilarboppfolging.repository.VeilederTilordningerRepository;
import no.nav.veilarboppfolging.schedule.IdPaOppfolgingFeedSchedule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class VeilederTilordningService {

    private final MetricsService metricsService;
    private final VeilederTilordningerRepository veilederTilordningerRepository;
    private final AuthService authService;
    private final FeedProducer<OppfolgingFeedDTO> oppfolgingFeed;
    private final OppfolgingRepositoryService oppfolgingRepositoryService;
    private final VeilederHistorikkRepository veilederHistorikkRepository;
    private final TransactionTemplate transactor;
    private final OppfolgingStatusKafkaProducer kafka;

    @Autowired
    public VeilederTilordningService(
            MetricsService metricsService,
            VeilederTilordningerRepository veilederTilordningerRepository,
            AuthService authService,
            FeedProducer<OppfolgingFeedDTO> oppfolgingFeed,
            OppfolgingRepositoryService oppfolgingRepositoryService,
            VeilederHistorikkRepository veilederHistorikkRepository,
            TransactionTemplate transactor,
            OppfolgingStatusKafkaProducer kafka
    ) {
        this.metricsService = metricsService;
        this.veilederTilordningerRepository = veilederTilordningerRepository;
        this.authService = authService;
        this.oppfolgingFeed = oppfolgingFeed;
        this.oppfolgingRepositoryService = oppfolgingRepositoryService;
        this.veilederHistorikkRepository = veilederHistorikkRepository;
        this.transactor = transactor;
        this.kafka = kafka;
    }

    public TilordneVeilederResponse tilordneVeiledere(List<VeilederTilordning> tilordninger) {
        authService.skalVereInternBruker();
        String innloggetVeilederId = authService.getInnloggetVeilederIdent();

        log.info("{} Prøver å tildele veileder", innloggetVeilederId);

        List<VeilederTilordning> feilendeTilordninger = new ArrayList<>();

        for (VeilederTilordning tilordning : tilordninger) {
            tilordning.setInnloggetVeilederId(innloggetVeilederId);

            try {
                String aktorId = authService.getAktorIdOrThrow(tilordning.getBrukerFnr());
                authService.sjekkSkrivetilgangMedAktorId(aktorId);

                tilordning.setAktoerId(aktorId);
                String eksisterendeVeileder = veilederTilordningerRepository.hentTilordningForAktoer(aktorId);

                feilendeTilordninger = tildelVeileder(feilendeTilordninger, tilordning, aktorId, eksisterendeVeileder);
            } catch (Exception e) {
                feilendeTilordninger.add(tilordning);
                loggFeilOppfolging(e, tilordning);
            }
        }

        TilordneVeilederResponse response = new TilordneVeilederResponse().setFeilendeTilordninger(feilendeTilordninger);

        if (feilendeTilordninger.isEmpty()) {
            response.setResultat("OK: Veiledere tilordnet");
        } else {
            response.setResultat("WARNING: Noen brukere kunne ikke tilordnes en veileder");
        }

        if (tilordninger.size() > feilendeTilordninger.size()) {
            //Kaller denne asynkront siden resultatet ikke er interessant og operasjonen tar litt tid.
            CompletableFuture.runAsync(this::kallWebhook);
        }

        return response;
    }

    public void lestAktivitetsplan(String fnr) {
        String aktorId = authService.getAktorIdOrThrow(fnr);

        authService.skalVereInternBruker();
        authService.sjekkLesetilgangMedAktorId(aktorId);

        veilederTilordningerRepository.hentTilordnetVeileder(aktorId)
                .filter(Tilordning::isNyForVeileder)
                .filter(this::erVeilederFor)
                .map(metricsService::lestAvVeileder)
                .map(Tilordning::getAktorId)
                .map(veilederTilordningerRepository::markerSomLestAvVeileder)
                .ifPresent(i -> kallWebhook());
    }

    private List<VeilederTilordning> tildelVeileder(List<VeilederTilordning> feilendeTilordninger, VeilederTilordning tilordning, String aktoerId, String eksisterendeVeileder) {
        if (kanTilordneVeileder(eksisterendeVeileder, tilordning)) {
            if (nyVeilederHarTilgang(tilordning)) {
                skrivTilDatabase(aktoerId, tilordning.getTilVeilederId());
            } else {
                log.info("Aktoerid {} kunne ikke tildeles. Ny veileder {} har ikke tilgang.", aktoerId, tilordning.getTilVeilederId());
                feilendeTilordninger.add(tilordning);
            }
        } else {
            log.info("Aktoerid {} kunne ikke tildeles. Oppgitt fraVeileder {} er feil eller tilVeileder {} er feil. Faktisk veileder: {}",
                    aktoerId, tilordning.getFraVeilederId(), tilordning.getTilVeilederId(), eksisterendeVeileder);
            feilendeTilordninger.add(tilordning);
        }

        return feilendeTilordninger;
    }

    private void loggFeilOppfolging(Exception e, VeilederTilordning tilordning) {

        String fraVeilederId = tilordning.getFraVeilederId();
        String tilVeilederId = tilordning.getTilVeilederId();
        String innloggetVeilederId = tilordning.getInnloggetVeilederId();
        String aktoerId = tilordning.getAktoerId();

        if (e instanceof ResponseStatusException) {
            log.warn("Feil ved tildeling av veileder: innlogget veileder: {}, fraVeileder: {} tilVeileder: {} bruker(aktørId): {} årsak: request is not authorized", innloggetVeilederId, fraVeilederId, tilVeilederId, aktoerId, e);
        } else if (e instanceof IllegalArgumentException) {
            log.error("Feil ved tildeling av veileder: innlogget veileder: {}, fraVeileder: {} tilVeileder: {} årsak: Fant ikke aktørId for bruker", innloggetVeilederId, tilordning.getFraVeilederId(), tilordning.getTilVeilederId(), e);
        } else {
            log.error("Feil ved tildeling av veileder: innlogget veileder: {}, fraVeileder: {} tilVeileder: {} bruker(aktørId): {} årsak: ukjent årsak", innloggetVeilederId, fraVeilederId, tilVeilederId, aktoerId, e);
        }
    }


    private boolean erVeilederFor(Tilordning tilordning) {
        return authService.getInnloggetVeilederIdent().equals(tilordning.getVeilederId());
    }

    private void kallWebhook() {
        try {
            //Venter for å gi tid til å populere ID-er i feeden
            Thread.sleep(IdPaOppfolgingFeedSchedule.INSERT_ID_INTERVAL);

            oppfolgingFeed.activateWebhook();
        } catch (Exception e) {
            // Logger feilen, men bryr oss ikke om det. At webhooken feiler påvirker ikke funksjonaliteten
            // men gjør at endringen kommer senere inn i portefølje
            log.warn("Webhook feilet", e);
        }
    }

    public void skrivTilDatabase(String aktoerId, String veileder) {
        transactor.executeWithoutResult((status) -> {
            veilederTilordningerRepository.upsertVeilederTilordning(aktoerId, veileder);
            veilederHistorikkRepository.insertTilordnetVeilederForAktorId(aktoerId, veileder);
            oppfolgingRepositoryService.startOppfolgingHvisIkkeAlleredeStartet(aktoerId);
            kafka.send(new AktorId(aktoerId));
        });

        log.debug(String.format("Veileder %s tilordnet aktoer %s", veileder, aktoerId));
    }

    static boolean kanTilordneVeileder(String eksisterendeVeileder, VeilederTilordning veilederTilordning) {
        return eksisterendeVeileder == null || validerVeilederTilordning(eksisterendeVeileder, veilederTilordning);
    }

    static boolean validerVeilederTilordning(String eksisterendeVeileder, VeilederTilordning veilederTilordning) {
        return eksisterendeVeilederErSammeSomFra(eksisterendeVeileder, veilederTilordning.getFraVeilederId()) &&
                tildelesTilAnnenVeileder(eksisterendeVeileder, veilederTilordning.getTilVeilederId());
    }

    static boolean eksisterendeVeilederErSammeSomFra(String eksisterendeVeileder, String fraVeileder) {
        return eksisterendeVeileder.equals(fraVeileder);
    }

    static boolean tildelesTilAnnenVeileder(String eksisterendeVeileder, String tilVeileder) {
        return !eksisterendeVeileder.equals(tilVeileder);
    }

    private boolean nyVeilederHarTilgang(VeilederTilordning veilederTilordning) {
        return authService.harVeilederSkriveTilgangTilFnr(veilederTilordning.getTilVeilederId(), veilederTilordning.getBrukerFnr());
    }

}
