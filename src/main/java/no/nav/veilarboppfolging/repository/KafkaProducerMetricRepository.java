package no.nav.veilarboppfolging.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Repository
@Slf4j
public class KafkaProducerMetricRepository {
    private final JdbcTemplate db;

    @Autowired
    public KafkaProducerMetricRepository(JdbcTemplate db) {
        this.db = db;
    }

    public Long getOldestMessage() {
        try {
            Timestamp oldestMessage = db.queryForObject("SELECT MIN(CREATED_AT) FROM KAFKA_PRODUCER_RECORD", Timestamp.class);

            if (oldestMessage != null){
                return ChronoUnit.MINUTES.between(oldestMessage.toLocalDateTime(), LocalDate.now());
            }
            return 0l;
        }
        catch (Exception e){
            return 0l;
        }
    }
}
