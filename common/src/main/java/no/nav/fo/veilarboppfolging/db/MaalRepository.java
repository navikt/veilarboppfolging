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
        return database.query("" +
                        "SELECT" +
                        "  id AS mal_id, " +
                        "  aktor_id AS mal_aktor_id, " +
                        "  mal AS mal_mal, " +
                        "  endret_av AS mal_endret_av, " +
                        "  dato AS mal_dato " +
                        "FROM MAL " +
                        "WHERE aktor_id = ? " +
                        "ORDER BY ID DESC",
                MaalRepository::map,
                aktorId);
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
                .setId(result.getLong("mal_id"))
                .setAktorId(result.getString("mal_aktor_id"))
                .setMal(result.getString("mal_mal"))
                .setEndretAv(result.getString("mal_endret_av"))
                .setDato(result.getTimestamp("mal_dato"));
    }

}
