package no.nav.veilarboppfolging.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto_schema.enums.arena.Formidlingsgruppe;
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe;
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2;
import no.nav.veilarboppfolging.client.veilarbarena.ArenaOppfolgingTilstand;
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository;
import no.nav.veilarboppfolging.repository.entity.OppfolgingEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static java.lang.Boolean.TRUE;
import static java.util.Optional.ofNullable;
import static no.nav.veilarboppfolging.utils.ArenaUtils.erIserv;
import static no.nav.veilarboppfolging.utils.ArenaUtils.erUnderOppfolging;
import static no.nav.veilarboppfolging.utils.SecureLog.secureLog;

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

    public void oppdaterOppfolgingMedStatusFraArena(EndringPaaOppfoelgingsBrukerV2 brukerV2) {
        Fnr fnr = Fnr.of(brukerV2.getFodselsnummer());
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);

        String formidlingsgruppe = ofNullable(brukerV2.getFormidlingsgruppe()).map(Formidlingsgruppe::toString).orElse(null);
        String kvalifiseringsgruppe = ofNullable(brukerV2.getKvalifiseringsgruppe()).map(Kvalifiseringsgruppe::toString).orElse(null);

        Optional<OppfolgingEntity> maybeOppfolging = oppfolgingsStatusRepository.hentOppfolging(aktorId);

        boolean erBrukerUnderOppfolging = maybeOppfolging.map(OppfolgingEntity::isUnderOppfolging).orElse(false);
        boolean erUnderOppfolgingIArena = erUnderOppfolging(formidlingsgruppe, kvalifiseringsgruppe);
        boolean erInaktivIArena = erIserv(formidlingsgruppe);

        secureLog.info(
                "Status for automatisk oppdatering av oppfølging."
                        + " aktorId={} erUnderOppfølgingIVeilarboppfolging={}"
                        + " erUnderOppfølgingIArena={} erInaktivIArena={}"
                        + " formidlingsgruppe={} kvalifiseringsgruppe={}",
                aktorId, erBrukerUnderOppfolging,
                erUnderOppfolgingIArena, erInaktivIArena,
                formidlingsgruppe, kvalifiseringsgruppe
        );

        if (!erBrukerUnderOppfolging && erUnderOppfolgingIArena) {
            secureLog.info("Starter oppfølging på bruker som er under oppfølging i Arena, men ikke i veilarboppfolging. aktorId={}", aktorId);
            oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(aktorId);
        } else if (erBrukerUnderOppfolging && !erUnderOppfolgingIArena && erInaktivIArena) {
            Optional<ArenaOppfolgingTilstand> maybeArenaTilstand = arenaOppfolgingService.hentOppfolgingTilstandDirekteFraArena(fnr);

            if (maybeArenaTilstand.isPresent()) {
                ArenaOppfolgingTilstand tilstand = maybeArenaTilstand.get();
                boolean kanEnkeltReaktiveres = TRUE.equals(tilstand.getKanEnkeltReaktiveres());
                boolean erUnderKvp = kvpService.erUnderKvp(aktorId);
                boolean skalAvsluttes = !kanEnkeltReaktiveres && !erUnderKvp;

                secureLog.info(
                        "Status for automatisk avslutting av oppfølging. aktorId={} kanEnkeltReaktiveres={} erUnderKvp={} skalAvsluttes={}",
                        aktorId, kanEnkeltReaktiveres, erUnderKvp, skalAvsluttes
                );

                if (skalAvsluttes) {
                    secureLog.info("Automatisk avslutting av oppfølging på bruker. aktorId={}", aktorId);
                    oppfolgingService.avsluttOppfolgingForBruker(aktorId, null, "Oppfølging avsluttet automatisk pga. inaktiv bruker som ikke kan reaktiveres");
                    metricsService.rapporterAutomatiskAvslutningAvOppfolging(true);
                }
            } else {
                secureLog.warn("Bruker har ikke oppfølgingtilstand i Arena. aktorId={}", aktorId);
            }
        }
    }

}
