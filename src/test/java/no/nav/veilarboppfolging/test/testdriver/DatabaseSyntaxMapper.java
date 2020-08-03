package no.nav.veilarboppfolging.test.testdriver;

import java.util.HashMap;
import java.util.Map;

/*
"Fattigmanns-løsning" for å kunne bruke hsql lokalt med oracle syntax
 NB: Har byttet til h2 så mye av disse kan funke hvis syntaxen var rett
*/
class DatabaseSyntaxMapper {

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
        map(
                "alter table OPPFOLGINGSPERIODE modify (AKTORID VARCHAR2(20))",
                "alter table OPPFOLGINGSPERIODE alter column AKTORID VARCHAR2(20)"
        );
        map(
                "ALTER TABLE STATUS MODIFY OPPRETTET_AV VARCHAR(255) NOT NULL",
                "ALTER TABLE STATUS alter column OPPRETTET_AV VARCHAR(255); ALTER TABLE STATUS alter column OPPRETTET_AV SET NOT NULL;"
        );
        map(
                "ALTER TABLE STATUS MODIFY BEGRUNNELSE VARCHAR(500)",
                "ALTER TABLE STATUS alter column BEGRUNNELSE VARCHAR(500)"
        );
        map(
                "ALTER TABLE STATUS MODIFY BEGRUNNELSE NVARCHAR2(500)",
                "ALTER TABLE STATUS alter column BEGRUNNELSE NVARCHAR2(500)"
        );
        map(
                "ALTER TABLE KODEVERK_BRUKER MODIFY ENDRET NOT NULL",
                "ALTER TABLE KODEVERK_BRUKER alter column ENDRET SET NOT NULL;"
        );
        map(
                "ALTER TABLE KODEVERK_BRUKER MODIFY ENDRET_AV NOT NULL",
                "ALTER TABLE KODEVERK_BRUKER alter column ENDRET_AV SET NOT NULL;"
        );
        map(
                "ALTER TABLE MAL MODIFY DATO NOT NULL",
                "ALTER TABLE MAL alter column  DATO SET NOT NULL;"
        );
        map(
                "ALTER TABLE MAL MODIFY ENDRET_AV NOT NULL",
                "ALTER TABLE MAL alter column  ENDRET_AV SET NOT NULL;"
        );
        map(
                "CREATE SEQUENCE BRUKER_REGISTRERING_SEQ ORDER",
                "CREATE SEQUENCE BRUKER_REGISTRERING_SEQ"
        );
        map(
                "CREATE SEQUENCE NYE_BRUKERE_FEED_SEQ ORDER",
                "CREATE SEQUENCE NYE_BRUKERE_FEED_SEQ"
        );
        map(
                "CREATE SEQUENCE OPPFOLGING_FEED_SEQ ORDER",
                "CREATE SEQUENCE OPPFOLGING_FEED_SEQ"
        );
        map("END", NOOP);
    }

    private static void map(String oracleSyntax, String hsqlSyntax) {
        syntaxMap.put(oracleSyntax, hsqlSyntax);
    }

    static String h2Syntax(final String orginialSql) {
        String fixedSql = orginialSql;

        if (orginialSql.contains("BEGIN") || orginialSql.contains("END LOOP")) {
            fixedSql = NOOP;
        }

        if (orginialSql.contains("GENERATED ALWAYS AS IDENTITY(START WITH 1 INCREMENT BY 1)")) {
            fixedSql = orginialSql.replaceAll("GENERATED ALWAYS AS IDENTITY\\(START WITH 1 INCREMENT BY 1\\)", "NOT NULL");
        }

        /*
         Fikser en bug i Flyway som gjør at installed_by ikke blir satt i Oracle modus med H2.
         Dette har blitt fikset i en nyere versjon av Flyway, men denne versjonen støtter ikke gammle Oracle databaser som vi i dag bruker.
        */
        if (orginialSql.contains("installed_by")) {
            fixedSql = orginialSql.replace("\"installed_by\" VARCHAR(100) NOT NULL", "\"installed_by\" VARCHAR(100) DEFAULT 'local_test'");
        }

        fixedSql = syntaxMap.getOrDefault(fixedSql, fixedSql);

        if (fixedSql.equals(orginialSql)) {
            System.out.println(orginialSql);
        } else {
            System.out.println("Original SQL: " + orginialSql + "\n--------\nFixed SQL: " + fixedSql);
        }

        return fixedSql;
    }

}
