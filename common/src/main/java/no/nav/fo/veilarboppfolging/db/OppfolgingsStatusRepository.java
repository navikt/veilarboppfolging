package no.nav.fo.veilarboppfolging.db;

import lombok.SneakyThrows;
import no.nav.fo.veilarboppfolging.domain.*;
import no.nav.sbl.jdbc.Database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class OppfolgingsStatusRepository {

    public static final String TABLE_NAME = "OPPFOLGINGSTATUS";
    public static final String UNDER_OPPFOLGING = "under_oppfolging";

    private Database db;

    public OppfolgingsStatusRepository(Database db) {
        this.db = db;
    }

    public OppfolgingTable fetch(String aktorId) {
        List<OppfolgingTable> t = db.query("" +
                "SELECT * FROM OPPFOLGINGSTATUS WHERE aktor_id = ?",
                OppfolgingsStatusRepository::map,
                aktorId
        );
        return t.size() > 0 ? t.get(0) : null;
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

    @SneakyThrows
    public static Boolean erUnderOppfolging(ResultSet resultSet) {
        return resultSet.getBoolean(UNDER_OPPFOLGING);
    }

    public static OppfolgingTable map(ResultSet r) throws SQLException {
        return new OppfolgingTable()
                .setAktorId(r.getString("aktor_id"))
                .setGjeldendeBrukervilkarId(r.getLong("gjeldende_brukervilkar"))
                .setGjeldendeManuellStatusId(r.getLong("gjeldende_manuell_status"))
                .setGjeldendeMaalId(r.getLong("gjeldende_mal"))
                .setGjeldendeEskaleringsvarselId(r.getLong("gjeldende_eskaleringsvarsel"))
                .setVeilederId(r.getString("veileder"))
                .setUnderOppfolging(r.getBoolean("under_oppfolging"));
    }
}
