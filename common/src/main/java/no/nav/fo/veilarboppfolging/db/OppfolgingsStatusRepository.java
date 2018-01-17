package no.nav.fo.veilarboppfolging.db;

import lombok.SneakyThrows;
import no.nav.fo.veilarboppfolging.domain.Oppfolging;
import no.nav.fo.veilarboppfolging.domain.OppfolgingTable;
import no.nav.sbl.jdbc.Database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class OppfolgingsStatusRepository {

    static final String GJELDENE_ESKALERINGSVARSEL = "gjeldende_eskaleringsvarsel";
    static final String GJELDENDE_BRUKERVILKAR = "gjeldende_brukervilkar";
    static final String GJELDENDE_MAL = "gjeldende_mal";
    static final String GJELDENDE_MANUELL_STATUS = "gjeldende_manuell_status";
    static final String AKTOR_ID = "aktor_id";
    static final String UNDER_OPPFOLGING = "under_oppfolging";
    static final String TABLE_NAME = "OPPFOLGINGSTATUS";
    static final String VEILEDER = "veileder";
    static final String NY_FOR_VEILEDER = "ny_for_veileder";
    static final String SIST_TILORDNET = "sist_tilordnet";
    static final String OPPDATERT = "oppdatert";

    private Database db;

    public OppfolgingsStatusRepository(Database db) {
        this.db = db;
    }

    public OppfolgingTable fetch(String aktorId) {
        List<OppfolgingTable> t = db.query(
                "SELECT * FROM OPPFOLGINGSTATUS WHERE aktor_id = ?",
                OppfolgingsStatusRepository::map,
                aktorId
        );
        return !t.isEmpty() ? t.get(0) : null;
    }

    public Oppfolging create(String aktorId) {
        db.update("INSERT INTO OPPFOLGINGSTATUS(" +
                        "aktor_id, " +
                        "under_oppfolging, " +
                        "oppdatert) " +
                        "VALUES(?, ?, CURRENT_TIMESTAMP)",
                aktorId,
                false);

        // FIXME: return the actual database object.
        return new Oppfolging().setAktorId(aktorId).setUnderOppfolging(false);
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
    private static Boolean erUnderOppfolging(ResultSet resultSet) {
        return resultSet.getBoolean(UNDER_OPPFOLGING);
    }

    public static OppfolgingTable map(ResultSet r) throws SQLException {
        return new OppfolgingTable()
                .setAktorId(r.getString(AKTOR_ID))
                .setGjeldendeBrukervilkarId(r.getLong(GJELDENDE_BRUKERVILKAR))
                .setGjeldendeManuellStatusId(r.getLong(GJELDENDE_MANUELL_STATUS))
                .setGjeldendeMaalId(r.getLong(GJELDENDE_MAL))
                .setGjeldendeEskaleringsvarselId(r.getLong(GJELDENE_ESKALERINGSVARSEL))
                .setGjeldendeKvpId(r.getLong("gjeldende_kvp"))
                .setVeilederId(r.getString(VEILEDER))
                .setUnderOppfolging(r.getBoolean(UNDER_OPPFOLGING));
    }
}
