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

    public void oppdaterOppfolgingMedStatusFraArena(EndringPaaOppfoelgingsBrukerV1 endringPaaOppfoelgingsBrukerV1) {
        Fnr fnr = Fnr.of(endringPaaOppfoelgingsBrukerV1.getFodselsnr());
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);

        String formidlingsgruppe = endringPaaOppfoelgingsBrukerV1.getFormidlingsgruppekode();
        String kvalifiseringsgruppekode = endringPaaOppfoelgingsBrukerV1.getKvalifiseringsgruppekode();

        Optional<OppfolgingTable> maybeOppfolging = ofNullable(oppfolgingsStatusRepository.fetch(aktorId));

        boolean erBrukerUnderOppfolging = maybeOppfolging.map(OppfolgingTable::isUnderOppfolging).orElse(false);
        boolean erUnderOppfolgingIArena = erUnderOppfolging(formidlingsgruppe, kvalifiseringsgruppekode);
        boolean erInaktivIArena = erIserv(formidlingsgruppe);

        if (!erBrukerUnderOppfolging && erUnderOppfolgingIArena) {
            oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(aktorId);
        } else if (erBrukerUnderOppfolging && !erUnderOppfolgingIArena && erInaktivIArena) {
            Optional<ArenaOppfolgingTilstand> arenaOppfolgingTilstand = arenaOppfolgingService.hentOppfolgingTilstandDirekteFraArena(fnr);

            arenaOppfolgingTilstand.ifPresent(tilstand -> {
                boolean kanEnkeltReaktiveres = TRUE.equals(tilstand.getKanEnkeltReaktiveres());
                boolean erUnderKvp = kvpService.erUnderKvp(aktorId);
                boolean skalAvsluttes = !kanEnkeltReaktiveres && !erUnderKvp;

                if (skalAvsluttes) {
                    oppfolgingService.avsluttOppfolgingForBruker(aktorId, null, "Oppfølging avsluttet automatisk pga. inaktiv bruker som ikke kan reaktiveres");
                    metricsService.raporterAutomatiskAvslutningAvOppfolging(true);
                }
            });
        }
    }

}
