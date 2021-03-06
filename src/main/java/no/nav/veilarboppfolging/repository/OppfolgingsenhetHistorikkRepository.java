package no.nav.veilarboppfolging.repository;

import no.nav.common.types.identer.AktorId;
import no.nav.veilarboppfolging.repository.entity.OppfolgingsenhetEndringEntity;
import no.nav.veilarboppfolging.utils.DbUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static no.nav.veilarboppfolging.utils.DbUtils.hentZonedDateTime;

@Repository
public class OppfolgingsenhetHistorikkRepository {

    private final JdbcTemplate db;

    @Autowired
    public OppfolgingsenhetHistorikkRepository(JdbcTemplate db) {
        this.db = db;
    }

    public void insertOppfolgingsenhetEndringForAktorId(AktorId aktorId, String enhet) {
        String sql = "INSERT INTO OPPFOLGINGSENHET_ENDRET (aktor_id, enhet, endret_dato, enhet_seq) VALUES(?, ?, CURRENT_TIMESTAMP, ?)";
        db.update(sql, aktorId.get(), enhet, DbUtils.nesteFraSekvens(db, "ENHET_SEQ"));
    }

    public List<OppfolgingsenhetEndringEntity> hentOppfolgingsenhetEndringerForAktorId(AktorId aktorId) {
        String sql = "SELECT enhet, endret_dato FROM OPPFOLGINGSENHET_ENDRET WHERE aktor_id = ? ORDER BY enhet_seq DESC";
        return db.query(sql, OppfolgingsenhetHistorikkRepository::mapper, aktorId.get());
    }

    private static OppfolgingsenhetEndringEntity mapper(ResultSet resultset, int rows) throws SQLException {
        return OppfolgingsenhetEndringEntity.builder()
                .enhet(resultset.getString("enhet"))
                .endretDato(hentZonedDateTime(resultset, "endret_dato"))
                .build();
    }
}
