package no.nav.fo.veilarboppfolging.services;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import no.nav.common.auth.Subject;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.OppfolgingsStatusRepository;
import no.nav.fo.veilarboppfolging.domain.IservMapper;
import no.nav.fo.veilarboppfolging.domain.OppfolgingTable;
import no.nav.fo.veilarboppfolging.mappers.ArenaBruker;
import no.nav.fo.veilarboppfolging.utils.FunksjonelleMetrikker;
import no.nav.metrics.utils.MetricsUtils;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.sql.*;
import java.time.*;
import java.util.List;

import static no.nav.common.auth.SubjectHandler.withSubject;
import static no.nav.sbl.sql.DbConstants.CURRENT_TIMESTAMP;

@Component
@Slf4j
public class Iserv28Service{

    private final JdbcTemplate jdbc;
    private final OppfolgingService oppfolgingService;
    private final AktorService aktorService;
    private final LockingTaskExecutor taskExecutor;
    private final SystemUserSubjectProvider systemUserSubjectProvider;
    private final OppfolgingsStatusRepository oppfolgingsStatusRepository;

    private static final int lockAutomatiskAvslutteOppfolgingSeconds = 3600;

    @Inject
    public Iserv28Service(
            JdbcTemplate jdbc,
            OppfolgingService oppfolgingService,
            OppfolgingsStatusRepository oppfolgingsStatusRepository,
            AktorService aktorService,
            LockingTaskExecutor taskExecutor,
            SystemUserSubjectProvider systemUserSubjectProvider
    ){
        this.jdbc = jdbc;
        this.oppfolgingService = oppfolgingService;
        this.oppfolgingsStatusRepository = oppfolgingsStatusRepository;
        this.aktorService = aktorService;
        this.taskExecutor = taskExecutor;
        this.systemUserSubjectProvider = systemUserSubjectProvider;
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

    private void automatiskAvslutteOppfolging() {

        MetricsUtils.timed("oppfolging.automatisk.avslutning", () ->   {
            long start = System.currentTimeMillis();
            try {
                log.info("Starter jobb for automatisk avslutning av brukere");
                List<IservMapper> iservert28DagerBrukere = finnBrukereMedIservI28Dager();
                log.info("Fant {} brukere som har vært ISERV mer enn 28 dager", iservert28DagerBrukere.size());
                if (!iservert28DagerBrukere.isEmpty()) {
                    Subject subject = systemUserSubjectProvider.getSystemUserSubject();
                    withSubject(subject, () -> {
                        iservert28DagerBrukere.forEach(iservMapper -> avslutteOppfolging(iservMapper.aktor_Id));
                    });
                }
            } catch (Exception e) {
                log.error("Feil ved automatisk avslutning av brukere", e);
            }
            log.info("Avslutter jobb for automatisk avslutning av brukere. Tid brukt: {} ms", System.currentTimeMillis() - start);
        });
    }

    public void filterereIservBrukere(ArenaBruker arenaBruker){
        boolean erIserv = "ISERV".equals(arenaBruker.getFormidlingsgruppekode());
        IservMapper eksisterendeIservBruker = eksisterendeIservBruker(arenaBruker);

        try {
            if (eksisterendeIservBruker != null && !erIserv) {
                slettAvsluttetOppfolgingsBruker(arenaBruker.getAktoerid());
            } else if (eksisterendeIservBruker != null) {
                updateIservBruker(arenaBruker);
            } else if(erIserv && brukerHarOppfolgingsflagg(arenaBruker.getAktoerid())) {
                insertIservBruker(arenaBruker);
            }
        }
        catch(Exception e){
            log.error("Exception ved filterereIservBrukere: {}" , e);
        }
    }

    private boolean brukerHarOppfolgingsflagg(String aktoerId) {
        OppfolgingTable eksisterendeOppfolgingstatus = oppfolgingsStatusRepository.fetch(aktoerId);
        boolean harOppfolgingsflagg = eksisterendeOppfolgingstatus != null && eksisterendeOppfolgingstatus.isUnderOppfolging();
        log.info("ISERV bruker med aktorid {} har oppfolgingsflagg: {}", aktoerId, harOppfolgingsflagg);
        return harOppfolgingsflagg;
    }

    public IservMapper eksisterendeIservBruker(ArenaBruker arenaBruker){
         return SqlUtils.select(jdbc, "UTMELDING", Iserv28Service::mapper)
                .column("aktor_id")
                .column("iserv_fra_dato")
                .where(WhereClause.equals("aktor_id",arenaBruker.getAktoerid())).execute();
    }

    private void updateIservBruker(ArenaBruker arenaBruker){
        SqlUtils.update(jdbc, "UTMELDING")
                .set("iserv_fra_dato", Timestamp.from(arenaBruker.getIserv_fra_dato().toInstant()))
                .set("oppdatert_dato", CURRENT_TIMESTAMP)
                .whereEquals("aktor_id", arenaBruker.getAktoerid())
                .execute();

        log.info("ISERV bruker med aktorid {} har blitt oppdatert inn i UTMELDING tabell", arenaBruker.getAktoerid());
    }

    void insertIservBruker(ArenaBruker arenaBruker) {
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

    void avslutteOppfolging(String aktoerId) {
        try {
            if(!brukerHarOppfolgingsflagg(aktoerId)) {
                log.info("Bruker med aktørid {} har ikke oppfølgingsflagg. Sletter fra utmelding-tabell", aktoerId);
                slettAvsluttetOppfolgingsBruker(aktoerId);
            } else {
                String fnr = aktorService.getFnr(aktoerId).orElseThrow(IllegalStateException::new);
                boolean oppfolgingAvsluttet = oppfolgingService.avsluttOppfolgingForSystemBruker(
                        fnr,
                        "System",
                        "Oppfolging avsluttet autmatisk for grunn av iservert 28 dager"
                );
                if(oppfolgingAvsluttet) {
                    slettAvsluttetOppfolgingsBruker(aktoerId);
                    FunksjonelleMetrikker.antallBrukereAvsluttetAutomatisk();
                }
            }
        } catch (Exception e) {
            log.error("Automatisk avsluttOppfolging feilet for aktoerid {} ", aktoerId, e);
        }
    }

    private void slettAvsluttetOppfolgingsBruker(String aktoerId) {
        WhereClause aktoeridClause = WhereClause.equals("aktor_id", aktoerId);
        SqlUtils.delete(jdbc, "UTMELDING").where(aktoeridClause).execute();
        log.info("Aktorid {} har blitt slettet fra UTMELDING tabell", aktoerId);
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
