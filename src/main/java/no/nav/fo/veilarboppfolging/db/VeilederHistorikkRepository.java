package no.nav.fo.veilarboppfolging.db;
import no.nav.fo.veilarboppfolging.domain.VeilederTilordningerData;
import no.nav.sbl.sql.DbConstants;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.order.OrderClause;
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

    public void insertTilordnetVeilederForAktorId(String aktorId, String veileder, String innloggetVeilederId) {
        SqlUtils.insert(jdbc, "VEILEDER_TILORDNINGER")
                .value("veileder", veileder)
                .value("aktor_id", aktorId)
                .value("lagt_inn_av_veileder", innloggetVeilederId)
                .value("sist_tilordnet", DbConstants.CURRENT_TIMESTAMP)
                .execute();
    }

    public List<VeilederTilordningerData> hentTilordnedeVeiledereForAktorId(String aktorId) {
        return SqlUtils.select(jdbc, "VEILEDER_TILORDNINGER", VeilederHistorikkRepository::mapper)
                .column("veileder")
                .column("sist_tilordnet")
                .column("lagt_inn_av_veileder")
                .where(WhereClause.equals("aktor_id", aktorId))
                .orderBy(OrderClause.desc("sist_tilordnet"))
                .executeToList();
    }

    private static VeilederTilordningerData mapper(ResultSet resultSet) throws SQLException {
        return VeilederTilordningerData.builder()
                .veileder(resultSet.getString("veileder"))
                .lagtInnAvVeilder(resultSet.getString("lagt_inn_av_veileder"))
                .sistTilordnet(resultSet.getDate("sist_tilordnet"))
                .build();
    }
}
