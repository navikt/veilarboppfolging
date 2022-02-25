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

    public Integer getNumberOfUnprocessedMessages() {
        return db.queryForObject("SELECT COUNT(*) FROM KAFKA_PRODUCER_RECORD WHERE CREATED_AT < SYSDATE - INTERVAL '30' MINUTE",
                Integer.class
        );
    }
}
