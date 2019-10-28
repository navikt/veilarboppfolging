package no.nav.fo.veilarboppfolging.db;

import no.nav.metrics.utils.MetricsUtils;
import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingFeedDTO;
import no.nav.fo.veilarboppfolging.utils.OppfolgingFeedUtil;
import no.nav.sbl.sql.SqlUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Slf4j
@Component
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

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<OppfolgingFeedDTO> hentEndringerEtterTimestamp(Timestamp timestamp, int pageSize) {
        return db.queryForList(
                "SELECT DISTINCT "
                        + "os.AKTOR_ID,"
                        + "os.VEILEDER,"
                        + "os.UNDER_OPPFOLGING,"
                        + "os.NY_FOR_VEILEDER,"
                        + "os.OPPDATERT,"
                        + "os.FEED_ID,"
                        + "m.MANUELL,"
                        + "first_value(op.STARTDATO) over (partition BY os.AKTOR_ID ORDER BY op.STARTDATO DESC) AS STARTDATO "
                        + "FROM "
                        + "VEILARBOPPFOLGING.OPPFOLGINGSPERIODE op,"
                        + "VEILARBOPPFOLGING.OPPFOLGINGSTATUS os LEFT JOIN VEILARBOPPFOLGING.MANUELL_STATUS m ON (os.GJELDENDE_MANUELL_STATUS = m.ID) "
                        + "WHERE os.AKTOR_ID = op.AKTOR_ID "
                        + "AND os.OPPDATERT >= ? "
                        + "AND ROWNUM <= ? "
                        + "ORDER BY os.FEED_ID;",
                timestamp,
                pageSize
        ).stream()
                .map(OppfolgingFeedUtil::mapRadTilOppfolgingFeedDTO)
                .collect(toList());
    }

    @Transactional
    public List<OppfolgingFeedDTO> hentEndringerEtterId(String sinceId, int pageSize) {

        return db.queryForList(
        "SELECT DISTINCT "
                + "os.AKTOR_ID,"
                + "os.VEILEDER,"
                + "os.UNDER_OPPFOLGING,"
                + "os.NY_FOR_VEILEDER,"
                + "os.OPPDATERT,"
                + "os.FEED_ID,"
                + "m.MANUELL,"
                + "first_value(op.STARTDATO) over (partition BY os.AKTOR_ID ORDER BY op.STARTDATO DESC) AS STARTDATO "
                + "FROM "
                + "VEILARBOPPFOLGING.OPPFOLGINGSPERIODE op,"
                + "VEILARBOPPFOLGING.OPPFOLGINGSTATUS os LEFT JOIN VEILARBOPPFOLGING.MANUELL_STATUS m ON (os.GJELDENDE_MANUELL_STATUS = m.ID) "
                + "WHERE os.AKTOR_ID = op.AKTOR_ID "
                + "AND os.FEED_ID >= ? "
                + "AND ROWNUM <= ? "
                + "ORDER BY os.FEED_ID;",
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
