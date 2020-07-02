package no.nav.veilarboppfolging.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarboppfolging.db.OppfolgingFeedRepository;
import no.nav.veilarboppfolging.db.OppfolgingRepository;
import no.nav.veilarboppfolging.db.VeilederHistorikkRepository;
import no.nav.veilarboppfolging.db.VeilederTilordningerRepository;
import no.nav.veilarboppfolging.domain.AktorId;
import no.nav.veilarboppfolging.domain.Tilordning;
import no.nav.veilarboppfolging.kafka.OppfolgingStatusKafkaProducer;
import no.nav.veilarboppfolging.controller.domain.TilordneVeilederResponse;
import no.nav.veilarboppfolging.controller.domain.VeilederTilordning;
import no.nav.veilarboppfolging.schedule.IdPaOppfolgingFeedSchedule;
import no.nav.veilarboppfolging.services.AuthService;
import no.nav.veilarboppfolging.services.MetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

// TODO: Det meste i denne klassen burde flyttes inn i en service

@Slf4j
@RestController
@RequestMapping("/api")
public class VeilederTilordningController {

    private final MetricsService metricsService;
    private final VeilederTilordningerRepository veilederTilordningerRepository;
    private final AuthService authService;
//    private final FeedProducer<OppfolgingFeedDTO> oppfolgingFeed;
    private final OppfolgingRepository oppfolgingRepository;
    private final VeilederHistorikkRepository veilederHistorikkRepository;
    private final TransactionTemplate transactor;
    private final OppfolgingStatusKafkaProducer kafka;

    @Autowired
    public VeilederTilordningController(
            MetricsService metricsService,
            VeilederTilordningerRepository veilederTilordningerRepository,
            AuthService authService,
            OppfolgingRepository oppfolgingRepository,
            VeilederHistorikkRepository veilederHistorikkRepository,
            TransactionTemplate transactor,
            OppfolgingStatusKafkaProducer kafka
    ) {
        this.metricsService = metricsService;
        this.veilederTilordningerRepository = veilederTilordningerRepository;
        this.authService = authService;
        this.oppfolgingRepository = oppfolgingRepository;
        this.veilederHistorikkRepository = veilederHistorikkRepository;
        this.transactor = transactor;
        this.kafka = kafka;
    }

    @PostMapping("/tilordneveileder")
    public TilordneVeilederResponse postVeilederTilordninger(List<VeilederTilordning> tilordninger) {
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

    @POST
    @Path("{fnr}/lestaktivitetsplan/")
    public void lestAktivitetsplan(@PathParam("fnr") String fnr) {
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

    private void loggFeilOppfolging(Exception e, VeilederTilordning tilordning) {

        String fraVeilederId = tilordning.getFraVeilederId();
        String tilVeilederId = tilordning.getTilVeilederId();
        String innloggetVeilederId = tilordning.getInnloggetVeilederId();
        String aktoerId = tilordning.getAktoerId();

        if (e instanceof NotAuthorizedException) {
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

            // TODO: Add feed
//            oppfolgingFeed.activateWebhook();
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
            oppfolgingRepository.startOppfolgingHvisIkkeAlleredeStartet(aktoerId);
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
