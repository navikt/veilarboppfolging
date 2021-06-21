package no.nav.veilarboppfolging.utils;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.function.Supplier;

public class DbUtils {

    public static long nesteFraSekvens(JdbcTemplate db, String sekvensNavn) {
        return db.queryForObject("select " + sekvensNavn + ".nextval from dual", Long.class);
    }

    public static ZonedDateTime hentZonedDateTime(ResultSet rs, String kolonneNavn) throws SQLException {
        return Optional.ofNullable(rs.getTimestamp(kolonneNavn))
                .map(timestamp -> timestamp.toLocalDateTime().atZone(ZoneId.systemDefault()))
                .orElse(null);
    }

    public static <T> Optional<T> queryForNullableObject(JdbcTemplate db, String sql, RowMapper<T> rowMapper, Object... args) {
        try {
            return Optional.ofNullable(db.queryForObject(sql, rowMapper, args));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public static <T> Optional<T> queryForNullableObject(Supplier<T> objectSupplier) {
        try {
            return Optional.ofNullable(objectSupplier.get());
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

}
