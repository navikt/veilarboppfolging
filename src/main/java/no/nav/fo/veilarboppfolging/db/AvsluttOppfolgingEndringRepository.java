package no.nav.fo.veilarboppfolging.db;

import lombok.SneakyThrows;
import no.nav.fo.veilarboppfolging.domain.AvsluttOppfolgingKafkaDTO;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.util.Date;
import java.util.List;

@Component
public class AvsluttOppfolgingEndringRepository {
    private final static String KAFKA_TABLE = "FEILEDE_KAFKA_AVSLUTT_OPPFOLGING_BRUKERE";
    private final static String AKTOR_ID = "AKTOR_ID";
    private final static String SLUTTDATO = "SLUTTDATO";

    @Inject
    private JdbcTemplate db;

    public void insertFeiletBruker(AvsluttOppfolgingKafkaDTO avsluttOppfolgingKafkaDTO) {
        SqlUtils.insert(db, KAFKA_TABLE)
                .value(AKTOR_ID, avsluttOppfolgingKafkaDTO.getAktorId())
                .value(SLUTTDATO, avsluttOppfolgingKafkaDTO.getSluttdato())
                .execute();
    }

    public void deleteFeiletBruker(String aktorId) {
        SqlUtils.delete(db, KAFKA_TABLE)
                .where(WhereClause.equals(AKTOR_ID, aktorId))
                .execute();
    }

    public List<AvsluttOppfolgingKafkaDTO> hentFeiledeBrukere() {
        return SqlUtils.select(db, KAFKA_TABLE, AvsluttOppfolgingEndringRepository::avsluttOppfolgingKafkaDTOMapper).executeToList();
    }

    @SneakyThrows
    private static AvsluttOppfolgingKafkaDTO avsluttOppfolgingKafkaDTOMapper(ResultSet resultSet){
        Date sluttdato = new Date(resultSet.getTimestamp(SLUTTDATO).toInstant().toEpochMilli());
        return new AvsluttOppfolgingKafkaDTO()
                .setAktorId(resultSet.getString(AKTOR_ID))
                .setSluttdato(sluttdato);
    }
}
