package no.nav.fo.veilarbsituasjon.db.testdriver;

import java.util.HashMap;
import java.util.Map;

/*
"Fattigmanns-løsning" for å kunne bruke hsql lokalt med oracle syntax
*/
class HsqlSyntaxMapper {

    private static final Map<String, String> syntaxMap = new HashMap<>();

    static {
        map(
                "ALTER TABLE MAL MODIFY (MAL VARCHAR2(500 CHAR))",
                "alter table MAL alter column MAL VARCHAR2(500 CHAR)"
        );
        map(
            "alter table MAL modify (MAL NVARCHAR2(500))",
            "alter table MAL alter column MAL NVARCHAR2(500)"
        );
    }

    private static void map(String oracleSyntax, String hsqlSyntax) {
        syntaxMap.put(oracleSyntax, hsqlSyntax);
    }

    static String hsqlSyntax(String sql) {
        return syntaxMap.getOrDefault(sql, sql);
    }

}
