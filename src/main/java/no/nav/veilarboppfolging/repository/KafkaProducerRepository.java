package no.nav.veilarboppfolging.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class KafkaProducerRepository {
    private final JdbcTemplate db;

    @Autowired
    public KafkaProducerRepository(JdbcTemplate db) {
        this.db = db;
    }

    public Integer getOldestMessage() {
        return db.queryForObject("SELECT MAX(ROUND(((SYSDATE+0)-(OPPDATERT+0)) * 24 * 60)) as minutes_processed_ago FROM KAFKA_PRODUCER_RECORD",
                Integer.class
        );
    }
}
