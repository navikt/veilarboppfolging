package no.nav.fo.veilarboppfolging.services;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.domain.IservMapper;
import no.nav.fo.veilarboppfolging.mappers.ArenaBruker;
import no.nav.fo.veilarboppfolging.utils.FunksjonelleMetrikker;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.sql.*;
import java.time.*;
import java.util.List;

import static no.nav.sbl.sql.DbConstants.CURRENT_TIMESTAMP;

@Component
@Slf4j
public class Iserv28Service{

    private final JdbcTemplate jdbc;
    private final OppfolgingService oppfolgingService;
    private final AktorService aktorService;
    private final LockingTaskExecutor taskExecutor;

    private static final int lockAutomatiskAvslutteOppfolgingSeconds = 3600;

    @Inject
    public Iserv28Service(JdbcTemplate jdbc, OppfolgingService oppfolgingService, AktorService aktorService, LockingTaskExecutor taskExecutor){
        this.jdbc = jdbc;
        this.oppfolgingService = oppfolgingService;
        this.aktorService = aktorService;
        this.taskExecutor = taskExecutor;
    }

    @Scheduled(fixedDelay = 10000L, initialDelay = 1000L)
    public void scheduledAvlutteOppfolging() {
        Instant lockAtMostUntil = Instant.now().plusSeconds(lockAutomatiskAvslutteOppfolgingSeconds);
        Instant lockAtLeastUntil = Instant.now().plusSeconds(10);
        taskExecutor.executeWithLock(
                this::automatiskAvlutteOppfolging,
                new LockConfiguration("oppdaterAvlutteOppfolging", lockAtMostUntil, lockAtLeastUntil)
        );
    }

    private void automatiskAvlutteOppfolging() {
        try {
            List<IservMapper> iservert28DagerBrukere = finnBrukereMedIservI28Dager();
            iservert28DagerBrukere.stream().forEach(iservMapper ->  avslutteOppfolging(iservMapper.aktor_Id));
        } catch(Exception e) {
            log.error("Feil ved automatisk avslutning av brukere", e);
        }
    }

    public void filterereIservBrukere(ArenaBruker arenaBruker){
        boolean erIserv = "ISERV".equals(arenaBruker.getFormidlingsgruppekode());
        IservMapper eksisterendeIservBruker = eksisterendeIservBruker(arenaBruker);

        if (eksisterendeIservBruker != null && !erIserv) {
            slettAvluttetOppfolgingsBruker(arenaBruker.getAktoerid());
        } else if (eksisterendeIservBruker != null) {
            updateIservBruker(arenaBruker);
        } else if (erIserv) {
            insertIservBruker(arenaBruker);
        }
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
        SqlUtils.insert(jdbc, "UTMELDING")
                .value("aktor_id", arenaBruker.getAktoerid())
                .value("iserv_fra_dato", Timestamp.from(arenaBruker.getIserv_fra_dato().toInstant()))
                .value("oppdatert_dato", CURRENT_TIMESTAMP)
                .execute();
        log.info("ISERV bruker med aktorid {} har blitt insertert inn i UTMELDING tabell", arenaBruker.getAktoerid());
    }

    public void avslutteOppfolging(String aktoerId) {
        try {
            String fnr = aktorService.getFnr(aktoerId).orElseThrow(IllegalStateException::new);
            oppfolgingService.avsluttOppfolging(
                    fnr,
                    "System",
                    "Oppfolging avsluttet autmatisk for grunn av iservert 28 dager"
            );
            slettAvluttetOppfolgingsBruker(aktoerId);
            FunksjonelleMetrikker.antallBrukereAvluttetAutomatisk();
        } catch (Exception e) {
            log.error("Automatisk avsluttOppfolging feilet for aktoerid {} ", aktoerId, e);
        }
    }

    private void slettAvluttetOppfolgingsBruker(String aktoerId) {
        WhereClause aktoerid = WhereClause.equals("aktor_id", aktoerId);
        SqlUtils.delete(jdbc, "UTMELDING").where(aktoerid).execute();
        log.info("Aktorid {} har blitt slettet fra UTMELDING tabell", aktoerid);
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
