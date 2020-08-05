package no.nav.veilarboppfolging.repository;
import no.nav.veilarboppfolging.domain.VeilederTilordningerData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static no.nav.veilarboppfolging.utils.DbUtils.hentDato;

@Repository
public class VeilederHistorikkRepository {

    private final JdbcTemplate db;

    @Autowired
    public VeilederHistorikkRepository(JdbcTemplate db) {
        this.db = db;
    }

    public void insertTilordnetVeilederForAktorId(String aktorId, String veileder) {
        String sql = "INSERT INTO VEILEDER_TILORDNINGER (veileder, aktor_id, sist_tilordnet, tilordning_seq) " +
                "VALUES(?,?, CURRENT_TIMESTAMP, VEILEDER_TILORDNING_SEQ.NEXTVAL)";

        db.update(sql, veileder, aktorId);
    }

    public List<VeilederTilordningerData> hentTilordnedeVeiledereForAktorId(String aktorId) {
        String sql = "SELECT * FROM VEILEDER_TILORDNINGER WHERE aktor_id = ? ORDER BY tilordning_seq DESC";
        return db.query(sql, VeilederHistorikkRepository::mapper, aktorId);
    }

    private static VeilederTilordningerData mapper(ResultSet resultSet, int rows) throws SQLException {
        return VeilederTilordningerData.builder()
                .veileder(resultSet.getString("veileder"))
                .sistTilordnet(hentDato(resultSet, "sist_tilordnet"))
                .build();
    }
}