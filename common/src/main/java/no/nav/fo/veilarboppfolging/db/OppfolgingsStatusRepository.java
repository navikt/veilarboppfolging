package no.nav.fo.veilarboppfolging.db;

import lombok.SneakyThrows;
import no.nav.sbl.jdbc.Database;

import java.sql.ResultSet;

public class  OppfolgingsStatusRepository {

    static final String GJELDENE_ESKALERINGSVARSEL = "gjeldende_eskaleringsvarsel";
    static final String GJELDENDE_BRUKERVILKAR = "gjeldende_brukervilkar";
    static final String GJELDENDE_MAL = "gjeldende_mal";
    static final String GJELDENDE_MANUELL_STATUS = "gjeldende_manuell_status";
    static final String AKTOR_ID = "aktor_id";
    static final String UNDER_OPPFOLGING = "under_oppfolging";
    static final String TABLE_NAME = "OPPFOLGINGSTATUS";


    private Database db;

    public OppfolgingsStatusRepository(Database db) {
        this.db = db;
    }

    public Boolean erOppfolgingsflaggSattForBruker(String aktorId) {
        return db.query("" +
                        "SELECT " +
                        "OPPFOLGINGSTATUS.under_oppfolging AS under_oppfolging " +
                        "FROM OPPFOLGINGSTATUS " +
                        "WHERE OPPFOLGINGSTATUS.aktor_id = ? ",
                OppfolgingsStatusRepository::erUnderOppfolging,
                aktorId
        ).get(0);
    }

    public void opprettOppfolging(String aktorId) {
        db.update("INSERT INTO OPPFOLGINGSTATUS(" +
                        "aktor_id, " +
                        "under_oppfolging, " +
                        "oppdatert) " +
                        "VALUES(?, ?, CURRENT_TIMESTAMP)",
                aktorId,
                false);
    }

    @SneakyThrows
    public static Boolean erUnderOppfolging(ResultSet resultSet) {
        return resultSet.getBoolean(UNDER_OPPFOLGING);
    }
}
