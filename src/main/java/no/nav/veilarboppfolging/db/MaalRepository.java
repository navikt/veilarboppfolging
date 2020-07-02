package no.nav.veilarboppfolging.db;

import lombok.SneakyThrows;
import no.nav.veilarboppfolging.domain.MalData;
import no.nav.veilarboppfolging.utils.DbUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.util.List;

import static no.nav.veilarboppfolging.db.OppfolgingsStatusRepository.AKTOR_ID;
import static no.nav.veilarboppfolging.db.OppfolgingsStatusRepository.GJELDENDE_MAL;

@Repository
public class MaalRepository {

    private final JdbcTemplate db;

    @Autowired
    public MaalRepository(JdbcTemplate db) {
        this.db = db;
    }

    public List<MalData> aktorMal(String aktorId) {
        return db.query("SELECT * FROM MAL WHERE aktor_id = ? ORDER BY ID DESC",
                MaalRepository::map,
                aktorId);
    }

    public MalData fetch(Long id) {
        String sql = "SELECT * FROM MAL WHERE id = ?";
        return db.query(sql, MaalRepository::map, id).get(0);
    }

    @Transactional
    public void opprett(MalData maal) {
        maal.setId(DbUtils.nesteFraSekvens(db, "MAL_SEQ"));
        insert(maal);
        setActive(maal);
    }

    private void insert(MalData maal) {
        String sql = "INSERT INTO MAL(id, aktor_id, mal, endret_av, dato) VALUES(?, ?, ?, ?, ?)";
        db.update(sql, maal.getId(), maal.getAktorId(), maal.getMal(), maal.getEndretAv(), maal.getDato());
    }

    private void setActive(MalData mal) {
        db.update("UPDATE OPPFOLGINGSTATUS " +
                        " SET " + GJELDENDE_MAL + " = ?," +
                        " oppdatert = CURRENT_TIMESTAMP, " +
                        " FEED_ID = null " +
                        "WHERE " + AKTOR_ID + " = ?",
                mal.getId(),
                mal.getAktorId()
        );
    }

    @SneakyThrows
    private static MalData map(ResultSet result, int row) {
        return new MalData()
                .setId(result.getLong("id"))
                .setAktorId(result.getString("aktor_id"))
                .setMal(result.getString("mal"))
                .setEndretAv(result.getString("endret_av"))
                .setDato(result.getTimestamp("dato"));
    }

}
