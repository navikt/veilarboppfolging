package no.nav.fo.veilarboppfolging.db;

import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingFeedDTO;
import no.nav.fo.veilarboppfolging.utils.OppfolgingFeedUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

import java.sql.Timestamp;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Slf4j
public class OppfolgingFeedRepository {

    private JdbcTemplate db;

    public OppfolgingFeedRepository(JdbcTemplate db) {
        this.db = db;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<OppfolgingFeedDTO> hentEndringerEtterTimestamp(Timestamp timestamp, int pageSize) {
        return db.queryForList("SELECT * FROM "
                        + "(SELECT o.aktor_id, o.veileder, o.under_oppfolging, o.ny_for_veileder, o.oppdatert, o.feed_id, m.manuell "
                        + "FROM OPPFOLGINGSTATUS o LEFT JOIN MANUELL_STATUS m ON (o.GJELDENDE_MANUELL_STATUS = m.ID) "
                        + "where o.oppdatert >= ? ORDER BY o.oppdatert) "
                        + "WHERE rownum <= ?",
                timestamp,
                pageSize
        ).stream()
                .map(OppfolgingFeedUtil::mapRadTilOppfolgingFeedDTO)
                .collect(toList());
    }
    
    @Transactional
    public List<OppfolgingFeedDTO> hentEndringerEtterId(String sinceId, int pageSize) {
        return db.queryForList("SELECT * FROM "
                        + "(SELECT o.aktor_id, o.veileder, o.under_oppfolging, o.ny_for_veileder, o.oppdatert, o.feed_id, m.manuell "
                        + "FROM OPPFOLGINGSTATUS o LEFT JOIN MANUELL_STATUS m ON (o.GJELDENDE_MANUELL_STATUS = m.ID) "
                        + "where o.feed_id >= ? ORDER BY o.feed_id) "
                        + "WHERE rownum <= ?",
                sinceId,
                pageSize
        ).stream()
                .map(OppfolgingFeedUtil::mapRadTilOppfolgingFeedDTO)
                .collect(toList());
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void settIderPaFeedElementer() {
        int updatedRows = db.update(
                "UPDATE OPPFOLGINGSTATUS " + 
                "SET FEED_ID = OPPFOLGING_FEED_SEQ.NEXTVAL " +
                "WHERE FEED_ID IS NULL");
        if(updatedRows > 0) {
            log.info("Satte feed-id på {} rader", updatedRows);
        }
    }

}
