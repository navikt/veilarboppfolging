package no.nav.veilarboppfolging.repository;


import lombok.SneakyThrows;
import no.nav.common.types.identer.AktorId;
import no.nav.veilarboppfolging.repository.entity.VeilederTilordningEntity;
import no.nav.veilarboppfolging.utils.DbUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.Optional;

import static java.lang.String.format;
import static no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository.*;
import static no.nav.veilarboppfolging.utils.DbUtils.queryForNullableObject;

@Repository
public class VeilederTilordningerRepository {

    private final JdbcTemplate db;

    @Autowired
    public VeilederTilordningerRepository(JdbcTemplate db) {
        this.db = db;
    }

    public String hentTilordningForAktoer(AktorId aktorId) {
        return hentTilordnetVeileder(aktorId)
                .map(VeilederTilordningEntity::getVeilederId)
                .orElse(null);
    }

    public Optional<VeilederTilordningEntity> hentTilordnetVeileder(AktorId aktorId) {
        String sql = format("SELECT * FROM %s WHERE %s = ?", TABLE_NAME, AKTOR_ID);
        return queryForNullableObject(db, sql, VeilederTilordningerRepository::map, aktorId.get());
    }

    public void upsertVeilederTilordning(AktorId aktorId, String veilederId) {
        String insertSql = "INSERT INTO OPPFOLGINGSTATUS(aktor_id, veileder, under_oppfolging, ny_for_veileder, sist_tilordnet, oppdatert) " +
                "SELECT ?, ?, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM DUAL " +
                "WHERE NOT EXISTS(SELECT * FROM OPPFOLGINGSTATUS WHERE aktor_id = ?)";

        int rowsUpdated = db.update(insertSql, aktorId.get(), veilederId, aktorId.get());

        if (rowsUpdated == 0) {
            String updateSql = "UPDATE OPPFOLGINGSTATUS SET veileder = ?, ny_for_veileder = 1, " +
                    "sist_tilordnet = CURRENT_TIMESTAMP, oppdatert = CURRENT_TIMESTAMP WHERE aktor_id = ?";

            db.update(updateSql, veilederId, aktorId.get());
        }

    }

    public int markerSomLestAvVeileder(AktorId aktorId) {
        String sql = "UPDATE OPPFOLGINGSTATUS SET ny_for_veileder = ?, oppdatert = CURRENT_TIMESTAMP WHERE aktor_id = ?";
        return db.update(sql, 0, aktorId.get());
    }

    @SneakyThrows
    private static VeilederTilordningEntity map(ResultSet resultSet, int row) {
        return new VeilederTilordningEntity()
                .setAktorId(resultSet.getString(AKTOR_ID))
                .setOppfolging(resultSet.getBoolean(UNDER_OPPFOLGING))
                .setVeilederId(resultSet.getString(VEILEDER))
                .setNyForVeileder(resultSet.getBoolean(NY_FOR_VEILEDER))
                .setSistTilordnet(DbUtils.hentZonedDateTime(resultSet, SIST_TILORDNET))
                .setSistOppdatert(DbUtils.hentZonedDateTime(resultSet, OPPDATERT));
    }
}
