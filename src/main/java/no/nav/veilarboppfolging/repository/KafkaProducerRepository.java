package no.nav.veilarboppfolging.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Repository
public class KafkaProducerRepository {
    private final JdbcTemplate db;

    @Autowired
    public KafkaProducerRepository(JdbcTemplate db) {
        this.db = db;
    }

    public Long getOldestMessage() {
        Timestamp oldestMessage = db.queryForObject("SELECT MIN(OPPDATERT) FROM KAFKA_PRODUCER_RECORD", Timestamp.class);

        if (oldestMessage != null){
            return ChronoUnit.MINUTES.between(oldestMessage.toLocalDateTime(), LocalDate.now());
        }
        return 0l;
    }
}
