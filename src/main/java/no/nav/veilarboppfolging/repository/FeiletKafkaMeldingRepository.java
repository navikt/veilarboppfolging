package no.nav.veilarboppfolging.repository;

import lombok.SneakyThrows;
import no.nav.veilarboppfolging.domain.FeiletKafkaMelding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.List;

import static java.lang.String.format;

@Repository
public class FeiletKafkaMeldingRepository {

    public final static String FEILET_KAFKA_MELDING_TABLE               = "FEILET_KAFKA_MELDING";
    private final static String ID                                      = "ID";
    private final static String TOPIC_NAME                              = "TOPIC_NAME";
    private final static String MESSAGE_KEY                             = "MESSAGE_KEY";
    private final static String JSON_PAYLOAD                            = "JSON_PAYLOAD";

    private final JdbcTemplate db;

    @Autowired
    public FeiletKafkaMeldingRepository(JdbcTemplate db) {
        this.db = db;
    }

    public void lagreFeiletKafkaMelding(String topicName, String messageKey, String payload) {
        String sql = format(
                "INSERT INTO %s (%s, %s, %s) VALUES (?, ?, ?)",
                FEILET_KAFKA_MELDING_TABLE, TOPIC_NAME, MESSAGE_KEY, JSON_PAYLOAD
        );

        db.update(sql, topicName, messageKey, payload);
    }

    public List<FeiletKafkaMelding> hentFeiledeKafkaMeldinger(int maxMeldinger) {
        String sql = format(
                "SELECT * FROM %s FETCH NEXT %d ROWS ONLY",
                FEILET_KAFKA_MELDING_TABLE, maxMeldinger
        );

        return db.query(sql, FeiletKafkaMeldingRepository::mapFeiletKafkaMelding);
    }

    public void slettFeiletKafkaMelding(long feiletMeldingId) {
        db.update(format("DELETE FROM %S WHERE %s = ?", FEILET_KAFKA_MELDING_TABLE, ID), feiletMeldingId);
    }

    @SneakyThrows
    private static FeiletKafkaMelding mapFeiletKafkaMelding(ResultSet rs, int rowNum) {
        return new FeiletKafkaMelding()
                .setId(rs.getLong(ID))
                .setTopicName(rs.getString(TOPIC_NAME))
                .setMessageKey(rs.getString(MESSAGE_KEY))
                .setJsonPayload(rs.getString(JSON_PAYLOAD));
    }

}
