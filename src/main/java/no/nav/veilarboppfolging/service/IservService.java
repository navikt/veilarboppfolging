package no.nav.veilarboppfolging.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.domain.AvslutningStatusData;
import no.nav.veilarboppfolging.repository.UtmeldingRepository;
import no.nav.veilarboppfolging.repository.entity.UtmeldingEntity;
import no.nav.veilarboppfolging.service.utmelding.KanskjeIservBruker;
import no.nav.veilarboppfolging.service.utmelding.UtmeldingsBruker;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static java.util.Optional.ofNullable;
import static no.nav.veilarboppfolging.config.ApplicationConfig.SYSTEM_USER_NAME;
import static no.nav.veilarboppfolging.service.IservService.AvslutteOppfolgingResultat.*;
import static no.nav.veilarboppfolging.utils.ArenaUtils.erIserv;
import static no.nav.veilarboppfolging.utils.SecureLog.secureLog;

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
    private final AuthService authService;

    public IservService(
            MetricsService metricsService,
            UtmeldingRepository utmeldingRepository,
            OppfolgingService oppfolgingService,
            AuthService authService
    ) {
        this.metricsService = metricsService;
        this.utmeldingRepository = utmeldingRepository;
        this.oppfolgingService = oppfolgingService;
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

    public void oppdaterUtmeldingsStatus(KanskjeIservBruker kanskjeIservBruker) {
        AktorId aktorId = authService.getAktorIdOrThrow(Fnr.of(kanskjeIservBruker.getFnr()));

        var formidlingsgruppe = ofNullable(kanskjeIservBruker.getFormidlingsgruppe()).orElse(null);

        if (erIserv(formidlingsgruppe)) {
            secureLog.info("Oppdaterer eller insert i utmelding tabell. aktorId={}", aktorId);
            oppdaterUtmeldingTabell(kanskjeIservBruker.utmeldingsBruker());
        } else {
            secureLog.info("Sletter fra utmelding tabell. aktorId={}", aktorId);
            utmeldingRepository.slettBrukerFraUtmeldingTabell(aktorId);
        }
    }

    private List<AvslutteOppfolgingResultat> finnBrukereOgAvslutt() {
        List<AvslutteOppfolgingResultat> resultater = new ArrayList<>();

        try {
            log.info("Starter jobb for automatisk avslutning av brukere");
            List<UtmeldingEntity> iservert28DagerBrukere = utmeldingRepository.finnBrukereMedIservI28Dager();
            log.info("Fant {} brukere som har vært ISERV mer enn 28 dager", iservert28DagerBrukere.size());

            resultater.addAll(iservert28DagerBrukere.stream()
                    .map(utmeldingEntity -> avslutteOppfolging(AktorId.of(utmeldingEntity.aktor_Id)))
                    .toList());

        } catch (Exception e) {
            secureLog.error("Feil ved automatisk avslutning av brukere", e);
        }

        return resultater;
    }

    private void oppdaterUtmeldingTabell(UtmeldingsBruker oppfolgingEndret) {
        AktorId aktorId = authService.getAktorIdOrThrow(Fnr.of(oppfolgingEndret.getFnr()));
        LocalDate iservFraDato = oppfolgingEndret.getIservFraDato();

        if (iservFraDato == null) {
            secureLog.error("Kan ikke oppdatere utmeldingstabell med bruker siden iservFraDato mangler. aktorId={}", aktorId);
            throw new IllegalArgumentException("iservFraDato mangler på EndringPaaOppfoelgingsBrukerV2");
        }

        if (finnesIUtmeldingTabell(aktorId)) {
            utmeldingRepository.updateUtmeldingTabell(aktorId, iservFraDato.atStartOfDay(ZoneId.systemDefault()));
        } else if (oppfolgingService.erUnderOppfolging(aktorId)) {
            utmeldingRepository.insertUtmeldingTabell(aktorId, iservFraDato.atStartOfDay(ZoneId.systemDefault()));
        }
    }

    private boolean finnesIUtmeldingTabell(AktorId aktorId) {
        return utmeldingRepository.eksisterendeIservBruker(aktorId).isPresent();
    }

    AvslutteOppfolgingResultat avslutteOppfolging(AktorId aktorId) {
        AvslutteOppfolgingResultat resultat;

        try {
            if (!oppfolgingService.erUnderOppfolging(aktorId)) {
                secureLog.info("Bruker med aktørid {} har ikke oppfølgingsflagg. Sletter fra utmelding-tabell", aktorId);
                utmeldingRepository.slettBrukerFraUtmeldingTabell(aktorId);
                resultat = IKKE_LENGER_UNDER_OPPFØLGING;
            } else {
                Fnr fnr = authService.getFnrOrThrow(aktorId);

                log.info("Utgang: Oppfølging avsluttet automatisk grunnet iserv i 28 dager");
                AvslutningStatusData avslutningStatus = oppfolgingService.avsluttOppfolging(fnr, SYSTEM_USER_NAME, "Oppfølging avsluttet automatisk grunnet iserv i 28 dager");
                // TODO litt i tvil om denne her. Attributtet sier om du per def er under oppfølging i arena, ikke om du er under oppfølging hos oss.
                boolean oppfolgingAvsluttet = !avslutningStatus.underOppfolging;

                resultat = oppfolgingAvsluttet ? AVSLUTTET_OK : IKKE_AVSLUTTET;

                if (oppfolgingAvsluttet) {
                    utmeldingRepository.slettBrukerFraUtmeldingTabell(aktorId);
                    metricsService.antallBrukereAvsluttetAutomatisk();
                }
            }
        } catch (Exception e) {
            secureLog.error("Automatisk avsluttOppfolging feilet for aktoerid {} ", aktorId, e);
            resultat = AVSLUTTET_FEILET;
        }

        return resultat;
    }

}
