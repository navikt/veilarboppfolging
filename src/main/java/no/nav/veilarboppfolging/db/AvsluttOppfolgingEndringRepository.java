package no.nav.veilarboppfolging.db;

import lombok.SneakyThrows;
import no.nav.veilarboppfolging.domain.AvsluttOppfolgingKafkaDTO;
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

    private final static String KAFKA_TABLE = "KAFKA_AVSLUTT_OPPFOLGING";
    private final static String AKTOR_ID = "AKTOR_ID";
    private final static String SLUTTDATO = "SLUTTDATO";

    private final JdbcTemplate db;

    @Autowired
    public AvsluttOppfolgingEndringRepository(JdbcTemplate db) {
        this.db = db;
    }

    @Transactional
    public void insertAvsluttOppfolgingBruker(String aktorId, LocalDateTime sluttdato) {
        Timestamp timestamp = Timestamp.valueOf(sluttdato);
        SqlUtils.insert(db, KAFKA_TABLE)
                .value(AKTOR_ID, aktorId)
                .value(SLUTTDATO, timestamp)
                .execute();
    }

    public void deleteAvsluttOppfolgingBruker(String aktorId, LocalDateTime sluttdato) {
        Timestamp timestamp = Timestamp.valueOf(sluttdato);
        SqlUtils.delete(db, KAFKA_TABLE)
                .where(WhereClause.equals(AKTOR_ID, aktorId).and(WhereClause.equals(SLUTTDATO, timestamp)))
                .execute();
    }

    public List<AvsluttOppfolgingKafkaDTO> hentAvsluttOppfolgingBrukere() {
        return SqlUtils.select(db, KAFKA_TABLE, AvsluttOppfolgingEndringRepository::avsluttOppfolgingKafkaDTOMapper)
                .column("*")
                .executeToList();
    }

    @SneakyThrows
    private static AvsluttOppfolgingKafkaDTO avsluttOppfolgingKafkaDTOMapper(ResultSet resultSet){
        return new AvsluttOppfolgingKafkaDTO()
                .setAktorId(resultSet.getString(AKTOR_ID))
                .setSluttdato(resultSet.getTimestamp(SLUTTDATO).toLocalDateTime());
    }
}
