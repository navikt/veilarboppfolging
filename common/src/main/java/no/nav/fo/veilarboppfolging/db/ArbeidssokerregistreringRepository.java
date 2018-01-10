package no.nav.fo.veilarboppfolging.db;

import lombok.SneakyThrows;
import no.nav.fo.veilarboppfolging.domain.AktorId;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.util.Optional;

public class ArbeidssokerregistreringRepository {

    private JdbcTemplate db;

    private final static String OPPFOLGINGSTATUS = "OPPFOLGINGSTATUS";
    private final static String UNDER_OPPFOLGING = "UNDER_OPPFOLGING";
    private final static String AKTOR_ID = "AKTOR_ID";

    public ArbeidssokerregistreringRepository(JdbcTemplate db) {
        this.db = db;
    }

    public boolean erOppfolgingsflaggSatt(AktorId aktorid) {
        return Optional.ofNullable(SqlUtils.select(db.getDataSource(), OPPFOLGINGSTATUS, ArbeidssokerregistreringRepository::oppfolgignsflaggMapper)
                .column(UNDER_OPPFOLGING)
                .where(WhereClause.equals(AKTOR_ID, aktorid.getAktorId()))
                .execute()).orElse(false);
    }

    @SneakyThrows
    private static boolean oppfolgignsflaggMapper(ResultSet rs) {
        return rs.getBoolean(UNDER_OPPFOLGING);
    }
}
