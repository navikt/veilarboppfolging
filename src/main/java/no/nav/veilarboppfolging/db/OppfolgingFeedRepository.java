package no.nav.veilarboppfolging.db;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import no.nav.veilarboppfolging.domain.AktorId;
import no.nav.veilarboppfolging.controller.domain.OppfolgingFeedDTO;
import no.nav.veilarboppfolging.controller.domain.OppfolgingKafkaDTO;
import no.nav.veilarboppfolging.utils.OppfolgingFeedUtil;
import no.nav.metrics.utils.MetricsUtils;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Slf4j
@Repository
public class OppfolgingFeedRepository {

    public static final int INSERT_ID_INTERVAL = 500;
    private static final long ADD_FEED_ID_MAX_LOCK = 10;

    private final JdbcTemplate db;
    private final LockingTaskExecutor taskExecutor;

    @Inject
    public OppfolgingFeedRepository(JdbcTemplate db, LockingTaskExecutor taskExecutor) {
        this.db = db;
        this.taskExecutor = taskExecutor;
    }

    public List<AktorId> hentAlleBrukereUnderOppfolging() {
        return SqlUtils
                .select(db, "OPPFOLGINGSTATUS", rs -> new AktorId(rs.getString("AKTOR_ID")))
                .column("*")
                .where(WhereClause.equals("UNDER_OPPFOLGING", 1))
                .executeToList();
    }

    public Try<OppfolgingKafkaDTO> hentOppfolgingStatus(String aktoerId) {

        val sql = "SELECT "
                + "os.AKTOR_ID, "
                + "os.VEILEDER, "
                + "os.UNDER_OPPFOLGING, "
                + "os.NY_FOR_VEILEDER, "
                + "os.OPPDATERT, "
                + "ms.MANUELL, "
                + "siste_periode.STARTDATO "
                + "from "
                + "OPPFOLGINGSTATUS os LEFT JOIN MANUELL_STATUS ms "
                + "on (os.GJELDENDE_MANUELL_STATUS = ms.ID) "
                + ", "
                + "(select "
                + "AKTOR_ID, "
                + "STARTDATO "
                + "from OPPFOLGINGSPERIODE "
                + "   where AKTOR_ID = ? "
                + "   order by OPPDATERT desc "
                + "   fetch next 1 rows only "
                + "  ) siste_periode "
                + "where os.AKTOR_ID = siste_periode.AKTOR_ID";

        val result = Try.of(() -> db.queryForObject(sql, new Object[]{aktoerId}, rowMapper()));

        if (result.isSuccess() && result.get() == null) {
            return Try.failure(new IllegalStateException("Result was empty"));
        }
        return result;
    }

    public Optional<Long> hentAntallBrukere() {
        val sql = "SELECT "
                + "count(*) "
                + "from "
                + "OPPFOLGINGSTATUS os LEFT JOIN MANUELL_STATUS ms "
                + "on (os.GJELDENDE_MANUELL_STATUS = ms.ID) "
                + ", "
                + "(select "
                + "AKTOR_ID, "
                + "STARTDATO "
                + "from OPPFOLGINGSPERIODE "
                + "where SLUTTDATO is null "
                + "  ) siste_periode "
                + "where os.AKTOR_ID = siste_periode.AKTOR_ID";

        Long count = db.query(sql, rs -> {
            rs.next();
            return rs.getLong(1);
        });
        return Optional.ofNullable(count);
    }

    public List<OppfolgingKafkaDTO> hentOppfolgingStatus(int offset) {

        val sql = "SELECT "
                + "os.AKTOR_ID, "
                + "os.VEILEDER, "
                + "os.UNDER_OPPFOLGING, "
                + "os.NY_FOR_VEILEDER, "
                + "os.OPPDATERT, "
                + "ms.MANUELL, "
                + "siste_periode.STARTDATO "
                + "from "
                + "OPPFOLGINGSTATUS os LEFT JOIN MANUELL_STATUS ms "
                + "on (os.GJELDENDE_MANUELL_STATUS = ms.ID) "
                + ", "
                + "(select "
                + "AKTOR_ID, "
                + "STARTDATO "
                + "from OPPFOLGINGSPERIODE "
                + "where SLUTTDATO is null "
                + "  ) siste_periode "
                + "where os.AKTOR_ID = siste_periode.AKTOR_ID "
                + "OFFSET ? ROWS FETCH NEXT 1000 ROWS ONLY";

        return db.query(sql, new Object[]{offset}, rowMapper());
    }


