package no.nav.fo.veilarboppfolging.db;

import lombok.SneakyThrows;
import no.nav.fo.veilarboppfolging.domain.MalData;
import no.nav.sbl.jdbc.Database;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.util.Date;
import java.util.List;

import static no.nav.fo.veilarboppfolging.db.OppfolgingsStatusRepository.AKTOR_ID;
import static no.nav.fo.veilarboppfolging.db.OppfolgingsStatusRepository.GJELDENDE_MAL;

public class MaalRepository {
    private Database database;

    public MaalRepository(Database database) {
        this.database = database;
    }

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

    @Transactional
    public void opprett(MalData maal) {
        maal.setId(database.nesteFraSekvens("MAL_SEQ"));
        insert(maal);
        setActive(maal);
    }

    @Transactional
    public void slettForAktorEtter(String aktorId, Date date) {
        removeActive(aktorId);
        deleteMaal(aktorId, date);
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

    private void insert(MalData maal) {
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

    private void setActive(MalData mal) {
        database.update("UPDATE OPPFOLGINGSTATUS SET " + GJELDENDE_MAL + " = ?," +
                        " oppdatert = CURRENT_TIMESTAMP " +
                        "WHERE " + AKTOR_ID + " = ?",
                mal.getId(),
                mal.getAktorId()
        );
    }

    private void deleteMaal(String aktorId, Date date) {
        database.update("DELETE FROM MAL " +
                        "WHERE aktor_id = ? AND dato > ?",
                aktorId,
                date);
    }

    private void removeActive(String aktorId) {
        database.update("UPDATE " +
                        OppfolgingsStatusRepository.TABLE_NAME +
                        " SET " + GJELDENDE_MAL + " = NULL" +
                        " WHERE " + AKTOR_ID + " = ?",
                aktorId);
    }

}
