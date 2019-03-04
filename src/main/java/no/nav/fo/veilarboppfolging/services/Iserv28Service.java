package no.nav.fo.veilarboppfolging.services;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.OppfolgingRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingsStatusRepository;
import no.nav.fo.veilarboppfolging.domain.IservMapper;
import no.nav.fo.veilarboppfolging.domain.OppfolgingTable;
import no.nav.fo.veilarboppfolging.mappers.ArenaBruker;
import no.nav.fo.veilarboppfolging.utils.FunksjonelleMetrikker;
import no.nav.metrics.utils.MetricsUtils;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.sql.*;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static no.nav.common.auth.SubjectHandler.withSubject;
import static no.nav.fo.veilarboppfolging.services.ArenaUtils.erIserv;
import static no.nav.fo.veilarboppfolging.services.ArenaUtils.erUnderOppfolging;
import static no.nav.fo.veilarboppfolging.services.Iserv28Service.AvslutteOppfolgingResultat.*;
import static no.nav.sbl.sql.DbConstants.CURRENT_TIMESTAMP;

@Component
@Slf4j
public class Iserv28Service{

    static final String START_OPPFOLGING_TOGGLE = "veilarboppfolging.start.oppfolging.automatisk";

    enum AvslutteOppfolgingResultat {
        AVSLUTTET_OK,
        IKKE_AVSLUTTET,
        IKKE_LENGER_UNDER_OPPFØLGING,
        AVSLUTTET_FEILET
    }

    private final JdbcTemplate jdbc;
    private final OppfolgingService oppfolgingService;
    private final AktorService aktorService;
    private final LockingTaskExecutor taskExecutor;
    private final SystemUserSubjectProvider systemUserSubjectProvider;
    private final OppfolgingsStatusRepository oppfolgingsStatusRepository;
    private final OppfolgingRepository oppfolgingRepository;
    private final UnleashService unleashService;

    private static final int lockAutomatiskAvslutteOppfolgingSeconds = 3600;

    @Inject
    public Iserv28Service(
            JdbcTemplate jdbc,
            OppfolgingService oppfolgingService,
            OppfolgingsStatusRepository oppfolgingsStatusRepository,
            OppfolgingRepository oppfolgingRepository,
            AktorService aktorService,
            LockingTaskExecutor taskExecutor,
            SystemUserSubjectProvider systemUserSubjectProvider,
            UnleashService unleashService
    ){
        this.jdbc = jdbc;
        this.oppfolgingService = oppfolgingService;
        this.oppfolgingsStatusRepository = oppfolgingsStatusRepository;
        this.oppfolgingRepository = oppfolgingRepository;
        this.aktorService = aktorService;
        this.taskExecutor = taskExecutor;
        this.systemUserSubjectProvider = systemUserSubjectProvider;
        this.unleashService = unleashService;
    }

    @Scheduled(cron="0 0 * * * *")
    public void scheduledAvslutteOppfolging() {
        Instant lockAtMostUntil = Instant.now().plusSeconds(lockAutomatiskAvslutteOppfolgingSeconds);
        Instant lockAtLeastUntil = Instant.now().plusSeconds(10);
        taskExecutor.executeWithLock(
                this::automatiskAvslutteOppfolging,
                new LockConfiguration("oppdaterAvlutteOppfolging", lockAtMostUntil, lockAtLeastUntil)
        );
    }

    void automatiskAvslutteOppfolging() {

        MetricsUtils.timed("oppfolging.automatisk.avslutning", () ->   {
            long start = System.currentTimeMillis();
            List<AvslutteOppfolgingResultat> resultater = finnBrukereOgAvslutt();
            log.info("Avslutter jobb for automatisk avslutning av brukere. Tid brukt: {} ms. Antall [Avsluttet/Ikke avsluttet/Ikke lenger under oppfølging/Feilet/Totalt]: [{}/{}/{}/{}/{}]", 
                System.currentTimeMillis() - start,
                resultater.stream().filter(r -> r == AVSLUTTET_OK).count(),
                resultater.stream().filter(r -> r == IKKE_AVSLUTTET).count(),
                resultater.stream().filter(r -> r == IKKE_LENGER_UNDER_OPPFØLGING).count(),
                resultater.stream().filter(r -> r == AVSLUTTET_FEILET).count(),
                resultater.size());
        });
    }