    private RowMapper<OppfolgingKafkaDTO> rowMapper() {
        return (rs, rowNum) ->
                OppfolgingKafkaDTO
                        .builder()
                        .aktoerid(rs.getString("AKTOR_ID"))
                        .veileder(rs.getString("VEILEDER"))
                        .oppfolging(rs.getBoolean("UNDER_OPPFOLGING"))
                        .nyForVeileder(rs.getBoolean("NY_FOR_VEILEDER"))
                        .endretTimestamp(rs.getTimestamp("OPPDATERT"))
                        .startDato((rs.getTimestamp("STARTDATO")))
                        .manuell(rs.getBoolean("MANUELL"))
                        .build();
    }

    @Transactional
    public List<OppfolgingFeedDTO> hentEndringerEtterId(String sinceId, int pageSize) {

        // 1. Join sammen Tabellen OPPFOLGINGSPERIODE og OPPFOLGINGSTATUS på AKTOR_ID
        // 2. LEFT JOIN inn tabellen MANUELL_STATUS siden ikke alle brukere ligger i denne tabellen (de radene hvor det ikke er noe match på id vil få null i MANUELL-kolonnen)
        // 3. Det er nå flere duplikate rader siden én bruker kan ha flere startdatoer, vi må filtrere ut de radene for en bruker som har den nyeste startdatoen,
        //    partition_by grupperer på AKTOR_ID for så å sortere på startdato hvor den nyeste ligger øverst, first_value velger så den øverste raden.

        return db.queryForList(
        "SELECT DISTINCT "
                + "os.AKTOR_ID,"
                + "os.VEILEDER,"
                + "os.UNDER_OPPFOLGING,"
                + "os.NY_FOR_VEILEDER,"
                + "os.OPPDATERT,"
                + "os.FEED_ID,"
                + "m.MANUELL,"
                // grupper duplikate rader og sorter på startdato, velg så den øverste raden (som da er den med nyeste startdatoen)
                // https://stackoverflow.com/questions/10515391/oracle-equivalent-of-postgres-distinct-on
                + "first_value(op.STARTDATO) over (partition BY os.AKTOR_ID ORDER BY op.STARTDATO DESC) AS STARTDATO "
                + "FROM "
                + "OPPFOLGINGSPERIODE op,"
                // LEFT JOIN fordi ikke alle brukere ligger i MANUELL_STATUS
                // (https://www.oracletutorial.com/oracle-basics/oracle-joins/)
                + "OPPFOLGINGSTATUS os LEFT JOIN MANUELL_STATUS m ON (os.GJELDENDE_MANUELL_STATUS = m.ID) "
                + "WHERE os.AKTOR_ID = op.AKTOR_ID "
                + "AND os.FEED_ID >= ? "
                + "AND ROWNUM <= ? "
                + "ORDER BY os.FEED_ID",
                sinceId,
                pageSize
        ).stream()
                .map(OppfolgingFeedUtil::mapRadTilOppfolgingFeedDTO)
                .collect(toList());
    }

    @Scheduled(fixedDelay = INSERT_ID_INTERVAL)
    @Transactional
    public void settIderPaFeedElementer() {
        insertFeedIdWithLock();
    }

    private void insertFeedIdWithLock() {
        Instant lockAtMostUntil = Instant.now().plusSeconds(ADD_FEED_ID_MAX_LOCK);
        taskExecutor.executeWithLock(
                () -> insertFeedId(),
                new LockConfiguration("oppdaterOppfolgingFeedId", lockAtMostUntil));
    }

    private void insertFeedId() {
        MetricsUtils.timed("oppfolging.feedid", () -> {
            long start = System.currentTimeMillis();
            int updatedRows = db.update(
                    "UPDATE OPPFOLGINGSTATUS " +
                            "SET FEED_ID = OPPFOLGING_FEED_SEQ.NEXTVAL " +
                            "WHERE FEED_ID IS NULL");
            if (updatedRows > 0) {
                log.info("Satte feed-id på {} rader. Tid brukt: {} ms", updatedRows, System.currentTimeMillis() - start);
            }
        });
    }
}