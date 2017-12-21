package no.nav.fo.veilarboppfolging.db;

import lombok.SneakyThrows;
import no.nav.fo.veilarboppfolging.domain.MalData;
import no.nav.sbl.jdbc.Database;

import java.sql.ResultSet;
import java.util.Date;
import java.util.List;

public class MaalRepository {
    private Database database;

    public MaalRepository(Database database) {
        this.database = database;
    }

    @SneakyThrows
    public List<MalData> aktorMal(String aktorId) {
        return database.query("SELECT * FROM MAL WHERE aktor_id = ? ORDER BY ID DESC",
                MaalRepository::map,
                aktorId);
    }

    public MalData fetch(Long id) {
        String sql = "SELECT * FROM MAL WHERE id = ?";
        return database.query(sql, MaalRepository::map, id).get(0);
    }

    // Creates a goal. Remember to update the OPPFOLGINGSSTATUS table with the current goal as well, if applicable.
    protected void opprett(MalData maal) {
        database.update("" +
                        "INSERT INTO MAL(id, aktor_id, mal, endret_av, dato) " +
                        "VALUES(?, ?, ?, ?, ?)",
                maal.getId(),
                maal.getAktorId(),
                maal.getMal(),
                maal.getEndretAv(),
                maal.getDato()
        );
    }

    protected void slettForAktorEtter(String aktorId, Date date) {
        database.update("DELETE FROM MAL WHERE aktor_id = ? AND dato > ?", aktorId, date);
    }

    @SneakyThrows
    public static MalData map(ResultSet result) {
        return new MalData()
                .setId(result.getLong("id"))
                .setAktorId(result.getString("aktor_id"))
                .setMal(result.getString("mal"))
                .setEndretAv(result.getString("endret_av"))
                .setDato(result.getTimestamp("dato"));
    }

}
