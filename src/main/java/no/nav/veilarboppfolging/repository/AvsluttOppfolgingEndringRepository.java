package no.nav.veilarboppfolging.repository;

import lombok.SneakyThrows;
import no.nav.veilarboppfolging.domain.kafka.AvsluttOppfolgingKafkaDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class AvsluttOppfolgingEndringRepository {

    private final JdbcTemplate db;

    @Autowired
    public AvsluttOppfolgingEndringRepository(JdbcTemplate db) {
        this.db = db;
    }

    @Transactional
    public void insertAvsluttOppfolgingBruker(String aktorId, LocalDateTime sluttdato) {
        String sql = "INSERT INTO KAFKA_AVSLUTT_OPPFOLGING (AKTOR_ID, SLUTTDATO) VALUES(?,?)";
        db.update(sql, aktorId, Timestamp.valueOf(sluttdato));
    }

    public void deleteAvsluttOppfolgingBruker(String aktorId, LocalDateTime sluttdato) {
        String sql = "DELETE FROM KAFKA_AVSLUTT_OPPFOLGING WHERE AKTOR_ID = ? AND SLUTTDATO = ?";
        db.update(sql, aktorId, Timestamp.valueOf(sluttdato));
    }

    public List<AvsluttOppfolgingKafkaDTO> hentAvsluttOppfolgingBrukere() {
        String sql = "SELECT * FROM KAFKA_AVSLUTT_OPPFOLGING";
        return db.query(sql, AvsluttOppfolgingEndringRepository::avsluttOppfolgingKafkaDTOMapper);
    }

    @SneakyThrows
    private static AvsluttOppfolgingKafkaDTO avsluttOppfolgingKafkaDTOMapper(ResultSet resultSet, int row) {
        return new AvsluttOppfolgingKafkaDTO()
                .setAktorId(resultSet.getString("AKTOR_ID"))
                .setSluttdato(resultSet.getTimestamp("SLUTTDATO").toLocalDateTime());
    }
}
