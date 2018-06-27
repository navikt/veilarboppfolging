package no.nav.fo.veilarboppfolging.db;

import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingFeedDTO;
import no.nav.fo.veilarboppfolging.utils.OppfolgingFeedUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class OppfolgingFeedRepository {

    private JdbcTemplate db;

    public OppfolgingFeedRepository(JdbcTemplate db) {
        this.db = db;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<OppfolgingFeedDTO> hentTilordningerEtterTimestamp(Timestamp timestamp, int pageSize) {
        return db.queryForList("SELECT * FROM "
                        + "(SELECT o.aktor_id, o.veileder, o.under_oppfolging, o.ny_for_veileder, o.oppdatert, m.manuell "
                        + "FROM OPPFOLGINGSTATUS o LEFT JOIN MANUELL_STATUS m ON (o.GJELDENDE_MANUELL_STATUS = m.ID) "
                        + "where o.oppdatert >= ? ORDER BY o.oppdatert) "
                        + "WHERE rownum <= ?",
                timestamp,
                pageSize
        ).stream()
                .map(OppfolgingFeedUtil::mapRadTilOppfolgingFeedDTO)
                .collect(toList());
    }
    
    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void settIdeerPaFeedElementer() {
        db.update(
                "UPDATE OPPFOLGINGSTATUS " + 
                "SET FEED_ID = OPPFOLGING_FEED_SEQ.NEXTVAL " +
                "WHERE FEED_ID IS NULL");
    }

}
