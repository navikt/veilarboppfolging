package no.nav.fo.veilarboppfolging.db;

import no.nav.metrics.utils.MetricsUtils;
import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingFeedDTO;
import no.nav.fo.veilarboppfolging.utils.OppfolgingFeedUtil;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.order.OrderClause;
import no.nav.sbl.sql.where.WhereClause;
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
        return SqlUtils.select(db, "OPPFOLGINGSTATUS o", OppfolgingFeedUtil::mapRadTilOppfolgingFeedDTO)
                .column("o.aktor_id")
                .column("o.veileder")
                .column("o.under_oppfolging")
                .column("o.ny_for_veileder")
                .column("o.oppdatert")
                .column("o.feed_id")
                .column("m.manuell")
                .column("op.startdato")
                .leftJoinOn("MANUELL_STATUS m", "o.GJELDENDE_MANUELL_STATUS", " m.ID")
                .leftJoinOn("OPFOLGINGSPERIODE op", "o.AKTOR_ID", " op.AKTOR_ID")
                .where(WhereClause.gteq("o.oppdatert", timestamp).and(WhereClause.isNull("op.SLUTTDATO")))
                .orderBy(OrderClause.asc("o.oppdatert"))
                .limit(pageSize)
                .executeToList();
    }

    @Transactional
    public List<OppfolgingFeedDTO> hentEndringerEtterId(String sinceId, int pageSize) {
       return SqlUtils.select(db, "OPPFOLGINGSTATUS o", OppfolgingFeedUtil::mapRadTilOppfolgingFeedDTO)
                .column("o.aktor_id")
                .column("o.veileder")
                .column("o.under_oppfolging")
                .column("o.ny_for_veileder")
                .column("o.oppdatert")
                .column("o.feed_id")
                .column("m.manuell")
                .column("op.startdato")
                .leftJoinOn("MANUELL_STATUS m", "o.GJELDENDE_MANUELL_STATUS", " m.ID")
                .leftJoinOn("OPFOLGINGSPERIODE op", "o.AKTOR_ID", " op.AKTOR_ID")
                .where(WhereClause.gteq("o.feed_id", sinceId).and(WhereClause.isNull("op.SLUTTDATO")))
                .orderBy(OrderClause.asc("o.oppdatert"))
                .limit(pageSize)
                .executeToList();
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
        MetricsUtils.timed("oppfolging.feedid", () ->   {
            long start = System.currentTimeMillis();
            int updatedRows = db.update(
                    "UPDATE OPPFOLGINGSTATUS " +
                    "SET FEED_ID = OPPFOLGING_FEED_SEQ.NEXTVAL " +
                    "WHERE FEED_ID IS NULL");
            if(updatedRows > 0) {
                log.info("Satte feed-id på {} rader. Tid brukt: {} ms", updatedRows, System.currentTimeMillis() - start);
            }
        });
    }
}
