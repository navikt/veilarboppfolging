package no.nav.veilarboppfolging.service;

import com.nimbusds.jwt.JWTParser;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.context.AuthContext;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.UserRole;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto_schema.enums.arena.Formidlingsgruppe;
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2;
import no.nav.veilarboppfolging.repository.UtmeldingRepository;
import no.nav.veilarboppfolging.repository.entity.UtmeldingEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static no.nav.veilarboppfolging.service.IservService.AvslutteOppfolgingResultat.*;
import static no.nav.veilarboppfolging.utils.ArenaUtils.erIserv;

@Slf4j
@Service
public class IservService {

    enum AvslutteOppfolgingResultat {
        AVSLUTTET_OK,
        IKKE_AVSLUTTET,
        IKKE_LENGER_UNDER_OPPFØLGING,
        AVSLUTTET_FEILET
    }

    private final AuthContextHolder authContextHolder;
    private final SystemUserTokenProvider systemUserTokenProvider;
    private final MetricsService metricsService;
    private final UtmeldingRepository utmeldingRepository;
    private final OppfolgingService oppfolgingService;
    private final AuthService authService;

    public IservService(
            AuthContextHolder authContextHolder,
            SystemUserTokenProvider systemUserTokenProvider,
            MetricsService metricsService,
            UtmeldingRepository utmeldingRepository,
            OppfolgingService oppfolgingService,
            AuthService authService
    ) {
        this.authContextHolder = authContextHolder;
        this.systemUserTokenProvider = systemUserTokenProvider;
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

    public void behandleEndretBruker(EndringPaaOppfoelgingsBrukerV2 brukerV2) {
        AktorId aktorId = authService.getAktorIdOrThrow(Fnr.of(brukerV2.getFodselsnummer()));

        String formidlingsgruppe = ofNullable(brukerV2.getFormidlingsgruppe()).map(Formidlingsgruppe::toString).orElse(null);

        if (erIserv(formidlingsgruppe)) {
            log.info("Oppdaterer eller insert i utmelding tabell. aktorId={}", aktorId);
            oppdaterUtmeldingTabell(brukerV2);
        } else {
            log.info("Sletter fra utmelding tabell. aktorId={}", aktorId);
            utmeldingRepository.slettBrukerFraUtmeldingTabell(aktorId);
        }
    }

    private List<AvslutteOppfolgingResultat> finnBrukereOgAvslutt() {
        List<AvslutteOppfolgingResultat> resultater = new ArrayList<>();

        try {
            log.info("Starter jobb for automatisk avslutning av brukere");
            List<UtmeldingEntity> iservert28DagerBrukere = utmeldingRepository.finnBrukereMedIservI28Dager();
            log.info("Fant {} brukere som har vært ISERV mer enn 28 dager", iservert28DagerBrukere.size());


            var context = new AuthContext(
                    UserRole.SYSTEM,
                    JWTParser.parse(systemUserTokenProvider.getSystemUserToken())
            );

            authContextHolder.withContext(context, () ->
                    resultater.addAll(iservert28DagerBrukere.stream()
                            .map(utmeldingEntity -> avslutteOppfolging(AktorId.of(utmeldingEntity.aktor_Id)))
                            .collect(toList()))
            );

        } catch (Exception e) {
            log.error("Feil ved automatisk avslutning av brukere", e);
        }

        return resultater;
    }

    private void oppdaterUtmeldingTabell(EndringPaaOppfoelgingsBrukerV2 oppfolgingEndret) {
        AktorId aktorId = authService.getAktorIdOrThrow(Fnr.of(oppfolgingEndret.getFodselsnummer()));
        LocalDate iservFraDato = oppfolgingEndret.getIservFraDato();

        if (iservFraDato == null) {
            log.error("Kan ikke oppdatere utmeldingstabell med bruker siden iservFraDato mangler. aktorId={}", aktorId);
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
                log.info("Bruker med aktørid {} har ikke oppfølgingsflagg. Sletter fra utmelding-tabell", aktorId);
                utmeldingRepository.slettBrukerFraUtmeldingTabell(aktorId);
                resultat = IKKE_LENGER_UNDER_OPPFØLGING;
            } else {
                Fnr fnr = authService.getFnrOrThrow(aktorId);

                boolean oppfolgingAvsluttet = oppfolgingService.avsluttOppfolgingForSystemBruker(fnr);

                resultat = oppfolgingAvsluttet ? AVSLUTTET_OK : IKKE_AVSLUTTET;

                if (oppfolgingAvsluttet) {
                    utmeldingRepository.slettBrukerFraUtmeldingTabell(aktorId);
                    metricsService.antallBrukereAvsluttetAutomatisk();
                }
            }
        } catch (Exception e) {
            log.error("Automatisk avsluttOppfolging feilet for aktoerid {} ", aktorId, e);
            resultat = AVSLUTTET_FEILET;
        }

        return resultat;
    }

}
