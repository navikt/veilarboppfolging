package no.nav.veilarboppfolging.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarboppfolging.domain.IservMapper;
import no.nav.veilarboppfolging.domain.OppfolgingTable;
import no.nav.veilarboppfolging.domain.VeilarbArenaOppfolgingEndret;
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository;
import no.nav.veilarboppfolging.repository.UtmeldingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static no.nav.veilarboppfolging.service.IservService.AvslutteOppfolgingResultat.*;
import static no.nav.veilarboppfolging.utils.ArenaUtils.erIserv;
import static no.nav.veilarboppfolging.utils.ArenaUtils.erUnderOppfolging;

@Slf4j
@Service
public class IservService {

    enum AvslutteOppfolgingResultat {
        AVSLUTTET_OK,
        IKKE_AVSLUTTET,
        IKKE_LENGER_UNDER_OPPFØLGING,
        AVSLUTTET_FEILET
    }

    private final MetricsService metricsService;
    private final UtmeldingRepository utmeldingRepository;
    private final OppfolgingService oppfolgingService;
    private final OppfolgingsStatusRepository oppfolgingsStatusRepository;
    private final OppfolgingRepositoryService oppfolgingRepositoryService;
    private final AuthService authService;

    public IservService(
            MetricsService metricsService,
            UtmeldingRepository utmeldingRepository,
            OppfolgingService oppfolgingService,
            OppfolgingsStatusRepository oppfolgingsStatusRepository,
            OppfolgingRepositoryService oppfolgingRepositoryService,
            AuthService authService
    ) {
        this.metricsService = metricsService;
        this.utmeldingRepository = utmeldingRepository;
        this.oppfolgingService = oppfolgingService;
        this.oppfolgingsStatusRepository = oppfolgingsStatusRepository;
        this.oppfolgingRepositoryService = oppfolgingRepositoryService;
        this.authService = authService;
    }


    /**
     * Brukes av Iserv28Schedule for å automatisk avslutte oppfølging av brukere som har vært ISERV i mer enn 28 dager
     */
    public void automatiskAvslutteOppfolging() {
        long start = System.currentTimeMillis();
        List<AvslutteOppfolgingResultat> resultater = finnBrukereOgAvslutt();
        log.info("Avslutter jobb for automatisk avslutning av brukere. Tid brukt: {} ms. Antall [Avsluttet/Ikke avsluttet/Ikke lenger under oppfølging/Feilet/Totalt]: [{}/{}/{}/{}/{}]",
                System.currentTimeMillis() - start,
                resultater.stream().filter(r -> r == AVSLUTTET_OK).count(),
                resultater.stream().filter(r -> r == IKKE_AVSLUTTET).count(),
                resultater.stream().filter(r -> r == IKKE_LENGER_UNDER_OPPFØLGING).count(),
                resultater.stream().filter(r -> r == AVSLUTTET_FEILET).count(),
                resultater.size());
    }

    @Transactional
    public void behandleEndretBruker(VeilarbArenaOppfolgingEndret oppfolgingEndret) {
        log.info("Behandler bruker: {}", oppfolgingEndret);

        if (erIserv(oppfolgingEndret.getFormidlingsgruppekode())) {
            oppdaterUtmeldingTabell(oppfolgingEndret);
        } else {
            utmeldingRepository.slettBrukerFraUtmeldingTabell(oppfolgingEndret.getAktoerid());

            if (erUnderOppfolging(oppfolgingEndret.getFormidlingsgruppekode(), oppfolgingEndret.getKvalifiseringsgruppekode())) {
                if (brukerHarOppfolgingsflagg(oppfolgingEndret.getAktoerid())) {
                    log.info("Bruker med aktørid {} er allerede under oppfølging", oppfolgingEndret.getAktoerid());
                } else {
                    startOppfolging(oppfolgingEndret);
                }
            }
        }
    }

