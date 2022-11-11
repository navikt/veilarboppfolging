package no.nav.veilarboppfolging.repository;

import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.utils.DbUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.Optional;

import static no.nav.veilarboppfolging.utils.DbUtils.queryForNullableObject;

@Repository
public class SisteEndringPaaOppfolgingBrukerRepository {

    private final JdbcTemplate db;

    @Autowired
    public SisteEndringPaaOppfolgingBrukerRepository(JdbcTemplate db) {
        this.db = db;
    }

    public int insertSisteEndringForFnr(Fnr fnr, ZonedDateTime endringDato) {
        String sql = "INSERT INTO SISTE_ENDRING_OPPFOELGING_BRUKER (FODSELSNR, SIST_ENDRET_DATO) VALUES(?, ?)";
        return db.update(sql, fnr.get(), Timestamp.valueOf(endringDato.toLocalDateTime()));
    }

    public int oppdatereSisteEndringForFnr(Fnr fnr, ZonedDateTime endringDato) {
        String sql = "UPDATE SISTE_ENDRING_OPPFOELGING_BRUKER SET SIST_ENDRET_DATO = ? WHERE FODSELSNR = ?";
        return db.update(sql, Timestamp.valueOf(endringDato.toLocalDateTime()), fnr.get());
    }

    public Optional<ZonedDateTime> hentSisteEndringForFnr(Fnr fnr) {
        String sql = "SELECT SIST_ENDRET_DATO FROM SISTE_ENDRING_OPPFOELGING_BRUKER WHERE FODSELSNR = ?";
        return queryForNullableObject(() -> db.queryForObject(sql, (rs, rowNum) ->  DbUtils.hentZonedDateTime(rs, "SIST_ENDRET_DATO"), fnr.get()));
    }
}
