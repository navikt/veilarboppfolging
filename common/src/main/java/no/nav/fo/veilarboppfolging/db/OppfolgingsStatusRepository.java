package no.nav.fo.veilarboppfolging.db;

import lombok.SneakyThrows;
import no.nav.fo.veilarboppfolging.domain.Brukervilkar;
import no.nav.fo.veilarboppfolging.domain.MalData;
import no.nav.fo.veilarboppfolging.domain.ManuellStatus;
import no.nav.sbl.jdbc.Database;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;

public class  OppfolgingsStatusRepository {

    public static final String TABLE_NAME = "OPPFOLGINGSTATUS";
    public static final String AKTOR_ID = "aktor_id";
    public static final String GJELDENE_ESKALERINGSVARSEL = "gjeldende_eskaleringsvarsel";
    public static final String UNDER_OPPFOLGING = "under_oppfolging";

    private Database db;

    public OppfolgingsStatusRepository(Database db) {
        this.db = db;
    }


    public void setUnderOppfolging(String aktorId) {
        db.update("UPDATE OPPFOLGINGSTATUS " +
                        "SET under_oppfolging = 1, " +
                        "oppdatert = CURRENT_TIMESTAMP " +
                        "WHERE aktor_id = ?",
                aktorId);
    }

    public void avsluttOppfolging(String aktorId){
        db.update("UPDATE OPPFOLGINGSTATUS SET under_oppfolging = 0, "
                        + "veileder = null, "
                        + "gjeldende_manuell_status = null, "
                        + "gjeldende_mal = null, "
                        + "gjeldende_brukervilkar = null, "
                        + "oppdatert = CURRENT_TIMESTAMP "
                        + "WHERE aktor_id = ?",
                aktorId
        );
    }

    public void fjernEskalering(String aktorId) {
        db.update("" +
                        "UPDATE OPPFOLGINGSTATUS " +
                        "SET gjeldende_eskaleringsvarsel = null, " +
                        "oppdatert = CURRENT_TIMESTAMP " +
                        "WHERE aktor_id = ?",
                aktorId
        );
    }
    public void setGjeldendeEskaleringsvarsel(String aktorId, long eskaleringsVarselId) {
        db.update("" +
                        "UPDATE OPPFOLGINGSTATUS " +
                        "SET gjeldende_eskaleringsvarsel = ?, " +
                        "oppdatert = CURRENT_TIMESTAMP " +
                        "WHERE aktor_id = ?",
                eskaleringsVarselId,
                aktorId
        );
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

    @SneakyThrows
    public static Boolean erUnderOppfolging(ResultSet resultSet) {
        return resultSet.getBoolean(UNDER_OPPFOLGING);
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

    public void oppdaterOppfolgingBrukervilkar(Brukervilkar gjeldendeBrukervilkar) {
        db.update("UPDATE OPPFOLGINGSTATUS SET gjeldende_brukervilkar = ?, oppdatert = CURRENT_TIMESTAMP WHERE aktor_id = ?",
                gjeldendeBrukervilkar.getId(),
                gjeldendeBrukervilkar.getAktorId()
        );
    }

    public void oppdaterManuellStatus(ManuellStatus gjeldendeManuellStatus) {
        db.update("UPDATE OPPFOLGINGSTATUS SET gjeldende_manuell_status = ?, oppdatert = CURRENT_TIMESTAMP WHERE aktor_id = ?",
                gjeldendeManuellStatus.getId(),
                gjeldendeManuellStatus.getAktorId()
        );
    }

    public void oppdaterOppfolgingMal(MalData mal) {
        db.update("UPDATE OPPFOLGINGSTATUS SET gjeldende_mal = ?, oppdatert = CURRENT_TIMESTAMP WHERE aktor_id = ?",
                mal.getId(),
                mal.getAktorId()
        );
    }

    public void fjernMaal(String aktorId) {
        db.update("UPDATE OPPFOLGINGSTATUS SET gjeldende_mal = NULL WHERE aktor_id = ?", aktorId);
    }
}