    private List<AvslutteOppfolgingResultat> finnBrukereOgAvslutt() {
        List<AvslutteOppfolgingResultat> resultater = new ArrayList<>();
        try {
            log.info("Starter jobb for automatisk avslutning av brukere");
            List<IservMapper> iservert28DagerBrukere = utmeldingRepository.finnBrukereMedIservI28Dager();
            log.info("Fant {} brukere som har vært ISERV mer enn 28 dager", iservert28DagerBrukere.size());

            // TODO: Check if we need to wrap with new subject
            // withSubject(systemUserSubjectProvider.getSystemUserSubject(), () -> {});

            resultater.addAll(iservert28DagerBrukere.stream()
                    .map(iservMapper -> avslutteOppfolging(iservMapper.aktor_Id))
                    .collect(toList()));

        } catch (Exception e) {
            log.error("Feil ved automatisk avslutning av brukere", e);
        }
        return resultater;
    }

    private void startOppfolging(VeilarbArenaOppfolgingEndret oppfolgingEndret) {
        log.info("Starter oppfølging automatisk for bruker med aktørid {}", oppfolgingEndret.getAktoerid());
        oppfolgingRepositoryService.startOppfolgingHvisIkkeAlleredeStartet(oppfolgingEndret.getAktoerid());
        metricsService.startetOppfolgingAutomatisk(oppfolgingEndret.getFormidlingsgruppekode(), oppfolgingEndret.getKvalifiseringsgruppekode());
    }

    private void oppdaterUtmeldingTabell(VeilarbArenaOppfolgingEndret oppfolgingEndret) {
        if (finnesIUtmeldingTabell(oppfolgingEndret)) {
            utmeldingRepository.updateUtmeldingTabell(oppfolgingEndret);
        } else if (brukerHarOppfolgingsflagg(oppfolgingEndret.getAktoerid())) {
            utmeldingRepository.insertUtmeldingTabell(oppfolgingEndret);
        }
    }

    private boolean brukerHarOppfolgingsflagg(String aktoerId) {
        OppfolgingTable eksisterendeOppfolgingstatus = oppfolgingsStatusRepository.fetch(aktoerId);
        return eksisterendeOppfolgingstatus != null && eksisterendeOppfolgingstatus.isUnderOppfolging();
    }

    private boolean finnesIUtmeldingTabell(VeilarbArenaOppfolgingEndret oppfolgingEndret) {
        return utmeldingRepository.eksisterendeIservBruker(oppfolgingEndret) != null;
    }

    AvslutteOppfolgingResultat avslutteOppfolging(String aktoerId) {
        AvslutteOppfolgingResultat resultat;

        try {
            if (!brukerHarOppfolgingsflagg(aktoerId)) {
                log.info("Bruker med aktørid {} har ikke oppfølgingsflagg. Sletter fra utmelding-tabell", aktoerId);
                utmeldingRepository.slettBrukerFraUtmeldingTabell(aktoerId);
                resultat = IKKE_LENGER_UNDER_OPPFØLGING;
            } else {
                String fnr = authService.getFnrOrThrow(aktoerId);

                boolean oppfolgingAvsluttet = oppfolgingService.avsluttOppfolgingForSystemBruker(
                        fnr,
                        "System",
                        "Oppfolging avsluttet autmatisk for grunn av iservert 28 dager"
                );

                resultat = oppfolgingAvsluttet ? AVSLUTTET_OK : IKKE_AVSLUTTET;

                if (oppfolgingAvsluttet) {
                    utmeldingRepository.slettBrukerFraUtmeldingTabell(aktoerId);
                    metricsService.antallBrukereAvsluttetAutomatisk();
                }
            }
        } catch (Exception e) {
            log.error("Automatisk avsluttOppfolging feilet for aktoerid {} ", aktoerId, e);
            resultat = AVSLUTTET_FEILET;
        }

        return resultat;
    }

}
