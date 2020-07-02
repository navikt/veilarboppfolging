package no.nav.veilarboppfolging.db;

import no.nav.veilarboppfolging.domain.OppfolgingsenhetEndringData;
import no.nav.veilarboppfolging.internal.OppfolgingEnhetDTO;
import no.nav.veilarboppfolging.utils.DbUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static no.nav.veilarboppfolging.utils.DbUtils.hentDato;

@Repository
public class OppfolgingsenhetHistorikkRepository {

    private final JdbcTemplate db;

    @Autowired
    public OppfolgingsenhetHistorikkRepository(JdbcTemplate db) {
        this.db = db;
    }

    public void insertOppfolgingsenhetEndring(List<OppfolgingEnhetDTO> dtoer) {
        dtoer.forEach(dto -> insertOppfolgingsenhetEndringForAktorId(dto.getAktorId(), dto.getEnhetId()));
    }

    public void insertOppfolgingsenhetEndringForAktorId(String aktorId, String enhet) {
        String sql = "INSERT INTO OPPFOLGINGSENHET_ENDRET (aktor_id, enhet, endret_dato, enhet_seq) VALUES(?, ?, CURRENT_TIMESTAMP, ?)";
        db.update(sql, aktorId, enhet, DbUtils.nesteFraSekvens(db, "ENHET_SEQ"));
    }

    public List<OppfolgingsenhetEndringData> hentOppfolgingsenhetEndringerForAktorId(String aktorId) {
        String sql = "SELECT enhet, endret_dato FROM OPPFOLGINGSENHET_ENDRET WHERE aktor_id = ? ORDER BY enhet_seq DESC";
        return db.query(sql, OppfolgingsenhetHistorikkRepository::mapper, aktorId);
    }

    private static OppfolgingsenhetEndringData mapper(ResultSet resultset, int rows) throws SQLException {
        return OppfolgingsenhetEndringData.builder()
                .enhet(resultset.getString("enhet"))
                .endretDato(hentDato(resultset, "endret_dato"))
                .build();
    }

    public void truncateOppfolgingsenhetEndret() {
        db.execute("TRUNCATE TABLE OPPFOLGINGSENHET_ENDRET");
    }

}
