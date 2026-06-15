package no.nav.veilarboppfolging.service;


import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;
import no.nav.veilarboppfolging.controller.request.VeilederTilordning;
import no.nav.veilarboppfolging.controller.response.TilordneVeilederResponse;
import no.nav.veilarboppfolging.repository.VeilederHistorikkRepository;
import no.nav.veilarboppfolging.repository.VeilederTilordningerRepository;
import no.nav.veilarboppfolging.repository.entity.VeilederTilordningEntity;
import no.nav.veilarboppfolging.utils.DtoMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static no.nav.veilarboppfolging.utils.SecureLog.secureLog;


@Service
public class VeilederTilordningService {

    private final MetricsService metricsService;
    private final VeilederTilordningerRepository veilederTilordningerRepository;
    private final AuthService authService;
    private final OppfolgingService oppfolgingService;
    private final VeilederHistorikkRepository veilederHistorikkRepository;
    private final TransactionTemplate transactor;
    private final KafkaProducerService kafkaProducerService;

    @Autowired
    public VeilederTilordningService(
            MetricsService metricsService,
            VeilederTilordningerRepository veilederTilordningerRepository,
            AuthService authService,
            OppfolgingService oppfolgingService,
            VeilederHistorikkRepository veilederHistorikkRepository,
            TransactionTemplate transactor,
            KafkaProducerService kafkaProducerService
    ) {
        this.metricsService = metricsService;
        this.veilederTilordningerRepository = veilederTilordningerRepository;
        this.authService = authService;
        this.oppfolgingService = oppfolgingService;
        this.veilederHistorikkRepository = veilederHistorikkRepository;
        this.transactor = transactor;
        this.kafkaProducerService = kafkaProducerService;
    }

