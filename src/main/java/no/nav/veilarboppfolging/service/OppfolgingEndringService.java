package no.nav.veilarboppfolging.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto_schema.enums.arena.Formidlingsgruppe;
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe;
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2;
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

        Formidlingsgruppe formidlingsgruppe = ofNullable(brukerV2.getFormidlingsgruppe()).orElse(null);
        Kvalifiseringsgruppe kvalifiseringsgruppe = ofNullable(brukerV2.getKvalifiseringsgruppe()).orElse(null);

        Optional<OppfolgingEntity> currentLocalOppfolging = oppfolgingsStatusRepository.hentOppfolging(aktorId);

        boolean erBrukerUnderOppfolgingLokalt = currentLocalOppfolging.map(OppfolgingEntity::isUnderOppfolging).orElse(false);
        boolean erUnderOppfolgingIArena = erUnderOppfolging(formidlingsgruppe, kvalifiseringsgruppe);
        boolean erInaktivIArena = erIserv(formidlingsgruppe);
        boolean skalOppfolges = !erBrukerUnderOppfolgingLokalt && erUnderOppfolgingIArena;

        secureLog.info(
                "Status for automatisk oppdatering av oppfølging."
                        + " aktorId={} erUnderOppfølgingIVeilarboppfolging={}"
                        + " erUnderOppfølgingIArena={} erInaktivIArena={}"
                        + " formidlingsgruppe={} kvalifiseringsgruppe={}",
                aktorId, erBrukerUnderOppfolgingLokalt,
                erUnderOppfolgingIArena, erInaktivIArena,
                formidlingsgruppe, kvalifiseringsgruppe
        );

        var harIngenOppfolgingLagret = currentLocalOppfolging.isEmpty();
        oppfolgingService.oppdaterArenaOppfolgingStatus(
                aktorId,
                harIngenOppfolgingLagret,
                new LocalArenaOppfolging(
                        brukerV2.getHovedmaal(),
                        kvalifiseringsgruppe,
                        formidlingsgruppe,
                        Optional.ofNullable(brukerV2.getOppfolgingsenhet()).map(EnhetId::new).orElse(null),
                        brukerV2.getIservFraDato()
                )
        );

        if (skalOppfolges) {
            secureLog.info("Starter oppfølging på bruker som er under oppfølging i Arena, men ikke i veilarboppfolging. aktorId={}", aktorId);
            oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(
                    OppfolgingsRegistrering.Companion.arenaSyncOppfolgingBruker(aktorId, formidlingsgruppe, kvalifiseringsgruppe));
        } else if (erBrukerUnderOppfolgingLokalt && erInaktivIArena) {
            Optional<Boolean> kanEnkeltReaktiveresLokalt = kanEnkeltReaktiveresLokalt(currentLocalOppfolging, brukerV2);
            var maybeKanEnkeltReaktiveres = arenaOppfolgingService.kanEnkeltReaktiveres(fnr);

            if (kanEnkeltReaktiveresLokalt.isPresent() && maybeKanEnkeltReaktiveres.isPresent()) {
                if (!kanEnkeltReaktiveresLokalt.get().equals(maybeKanEnkeltReaktiveres.get())) {
                    log.warn("kunne ikke si om bruker kunne reaktiveres lokalt " +
                                    "\n kanReaktiveres lokalt {} kanReaktiveres remote {}" +
                                    "\n iservDato: {}, kvalifiseringsGruppe: {}, forrige lagrede formidlingsgruppe: {}",
                            kanEnkeltReaktiveresLokalt.get(),
                            maybeKanEnkeltReaktiveres.get(),
                            brukerV2.getIservFraDato(),
                            brukerV2.getKvalifiseringsgruppe(),
                            currentLocalOppfolging.get().getLocalArenaOppfolging().map(LocalArenaOppfolging::getFormidlingsgruppe).orElse(null)
                        );
                }
            }

            if (maybeKanEnkeltReaktiveres.isPresent()) {
                boolean kanEnkeltReaktiveres = maybeKanEnkeltReaktiveres.get();
                boolean erUnderKvp = kvpService.erUnderKvp(aktorId);
                boolean harAktiveTiltaksdeltakelser = oppfolgingService.harAktiveTiltaksdeltakelser(fnr);
                boolean skalAvsluttes = !kanEnkeltReaktiveres && !erUnderKvp && !harAktiveTiltaksdeltakelser;

                secureLog.info(
                        "Status for automatisk avslutting av oppfølging. aktorId={} kanEnkeltReaktiveres={} erUnderKvp={} harAktiveTiltaksdeltakelser={} skalAvsluttes={}",
                        aktorId, kanEnkeltReaktiveres, erUnderKvp, harAktiveTiltaksdeltakelser, skalAvsluttes
                );

                if (skalAvsluttes) {
                    secureLog.info("Automatisk avslutting av oppfølging på bruker. aktorId={}", aktorId);
                    log.info("Utgang: Oppfølging avsluttet automatisk pga. inaktiv bruker som ikke kan reaktiveres");
                    var avregistrering = new ArenaIservKanIkkeReaktiveres(aktorId);
                    oppfolgingService.avsluttOppfolging(avregistrering);
                    metricsService.rapporterAutomatiskAvslutningAvOppfolging(true);
                }
            } else {
                secureLog.warn("Bruker har ikke oppfølgingtilstand i Arena. aktorId={}", aktorId);
            }
        }
    }

    private  Optional<Boolean> kanEnkeltReaktiveresLokalt(Optional<OppfolgingEntity> maybeOppfolging, EndringPaaOppfoelgingsBrukerV2 brukerV2) {
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