    private List<AvslutteOppfolgingResultat> finnBrukereOgAvslutt() {
        List<AvslutteOppfolgingResultat> resultater = new ArrayList<>();
        try {
            log.info("Starter jobb for automatisk avslutning av brukere");
            List<IservMapper> iservert28DagerBrukere = finnBrukereMedIservI28Dager();
            log.info("Fant {} brukere som har vært ISERV mer enn 28 dager", iservert28DagerBrukere.size());
            withSubject(systemUserSubjectProvider.getSystemUserSubject(), () -> {
                resultater.addAll(iservert28DagerBrukere.stream()
                        .map(iservMapper -> avslutteOppfolging(iservMapper.aktor_Id))
                        .collect(toList()));
            });
        } catch (Exception e) {
            log.error("Feil ved automatisk avslutning av brukere", e);
        }
        return resultater;
    }

    @Transactional
    public void behandleEndretBruker(ArenaBruker arenaBruker) {

        log.info("Behandler bruker: {}", arenaBruker);
    
        if(erIserv(arenaBruker.getFormidlingsgruppekode())) {
            oppdaterUtmeldingTabell(arenaBruker);
        } else {
            slettBrukerFraUtmeldingTabell(arenaBruker.getAktoerid());
            if(erUnderOppfolging(arenaBruker.getFormidlingsgruppekode(), arenaBruker.getKvalifiseringsgruppekode())) {
                if (brukerHarOppfolgingsflagg(arenaBruker.getAktoerid())) {
                    log.info("Bruker med aktørid {} er allerede under oppfølging", arenaBruker.getAktoerid());
                } else {
                    startOppfolging(arenaBruker);
                }
            }
        }
    }

    private void startOppfolging(ArenaBruker arenaBruker) {
        if(unleashService.isEnabled(START_OPPFOLGING_TOGGLE)) {
            log.info("Starter oppfølging automatisk for bruker med aktørid {}", arenaBruker.getAktoerid());
            oppfolgingRepository.startOppfolgingHvisIkkeAlleredeStartet(arenaBruker.getAktoerid());
            FunksjonelleMetrikker.startetOppfolgingAutomatisk();
        } else {
            log.info("Automatisk start av oppfølging er slått av i unleash. Aktørid {}", arenaBruker.getAktoerid());
        }
    }

    private void oppdaterUtmeldingTabell(ArenaBruker arenaBruker) {
        if (finnesIUtmeldingTabell(arenaBruker)) {
            updateUtmeldingTabell(arenaBruker);
        } else if (brukerHarOppfolgingsflagg(arenaBruker.getAktoerid())) {
            insertUtmeldingTabell(arenaBruker);
        }
    }

    private boolean brukerHarOppfolgingsflagg(String aktoerId) {
        OppfolgingTable eksisterendeOppfolgingstatus = oppfolgingsStatusRepository.fetch(aktoerId);
        return eksisterendeOppfolgingstatus != null && eksisterendeOppfolgingstatus.isUnderOppfolging();
    }

    private boolean finnesIUtmeldingTabell(ArenaBruker arenaBruker) {
        return eksisterendeIservBruker(arenaBruker) != null;
    }
    
    IservMapper eksisterendeIservBruker(ArenaBruker arenaBruker){
         return SqlUtils.select(jdbc, "UTMELDING", Iserv28Service::mapper)
                .column("aktor_id")
                .column("iserv_fra_dato")
                .where(WhereClause.equals("aktor_id",arenaBruker.getAktoerid())).execute();
    }

