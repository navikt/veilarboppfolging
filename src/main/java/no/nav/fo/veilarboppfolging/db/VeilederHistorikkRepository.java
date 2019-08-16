package no.nav.fo.veilarboppfolging.db;
import no.nav.fo.veilarboppfolging.domain.VeilederTilordningerData;
import no.nav.sbl.sql.DbConstants;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Component
public class VeilederHistorikkRepository {

    private final JdbcTemplate jdbc;

    @Inject
    public VeilederHistorikkRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insertTilordnetVeilederForAktorId(String aktorId, String veileder) {
        SqlUtils.insert(jdbc, "VEILEDER_TILLORDNINGER")
                .value("veileder", veileder)
                .value("aktorId", aktorId)
                .value("sist_tilordnet", DbConstants.CURRENT_TIMESTAMP)
                .execute();
    }

    public List<VeilederTilordningerData> hentTilordnedeVeiledereForAktorId(String aktorId) {
        return SqlUtils.select(jdbc, "VEILEDER_TILLORDNINGER", VeilederHistorikkRepository::mapper)
                .column("veileder")
                .column("sist_tilordnet")
                .where(WhereClause.equals("aktor_id", aktorId))
                .executeToList();
    }

    private static VeilederTilordningerData mapper(ResultSet resultSet) throws SQLException {
        return new VeilederTilordningerData(
                resultSet.getString("veileder"),
                resultSet.getDate("sist_tilordnet")
        );
    }
}