    public Optional<NavIdent> hentTilordnetVeilederIdent(Fnr fnr) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);

        return veilederTilordningerRepository.hentTilordnetVeileder(aktorId)
                .flatMap(v -> Optional.ofNullable(v.getVeilederId()))
                .map(NavIdent::of);
    }

    public TilordneVeilederResponse tilordneVeiledere(List<VeilederTilordning> tilordninger) {
        authService.skalVereInternBruker();
        String innloggetVeilederId = authService.getInnloggetVeilederIdent();

        secureLog.info("{} Prøver å tildele veileder", innloggetVeilederId);

        List<VeilederTilordning> feilendeTilordninger = new ArrayList<>();

        for (VeilederTilordning tilordning : tilordninger) {
            var oppdatertTilordning = tilordning.oppdaterMedInnloggetVeilederId(innloggetVeilederId);

            try {
                AktorId aktorId = authService.getAktorIdOrThrow(Fnr.of(tilordning.getBrukerFnr()));
                authService.sjekkSkrivetilgangMedAktorId(aktorId);
                oppdatertTilordning = tilordning.oppdaterMedAktorId(aktorId.get());
                String eksisterendeVeileder = veilederTilordningerRepository.hentTilordningForAktoer(aktorId);
                feilendeTilordninger = tildelVeileder(feilendeTilordninger, oppdatertTilordning, aktorId, eksisterendeVeileder, innloggetVeilederId);
            } catch (Exception e) {
                feilendeTilordninger.add(oppdatertTilordning);
                loggFeilOppfolging(e, oppdatertTilordning);
            }
        }

        if (feilendeTilordninger.isEmpty()) {
            return new TilordneVeilederResponse("OK: Veiledere tilordnet", List.of());
        } else {
            return new TilordneVeilederResponse("WARNING: Noen brukere kunne ikke tilordnes en veileder", feilendeTilordninger);
        }
    }

    public void lestAktivitetsplan(Fnr fnr) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);

        authService.skalVereInternBruker();
        authService.sjekkLesetilgangMedFnr(fnr);

        // TODO: Skriveoperasjonene burde gjøres i en transaksjon
        veilederTilordningerRepository.hentTilordnetVeileder(aktorId)
                .filter(VeilederTilordningEntity::getNyForVeileder)
                .filter(this::erVeilederFor)
                .map(metricsService::lestAvVeileder)
                .map(VeilederTilordningEntity::getAktorId)
                .map(AktorId::of)
                .map(veilederTilordningerRepository::markerSomLestAvVeileder)
                .ifPresent(i -> kafkaProducerService.publiserEndringPaNyForVeileder(aktorId, false));
    }

    private List<VeilederTilordning> tildelVeileder(List<VeilederTilordning> feilendeTilordninger, VeilederTilordning tilordning, AktorId aktorId, String eksisterendeVeileder, String innloggetVeilederId) {
        if (kanTilordneVeileder(eksisterendeVeileder, tilordning)) {
            if (nyVeilederHarTilgang(tilordning)) {
                lagreVeilederTilordning(aktorId, tilordning.getTilVeilederId(), innloggetVeilederId);
            } else {
                secureLog.info("Aktoerid {} kunne ikke tildeles. Ny veileder {} har ikke tilgang.", aktorId, tilordning.getTilVeilederId());
                feilendeTilordninger.add(tilordning);
            }
        } else {
            secureLog.info("Aktoerid {} kunne ikke tildeles. Oppgitt fraVeileder {} er feil eller tilVeileder {} er feil. Faktisk veileder: {}",
                    aktorId, tilordning.getFraVeilederId(), tilordning.getTilVeilederId(), eksisterendeVeileder);
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
            secureLog.warn("Feil ved tildeling av veileder: innlogget veileder: {}, fraVeileder: {} tilVeileder: {} bruker(aktørId): {} årsak: request is not authorized", innloggetVeilederId, fraVeilederId, tilVeilederId, aktoerId, e);
        } else if (e instanceof IllegalArgumentException) {
            secureLog.error("Feil ved tildeling av veileder: innlogget veileder: {}, fraVeileder: {} tilVeileder: {} årsak: Fant ikke aktørId for bruker", innloggetVeilederId, tilordning.getFraVeilederId(), tilordning.getTilVeilederId(), e);
        } else {
            secureLog.error("Feil ved tildeling av veileder: innlogget veileder: {}, fraVeileder: {} tilVeileder: {} bruker(aktørId): {} årsak: ukjent årsak", innloggetVeilederId, fraVeilederId, tilVeilederId, aktoerId, e);
        }
    }


    private boolean erVeilederFor(VeilederTilordningEntity tilordning) {
        return authService.getInnloggetVeilederIdent().equals(tilordning.getVeilederId());
    }

    private void lagreVeilederTilordning(AktorId aktorId, String veilederId, String tilordnetAvVeileder) {
        transactor.executeWithoutResult((status) -> {
            veilederTilordningerRepository.upsertVeilederTilordning(aktorId, veilederId);
            veilederHistorikkRepository.insertTilordnetVeilederForAktorId(aktorId, veilederId, tilordnetAvVeileder);

            boolean erUnderOppfolging = oppfolgingService.erUnderOppfolging(aktorId);

            if (!erUnderOppfolging) {
                throw new IllegalStateException(
                        format("Bruker med aktør-id %s som ikke er under oppfølging kan ikke få tilordnet veileder", aktorId)
                );
            }

            secureLog.debug(format("Veileder %s tilordnet aktoer %s", veilederId, aktorId));

            Optional<VeilederTilordningEntity> maybeTilordning = veilederTilordningerRepository.hentTilordnetVeileder(aktorId);

            maybeTilordning.ifPresentOrElse(tilordning -> {
                var dto = DtoMappers.tilSisteTilordnetVeilederKafkaDTO(tilordning);
                kafkaProducerService.publiserSisteTilordnetVeileder(dto);
                kafkaProducerService.publiserVeilederTilordnet(aktorId, veilederId, tilordning.getSistTilordnet() );
            }, () -> secureLog.error("Fant ikke tilordning til nylig tilordnet veileder. AktorId={} VeilederId={}", aktorId, veilederId));

            kafkaProducerService.publiserEndringPaNyForVeileder(aktorId, true);

        });
    }

    static boolean kanTilordneVeileder(String eksisterendeVeileder, VeilederTilordning veilederTilordning) {
        return eksisterendeVeileder == null || validerVeilederTilordning(eksisterendeVeileder, veilederTilordning);
    }

    private static boolean validerVeilederTilordning(String eksisterendeVeileder, VeilederTilordning
            veilederTilordning) {
        return eksisterendeVeilederErSammeSomFra(eksisterendeVeileder, veilederTilordning.getFraVeilederId()) &&
                tildelesTilAnnenVeileder(eksisterendeVeileder, veilederTilordning.getTilVeilederId());
    }

    private static boolean eksisterendeVeilederErSammeSomFra(String eksisterendeVeileder, String fraVeileder) {
        return eksisterendeVeileder.equals(fraVeileder);
    }

    private static boolean tildelesTilAnnenVeileder(String eksisterendeVeileder, String tilVeileder) {
        return !eksisterendeVeileder.equals(tilVeileder);
    }

    private boolean nyVeilederHarTilgang(VeilederTilordning veilederTilordning) {
        return authService.harVeilederSkriveTilgangTilFnr(veilederTilordning.getTilVeilederId(), Fnr.of(veilederTilordning.getBrukerFnr()));
    }

}
