package no.nav.veilarboppfolging.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto_schema.enums.arena.Formidlingsgruppe;
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe;
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.EndringPaaOppfolgingsBruker;
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.ArenaIservKanIkkeReaktiveres;
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingsRegistrering;
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingService;
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.LocalArenaOppfolging;
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository;
import no.nav.veilarboppfolging.repository.entity.OppfolgingEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static no.nav.veilarboppfolging.utils.ArenaUtils.erIserv;
import static no.nav.veilarboppfolging.utils.ArenaUtils.erUnderOppfolging;
import static no.nav.veilarboppfolging.utils.SecureLog.secureLog;

@Slf4j
@Service
@RequiredArgsConstructor
public class OppfolgingsbrukerEndretIArenaService {

    private final OppfolgingService oppfolgingService;
    private final StartOppfolgingService startOppfolgingService;
    private final ArenaOppfolgingService arenaOppfolgingService;
    private final KvpService kvpService;
    private final MetricsService metricsService;
    private final OppfolgingsStatusRepository oppfolgingsStatusRepository;

    public void oppdaterOppfolgingMedStatusFraArena(EndringPaaOppfolgingsBruker bruker) {
        Fnr fnr = Fnr.of(bruker.getFodselsnummer());

        Formidlingsgruppe formidlingsgruppe = ofNullable(bruker.getFormidlingsgruppe()).orElse(null);
        Kvalifiseringsgruppe kvalifiseringsgruppe = ofNullable(bruker.getKvalifiseringsgruppe()).orElse(null);

        Optional<OppfolgingEntity> currentLocalOppfolging = oppfolgingsStatusRepository.hentOppfolging(bruker.getAktorId());

        boolean erBrukerUnderOppfolgingLokalt = currentLocalOppfolging.map(OppfolgingEntity::isUnderOppfolging).orElse(false);
        boolean erUnderOppfolgingIArena = erUnderOppfolging(formidlingsgruppe, kvalifiseringsgruppe);
        boolean erInaktivIArena = erIserv(formidlingsgruppe);
        boolean skalOppfolges = !erBrukerUnderOppfolgingLokalt && erUnderOppfolgingIArena;

        secureLog.info(
                "Status for automatisk oppdatering av oppfølging."
                        + " aktorId={} erUnderOppfølgingIVeilarboppfolging={}"
                        + " erUnderOppfølgingIArena={} erInaktivIArena={}"
                        + " formidlingsgruppe={} kvalifiseringsgruppe={}",
                bruker.getAktorId(), erBrukerUnderOppfolgingLokalt,
                erUnderOppfolgingIArena, erInaktivIArena,
                formidlingsgruppe, kvalifiseringsgruppe
        );

        var harIngenOppfolgingLagret = currentLocalOppfolging.isEmpty();
        oppfolgingService.oppdaterArenaOppfolgingStatus(
                bruker.getAktorId(),
                harIngenOppfolgingLagret,
                new LocalArenaOppfolging(
                        bruker.getHovedmaal(),
                        kvalifiseringsgruppe,
                        formidlingsgruppe,
                        Optional.ofNullable(bruker.getOppfolgingsenhet()).map(EnhetId::new).orElse(null),
                        bruker.getIservFraDato()
                )
        );

        if (skalOppfolges) {
            secureLog.info("Starter oppfølging på bruker som er under oppfølging i Arena, men ikke i veilarboppfolging. aktorId={}", bruker.getAktorId());
            startOppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(
                    OppfolgingsRegistrering.Companion.arenaSyncOppfolgingBruker(
                            fnr,
                            bruker.getAktorId(),
                            formidlingsgruppe,
                            kvalifiseringsgruppe,
                            EnhetId.of(bruker.getOppfolgingsenhet())
                    )
            );
        } else if (erBrukerUnderOppfolgingLokalt && erInaktivIArena) {
            Optional<Boolean> kanEnkeltReaktiveresLokalt = kanEnkeltReaktiveresLokalt(currentLocalOppfolging, bruker);
            var maybeKanEnkeltReaktiveres = arenaOppfolgingService.kanEnkeltReaktiveres(fnr);

            if (kanEnkeltReaktiveresLokalt.isPresent() && maybeKanEnkeltReaktiveres.isPresent()) {
                if (!kanEnkeltReaktiveresLokalt.get().equals(maybeKanEnkeltReaktiveres.get())) {
                    log.warn("kunne ikke si om bruker kunne reaktiveres lokalt " +
                                    "\n kanReaktiveres lokalt {} kanReaktiveres remote {}" +
                                    "\n iservDato: {}, kvalifiseringsGruppe: {}, forrige lagrede formidlingsgruppe: {}",
                            kanEnkeltReaktiveresLokalt.get(),
                            maybeKanEnkeltReaktiveres.get(),
                            bruker.getIservFraDato(),
                            bruker.getKvalifiseringsgruppe(),
                            currentLocalOppfolging.get().getLocalArenaOppfolging().map(LocalArenaOppfolging::getFormidlingsgruppe).orElse(null)
                    );
                }
            }

            if (maybeKanEnkeltReaktiveres.isPresent()) {
                boolean kanEnkeltReaktiveres = maybeKanEnkeltReaktiveres.get();
                boolean erUnderKvp = kvpService.erUnderKvp(bruker.getAktorId());
                boolean harAktiveTiltaksdeltakelser = oppfolgingService.harAktiveTiltaksdeltakelser(fnr);
                boolean skalAvsluttes = !kanEnkeltReaktiveres && !erUnderKvp && !harAktiveTiltaksdeltakelser;

                secureLog.info(
                        "Status for automatisk avslutting av oppfølging. aktorId={} kanEnkeltReaktiveres={} erUnderKvp={} harAktiveTiltaksdeltakelser={} skalAvsluttes={}",
                        bruker.getAktorId(), kanEnkeltReaktiveres, erUnderKvp, harAktiveTiltaksdeltakelser, skalAvsluttes
                );

                if (skalAvsluttes) {
                    secureLog.info("Automatisk avslutting av oppfølging på bruker. aktorId={}", bruker.getAktorId());
                    log.info("Utgang: Oppfølging avsluttet automatisk pga. inaktiv bruker som ikke kan reaktiveres");
                    var avregistrering = new ArenaIservKanIkkeReaktiveres(bruker.getAktorId());
                    oppfolgingService.avsluttOppfolging(avregistrering);
                    metricsService.rapporterAutomatiskAvslutningAvOppfolging(true);
                }
            } else {
                secureLog.warn("Bruker har ikke oppfølgingtilstand i Arena. aktorId={}", bruker.getAktorId());
            }
        }
    }

    private Optional<Boolean> kanEnkeltReaktiveresLokalt(Optional<OppfolgingEntity> maybeOppfolging, EndringPaaOppfolgingsBruker brukerV2) {
        return maybeOppfolging
                .flatMap(OppfolgingEntity::getLocalArenaOppfolging)
                .map(forrigeArenaOppfolging ->
                        brukerV2.getFormidlingsgruppe() == Formidlingsgruppe.ISERV &&
                                brukerV2.getIservFraDato().isAfter(LocalDate.now().minusDays(28)) &&
                                forrigeArenaOppfolging.getFormidlingsgruppe() == Formidlingsgruppe.ARBS &&
                                !List.of(Kvalifiseringsgruppe.BKART, Kvalifiseringsgruppe.IVURD).contains(brukerV2.getKvalifiseringsgruppe())
                );
    }
}
