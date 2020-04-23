package no.nav.fo.veilarboppfolging.db;

import no.nav.fo.veilarboppfolging.domain.AktorId;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

@Component
public class OppfolgingKafkaFeiletMeldingRepository {
    private static final String TABLE_NAME = "KAFKA_OPPFOLGING_FEILET_MLD";
    private static final String AKTOR_ID = "AKTOR_ID";
    private final JdbcTemplate db;

    @Inject
    public OppfolgingKafkaFeiletMeldingRepository(JdbcTemplate db) {
        this.db = db;
    }

    public int insertFeiletMelding(AktorId aktorId) {
        return SqlUtils
                .insert(db, TABLE_NAME)
                .value(AKTOR_ID, aktorId.getAktorId())
                .execute();
    }

    public List<AktorId> hentFeiledeMeldinger() {
        return SqlUtils
                .select(db, TABLE_NAME, rs -> new AktorId(rs.getString(AKTOR_ID)))
                .column("*")
                .executeToList();
    }

    public int deleteFeiletMelding(AktorId aktorId) {
        return SqlUtils
                .delete(db, TABLE_NAME)
                .where(WhereClause.equals(AKTOR_ID, aktorId.getAktorId()))
                .execute();
    }
}