    private void updateUtmeldingTabell(ArenaBruker arenaBruker){
        SqlUtils.update(jdbc, "UTMELDING")
                .set("iserv_fra_dato", Timestamp.from(arenaBruker.getIserv_fra_dato().toInstant()))
                .set("oppdatert_dato", CURRENT_TIMESTAMP)
                .whereEquals("aktor_id", arenaBruker.getAktoerid())
                .execute();

        log.info("ISERV bruker med aktorid {} har blitt oppdatert inn i UTMELDING tabell", arenaBruker.getAktoerid());
    }

    void insertUtmeldingTabell(ArenaBruker arenaBruker) {
        Timestamp iservFraDato = Timestamp.from(arenaBruker.getIserv_fra_dato().toInstant());
        SqlUtils.insert(jdbc, "UTMELDING")
                .value("aktor_id", arenaBruker.getAktoerid())
                .value("iserv_fra_dato", iservFraDato)
                .value("oppdatert_dato", CURRENT_TIMESTAMP)
                .execute();

        log.info("ISERV bruker med aktorid {} og iserv_fra_dato {} har blitt insertert inn i UTMELDING tabell",
                arenaBruker.getAktoerid(),
                iservFraDato
        );
    }
    
    AvslutteOppfolgingResultat avslutteOppfolging(String aktoerId) {
        AvslutteOppfolgingResultat resultat;
        try {
            if(!brukerHarOppfolgingsflagg(aktoerId)) {
                log.info("Bruker med aktørid {} har ikke oppfølgingsflagg. Sletter fra utmelding-tabell", aktoerId);
                slettBrukerFraUtmeldingTabell(aktoerId);
                resultat = IKKE_LENGER_UNDER_OPPFØLGING;
            } else {
                String fnr = aktorService.getFnr(aktoerId).orElseThrow(IllegalStateException::new);
                boolean oppfolgingAvsluttet = oppfolgingService.avsluttOppfolgingForSystemBruker(
                        fnr,
                        "System",
                        "Oppfolging avsluttet autmatisk for grunn av iservert 28 dager"
                );
                resultat = oppfolgingAvsluttet ? AVSLUTTET_OK : IKKE_AVSLUTTET;
                if(oppfolgingAvsluttet) {
                    slettBrukerFraUtmeldingTabell(aktoerId);
                    FunksjonelleMetrikker.antallBrukereAvsluttetAutomatisk();
                }
            }
        } catch (Exception e) {
            log.error("Automatisk avsluttOppfolging feilet for aktoerid {} ", aktoerId, e);
            resultat = AVSLUTTET_FEILET;
        }
        return resultat;
    }

    private void slettBrukerFraUtmeldingTabell(String aktoerId) {
        WhereClause aktoeridClause = WhereClause.equals("aktor_id", aktoerId);
        int slettedeRader = SqlUtils.delete(jdbc, "UTMELDING").where(aktoeridClause).execute();
        if(slettedeRader > 0) {
            log.info("Aktorid {} har blitt slettet fra UTMELDING tabell", aktoerId);
        }
    }

    public List<IservMapper> finnBrukereMedIservI28Dager() {
        Timestamp tilbake28 = Timestamp.valueOf(LocalDateTime.now().minusDays(28));
        WhereClause harAktoerId = WhereClause.isNotNull("aktor_id");
        WhereClause iservDato28DagerTilbake = WhereClause.lt("iserv_fra_dato", tilbake28);

        return SqlUtils.select(jdbc, "UTMELDING", Iserv28Service::mapper)
                .column("aktor_id")
                .column("iserv_fra_dato")
                .where(harAktoerId.and(iservDato28DagerTilbake))
                .executeToList();
    }

    private static IservMapper mapper(ResultSet resultSet) throws SQLException {
        return new IservMapper(
                resultSet.getString("aktor_id"),
                resultSet.getTimestamp("iserv_fra_dato").toLocalDateTime().atZone(ZoneId.systemDefault())
        );
    }

}
