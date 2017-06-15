package no.nav.fo.veilarbsituasjon.db.testdriver;

import java.util.HashMap;
import java.util.Map;

/*
"Fattigmanns-løsning" for å kunne bruke hsql lokalt med oracle syntax
*/
class HsqlSyntaxMapper {

    private static final Map<String, String> syntaxMap = new HashMap<>();
    private static final String NOOP = "SELECT 1 FROM DUAL";

    static {
        map(
                "ALTER TABLE MAL MODIFY (MAL VARCHAR2(500 CHAR))",
                "alter table MAL alter column MAL VARCHAR2(500 CHAR)"
        );
        map(
                "alter table MAL modify (MAL NVARCHAR2(500))",
                "alter table MAL alter column MAL NVARCHAR2(500)"

        );
        map(
                "ALTER TABLE AKTOER_ID_TO_VEILEDER MODIFY (VEILEDER NULL)",
                "alter table AKTOER_ID_TO_VEILEDER alter column VEILEDER VARCHAR(20)"
        );
        map(
                "ALTER TABLE OPPFOLGINGSPERIODE MODIFY (sluttdato TIMESTAMP, oppdatert TIMESTAMP)",
                "alter table OPPFOLGINGSPERIODE alter column sluttdato TIMESTAMP; alter table OPPFOLGINGSPERIODE alter column oppdatert TIMESTAMP;"
        );

        map("END", NOOP);
    }

    private static void map(String oracleSyntax, String hsqlSyntax) {
        syntaxMap.put(oracleSyntax, hsqlSyntax);
    }

    static String hsqlSyntax(String sql) {
        System.out.println(sql + "-------------");
        if (sql.contains("BEGIN") || sql.contains("END LOOP")) {
            return NOOP;
        }
        return syntaxMap.getOrDefault(sql, sql);
    }

}
