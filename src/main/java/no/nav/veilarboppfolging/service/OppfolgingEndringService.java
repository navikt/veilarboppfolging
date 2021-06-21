package no.nav.veilarboppfolging.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV1;
import no.nav.veilarboppfolging.domain.ArenaOppfolgingTilstand;
import no.nav.veilarboppfolging.domain.OppfolgingTable;
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static java.lang.Boolean.TRUE;
import static java.util.Optional.ofNullable;
import static no.nav.veilarboppfolging.utils.ArenaUtils.erIserv;
import static no.nav.veilarboppfolging.utils.ArenaUtils.erUnderOppfolging;

@Slf4j
@Service
@RequiredArgsConstructor
public class OppfolgingEndringService {

    private final AuthService authService;

    private final OppfolgingService oppfolgingService;

    private final ArenaOppfolgingService arenaOppfolgingService;

    private final KvpService kvpService;

    private final MetricsService metricsService;

    private final OppfolgingsStatusRepository oppfolgingsStatusRepository;

    private final UnleashService unleashService;

    public void oppdaterOppfolgingMedStatusFraArena(EndringPaaOppfoelgingsBrukerV1 endringPaaOppfoelgingsBrukerV1) {
        Fnr fnr = Fnr.of(endringPaaOppfoelgingsBrukerV1.getFodselsnr());
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);

        String formidlingsgruppe = endringPaaOppfoelgingsBrukerV1.getFormidlingsgruppekode();
        String kvalifiseringsgruppe = endringPaaOppfoelgingsBrukerV1.getKvalifiseringsgruppekode();

        Optional<OppfolgingTable> maybeOppfolging = ofNullable(oppfolgingsStatusRepository.fetch(aktorId));

        boolean erBrukerUnderOppfolging = maybeOppfolging.map(OppfolgingTable::isUnderOppfolging).orElse(false);
        boolean erUnderOppfolgingIArena = erUnderOppfolging(formidlingsgruppe, kvalifiseringsgruppe);
        boolean erInaktivIArena = erIserv(formidlingsgruppe);

        log.info(
                "Status for automatisk oppdatering av oppfølging."
                        + " aktorId={} erUnderOppfølgingIVeilarboppfolging={}"
                        + " erUnderOppfølgingIArena={} erInaktivIArena={}"
                        + " formidlingsgruppe={} kvalifiseringsgruppe={}",
                aktorId, erBrukerUnderOppfolging,
                erUnderOppfolgingIArena, erInaktivIArena,
                formidlingsgruppe, kvalifiseringsgruppe
        );

        if (!erBrukerUnderOppfolging && erUnderOppfolgingIArena) {
            log.info("Starter oppfølging på bruker som er under oppfølging i Arena, men ikke i veilarboppfolging. aktorId={}", aktorId);

            if (!unleashService.skalOppdaterOppfolgingMedKafka()) {
                log.info("Oppdatering av oppfølging med kafka er ikke skrudd på. Stopper start av oppfølging for aktorId={}", aktorId);
                return;
            }

            oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(aktorId);
        } else if (erBrukerUnderOppfolging && !erUnderOppfolgingIArena && erInaktivIArena) {
            Optional<ArenaOppfolgingTilstand> maybeArenaTilstand = arenaOppfolgingService.hentOppfolgingTilstandDirekteFraArena(fnr);

            if (maybeArenaTilstand.isPresent()) {
                ArenaOppfolgingTilstand tilstand = maybeArenaTilstand.get();
                boolean kanEnkeltReaktiveres = TRUE.equals(tilstand.getKanEnkeltReaktiveres());
                boolean erUnderKvp = kvpService.erUnderKvp(aktorId);
                boolean skalAvsluttes = !kanEnkeltReaktiveres && !erUnderKvp;

                log.info(
                        "Status for automatisk avslutting av oppfølging. aktorId={} kanEnkeltReaktiveres={} erUnderKvp={} skalAvsluttes={}",
                        aktorId, kanEnkeltReaktiveres, erUnderKvp, skalAvsluttes
                );

                if (skalAvsluttes) {
                    log.info("Automatisk avslutting av oppfølging på bruker. aktorId={}", aktorId);

                    if (!unleashService.skalOppdaterOppfolgingMedKafka()) {
                        log.info("Oppdatering av oppfølging med kafka er ikke skrudd på. Stopper avslutting av oppfølging for aktorId={}", aktorId);
                        return;
                    }

                    oppfolgingService.avsluttOppfolgingForBruker(aktorId, null, "Oppfølging avsluttet automatisk pga. inaktiv bruker som ikke kan reaktiveres");
                    metricsService.rapporterAutomatiskAvslutningAvOppfolging(true);
                }
            } else {
                log.warn("Bruker har ikke oppfølgingtilstand i Arena. aktorId={}", aktorId);
            }
        }
    }

}
