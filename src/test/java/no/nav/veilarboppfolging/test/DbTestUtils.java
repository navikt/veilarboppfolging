package no.nav.veilarboppfolging.test;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.List;

public class DbTestUtils {

    private final static List<String> ALL_TABLES = Arrays.asList(
            "OPPFOLGINGSPERIODE",
            "OPPFOLGINGSTATUS"
    );

    public static void cleanupTestDb() {
        ALL_TABLES.forEach((table) -> deleteAllFromTable(LocalH2Database.getDb(), table));
    }

    private static void deleteAllFromTable(JdbcTemplate db, String tableName) {
        db.execute("TRUNCATE TABLE " + tableName);
    }

}
