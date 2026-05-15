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
import static no.nav.veilarboppfolging.service.EndringPaaOppfolgingsbrukerEventKt.harBlittSykmeldtUtenArbeidsgiver;
import static no.nav.veilarboppfolging.utils.ArenaUtils.*;
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

    public void oppdaterOppfolgingMedStatusFraArena(EndringPaaOppfolgingsBruker endringOppfolgingsbruker) {
        Fnr fnr = Fnr.of(endringOppfolgingsbruker.getFodselsnummer());

        Formidlingsgruppe formidlingsgruppe = ofNullable(endringOppfolgingsbruker.getFormidlingsgruppe()).orElse(null);
        Kvalifiseringsgruppe kvalifiseringsgruppe = ofNullable(endringOppfolgingsbruker.getKvalifiseringsgruppe()).orElse(null);

        Optional<OppfolgingEntity> currentLocalOppfolging = oppfolgingsStatusRepository.hentOppfolging(endringOppfolgingsbruker.getAktorId());

        boolean erBrukerUnderOppfolgingLokalt = currentLocalOppfolging.map(OppfolgingEntity::isUnderOppfolging).orElse(false);
        boolean erUnderOppfolgingIArena = erUnderOppfolging(formidlingsgruppe, kvalifiseringsgruppe);
        boolean erInaktivIArena = erIserv(formidlingsgruppe);
        boolean harBlittSykmeldtUtenArbeidsgiver = harBlittSykmeldtUtenArbeidsgiver(
                endringOppfolgingsbruker,
                currentLocalOppfolging.flatMap(OppfolgingEntity::getLocalArenaOppfolging).orElse(null)
        );

        secureLog.info(
                "Status for automatisk oppdatering av oppfølging."
                        + " aktorId={} erUnderOppfølgingIVeilarboppfolging={}"
                        + " erUnderOppfølgingIArena={} erInaktivIArena={}"
                        + " formidlingsgruppe={} kvalifiseringsgruppe={}",
                endringOppfolgingsbruker.getAktorId(), erBrukerUnderOppfolgingLokalt,
                erUnderOppfolgingIArena, erInaktivIArena,
                formidlingsgruppe, kvalifiseringsgruppe
        );

        var harIngenOppfolgingLagret = currentLocalOppfolging.isEmpty();
        oppfolgingService.oppdaterArenaOppfolgingStatus(
                endringOppfolgingsbruker.getAktorId(),
                harIngenOppfolgingLagret,
                new LocalArenaOppfolging(
                        endringOppfolgingsbruker.getHovedmaal(),
                        kvalifiseringsgruppe,
                        formidlingsgruppe,
                        Optional.ofNullable(endringOppfolgingsbruker.getOppfolgingsenhet()).map(EnhetId::new).orElse(null),
                        endringOppfolgingsbruker.getIservFraDato()
                )
        );

        if (skalOppfolges) {
            secureLog.info("Starter oppfølging på bruker som er under oppfølging i Arena, men ikke i veilarboppfolging. aktorId={}", endringOppfolgingsbruker.getAktorId());
            startOppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(
                    OppfolgingsRegistrering.Companion.arenaSyncOppfolgingBrukerRegistrering(
                            fnr,
                            endringOppfolgingsbruker.getAktorId(),
                            formidlingsgruppe,
                            kvalifiseringsgruppe,
                            EnhetId.of(endringOppfolgingsbruker.getOppfolgingsenhet())
                    )
            );
        } else if (erBrukerUnderOppfolgingLokalt && erInaktivIArena) {
            Optional<Boolean> kanEnkeltReaktiveresLokalt = kanEnkeltReaktiveresLokalt(currentLocalOppfolging, endringOppfolgingsbruker);
            var maybeKanEnkeltReaktiveres = arenaOppfolgingService.kanEnkeltReaktiveres(fnr);

            if (kanEnkeltReaktiveresLokalt.isPresent() && maybeKanEnkeltReaktiveres.isPresent()) {
                if (!kanEnkeltReaktiveresLokalt.get().equals(maybeKanEnkeltReaktiveres.get())) {
                    log.warn("kunne ikke si om bruker kunne reaktiveres lokalt " +
                                    "\n kanReaktiveres lokalt {} kanReaktiveres remote {}" +
                                    "\n iservDato: {}, kvalifiseringsGruppe: {}, forrige lagrede formidlingsgruppe: {}",
                            kanEnkeltReaktiveresLokalt.get(),
                            maybeKanEnkeltReaktiveres.get(),
                            endringOppfolgingsbruker.getIservFraDato(),
                            endringOppfolgingsbruker.getKvalifiseringsgruppe(),
                            currentLocalOppfolging.get().getLocalArenaOppfolging().map(LocalArenaOppfolging::getFormidlingsgruppe).orElse(null)
                    );
                }
            }

            if (maybeKanEnkeltReaktiveres.isPresent()) {
                boolean kanEnkeltReaktiveres = maybeKanEnkeltReaktiveres.get();
                boolean erUnderKvp = kvpService.erUnderKvp(endringOppfolgingsbruker.getAktorId());
                boolean harAktiveTiltaksdeltakelser = oppfolgingService.harAktiveTiltaksdeltakelser(fnr);
                boolean erDeltakerIUngdomsprogrammet = oppfolgingService.erDeltakerIUngdomsprogrammet(fnr);
                boolean erArbeidssoeker = oppfolgingService.erArbeidssoeker(fnr);
                boolean harAap = oppfolgingService.harAap(fnr);
                boolean skalAvsluttes = !kanEnkeltReaktiveres && !erUnderKvp && !harAktiveTiltaksdeltakelser && !erDeltakerIUngdomsprogrammet && !erArbeidssoeker && !harAap;

                secureLog.info(
                        "Status for automatisk avslutting av oppfølging. aktorId={} kanEnkeltReaktiveres={} erUnderKvp={} harAktiveTiltaksdeltakelser={} erDeltakerIUngdomsprogrammet={} erArbeidssoeker={} harAap={} skalAvsluttes={}",
                        endringOppfolgingsbruker.getAktorId(), kanEnkeltReaktiveres, erUnderKvp, harAktiveTiltaksdeltakelser, erDeltakerIUngdomsprogrammet, erArbeidssoeker, harAap, skalAvsluttes
                );

                if (skalAvsluttes) {
                    secureLog.info("Automatisk avslutting av oppfølging på bruker. aktorId={}", endringOppfolgingsbruker.getAktorId());
                    log.info("Utgang: Oppfølging avsluttet automatisk pga. inaktiv bruker som ikke kan reaktiveres");
                    var avregistrering = new ArenaIservKanIkkeReaktiveres(endringOppfolgingsbruker.getAktorId());
                    oppfolgingService.avsluttOppfolging(avregistrering);
                    metricsService.rapporterAutomatiskAvslutningAvOppfolging(true);
                }
            } else {
                secureLog.warn("Bruker har ikke oppfølgingtilstand i Arena. aktorId={}", endringOppfolgingsbruker.getAktorId());
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
