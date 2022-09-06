package no.nav.veilarboppfolging.repository;
import no.nav.common.types.identer.AktorId;
import no.nav.veilarboppfolging.repository.entity.VeilederTilordningHistorikkEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static no.nav.veilarboppfolging.utils.DbUtils.hentZonedDateTime;

@Repository
public class VeilederHistorikkRepository {

    private final JdbcTemplate db;

    @Autowired
    public VeilederHistorikkRepository(JdbcTemplate db) {
        this.db = db;
    }

    public void insertTilordnetVeilederForAktorId(AktorId aktorId, String veileder, String tilordnetAvVeileder) {
        String sql = "INSERT INTO VEILEDER_TILORDNINGER (veileder, aktor_id, sist_tilordnet, tilordning_seq, tilordnet_av_veileder) " +
                "VALUES(?,?, CURRENT_TIMESTAMP, VEILEDER_TILORDNING_SEQ.NEXTVAL, ?)";

        db.update(sql, veileder, aktorId.get(), tilordnetAvVeileder);
    }

    public List<VeilederTilordningHistorikkEntity> hentTilordnedeVeiledereForAktorId(AktorId aktorId) {
        String sql = "SELECT * FROM VEILEDER_TILORDNINGER WHERE aktor_id = ? ORDER BY tilordning_seq DESC";
        return db.query(sql, VeilederHistorikkRepository::mapper, aktorId.get());
    }

    private static VeilederTilordningHistorikkEntity mapper(ResultSet resultSet, int rows) throws SQLException {
        return VeilederTilordningHistorikkEntity.builder()
                .veileder(resultSet.getString("veileder"))
                .sistTilordnet(hentZonedDateTime(resultSet, "sist_tilordnet"))
                .tilordnetAvVeileder(resultSet.getString("tilordnet_av_veileder"))
                .build();
    }
}
