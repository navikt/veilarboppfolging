package no.nav.veilarboppfolging.db;


import lombok.SneakyThrows;
import no.nav.veilarboppfolging.domain.Tilordning;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.Optional;

import static java.lang.String.format;
import static no.nav.veilarboppfolging.db.OppfolgingsStatusRepository.*;

@Repository
public class VeilederTilordningerRepository {

    private final JdbcTemplate db;

    @Autowired
    public VeilederTilordningerRepository(JdbcTemplate db) {
        this.db = db;
    }

    public String hentTilordningForAktoer(String aktorId) {
        return hentTilordnetVeileder(aktorId)
                .map(Tilordning::getVeilederId)
                .orElse(null);
    }

    public Optional<Tilordning> hentTilordnetVeileder(String aktorId) {
        String sql = format("SELECT * FROM %s WHERE %s = ?", TABLE_NAME, AKTOR_ID);
        return Optional.ofNullable(db.queryForObject(sql, VeilederTilordningerRepository::map, aktorId));
    }

    public void upsertVeilederTilordning(String aktoerId, String veileder) {
        String insertSql = "INSERT INTO OPPFOLGINGSTATUS(aktor_id, veileder, under_oppfolging, ny_for_veileder, sist_tilordnet, oppdatert, FEED_ID) " +
                "SELECT ?, ?, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null FROM DUAL " +
                "WHERE NOT EXISTS(SELECT * FROM OPPFOLGINGSTATUS WHERE aktor_id = ?)";

        int rowsUpdated = db.update(insertSql, aktoerId, veileder, aktoerId);

        if (rowsUpdated == 0) {
            String updateSql = "UPDATE OPPFOLGINGSTATUS SET veileder = ?, ny_for_veileder = 1, " +
                    "sist_tilordnet = CURRENT_TIMESTAMP, oppdatert = CURRENT_TIMESTAMP, FEED_ID = null WHERE aktor_id = ?";

            db.update(updateSql, veileder, aktoerId);
        }

    }

    public int markerSomLestAvVeileder(String aktorId) {
        String sql = "UPDATE OPPFOLGINGSTATUS SET ny_for_veileder = ?, oppdatert = CURRENT_TIMESTAMP, FEED_ID = null WHERE aktor_id = ?";
        return db.update(sql, 0, aktorId);
    }

    @SneakyThrows
    private static Tilordning map(ResultSet resultSet, int row) {
        return new Tilordning()
                .setAktorId(resultSet.getString(AKTOR_ID))
                .setOppfolging(resultSet.getBoolean(UNDER_OPPFOLGING))
                .setVeilederId(resultSet.getString(VEILEDER))
                .setNyForVeileder(resultSet.getBoolean(NY_FOR_VEILEDER))
                .setSistTilordnet(resultSet.getDate(SIST_TILORDNET))
                .setSistOppdatert(resultSet.getDate(OPPDATERT));
    }
}
