package no.nav.veilarboppfolging.utils;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class DbUtils {

    public static long nesteFraSekvens(JdbcTemplate db, String sekvensNavn) {
        return db.queryForObject("select " + sekvensNavn + ".nextval from dual", Long.class);
    }

    public static <T> T firstOrNull(List<T> dataList) {
        return dataList.isEmpty() ? null : dataList.get(0);
    }

    public static Date hentDato(ResultSet rs, String kolonneNavn) throws SQLException {
        return Optional.ofNullable(rs.getTimestamp(kolonneNavn))
                .map(Timestamp::getTime)
                .map(Date::new)
                .orElse(null);
    }

    public static <T> Optional<T> queryForNullableObject(JdbcTemplate db, String sql, RowMapper<T> rowMapper, Object... args) {
        try {
            return Optional.ofNullable(db.queryForObject(sql, rowMapper, args));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}
