package no.nav.veilarboppfolging.db;
import no.nav.veilarboppfolging.domain.VeilederTilordningerData;
import no.nav.sbl.sql.DbConstants;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.order.OrderClause;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import static no.nav.sbl.jdbc.Database.hentDato;

@Repository
public class VeilederHistorikkRepository {
    private final JdbcTemplate jdbc;

    @Inject
    public VeilederHistorikkRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insertTilordnetVeilederForAktorId(String aktorId, String veileder) {
        SqlUtils.insert(jdbc, "VEILEDER_TILORDNINGER")
                .value("veileder", veileder)
                .value("aktor_id", aktorId)
                .value("sist_tilordnet", DbConstants.CURRENT_TIMESTAMP)
                .value("tilordning_seq", DbConstants.nextSeq("VEILEDER_TILORDNING_SEQ"))
                .execute();
    }

    public List<VeilederTilordningerData> hentTilordnedeVeiledereForAktorId(String aktorId) {
        return SqlUtils.select(jdbc, "VEILEDER_TILORDNINGER", VeilederHistorikkRepository::mapper)
                .column("veileder")
                .column("sist_tilordnet")
                .where(WhereClause.equals("aktor_id", aktorId))
                .orderBy(OrderClause.desc("tilordning_seq"))
                .executeToList();
    }

    private static VeilederTilordningerData mapper(ResultSet resultSet) throws SQLException {
        return VeilederTilordningerData.builder()
                .veileder(resultSet.getString("veileder"))
                .sistTilordnet(hentDato(resultSet,"sist_tilordnet"))
                .build();
    }
}
