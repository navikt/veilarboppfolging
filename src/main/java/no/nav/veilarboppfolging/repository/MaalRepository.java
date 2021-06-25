package no.nav.veilarboppfolging.repository;

import lombok.SneakyThrows;
import no.nav.common.types.identer.AktorId;
import no.nav.veilarboppfolging.repository.entity.MaalEntity;
import no.nav.veilarboppfolging.utils.DbUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;

import static no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository.AKTOR_ID;
import static no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository.GJELDENDE_MAL;
import static no.nav.veilarboppfolging.utils.DbUtils.hentZonedDateTime;
import static no.nav.veilarboppfolging.utils.DbUtils.queryForNullableObject;

@Repository
public class MaalRepository {

    private final JdbcTemplate db;

    private final TransactionTemplate transactor;

    @Autowired
    public MaalRepository(JdbcTemplate db, TransactionTemplate transactor) {
        this.db = db;
        this.transactor = transactor;
    }

    public List<MaalEntity> aktorMal(AktorId aktorId) {
        return db.query("SELECT * FROM MAL WHERE aktor_id = ? ORDER BY ID DESC",
                MaalRepository::mapMaalEntity,
                aktorId.get());
    }

    public Optional<MaalEntity> hentMaal(long id) {
        String sql = "SELECT * FROM MAL WHERE id = ?";
        return queryForNullableObject(() -> db.queryForObject(sql, MaalRepository::mapMaalEntity, id));
    }

    public void opprett(MaalEntity maal) {
        transactor.executeWithoutResult((ignored) -> {
            maal.setId(DbUtils.nesteFraSekvens(db, "MAL_SEQ"));
            insert(maal);
            setActive(maal);
        });
    }

    private void insert(MaalEntity maal) {
        String sql = "INSERT INTO MAL(id, aktor_id, mal, endret_av, dato) VALUES(?, ?, ?, ?, ?)";
        db.update(sql, maal.getId(), maal.getAktorId(), maal.getMal(), maal.getEndretAv(), maal.getDato());
    }

    private void setActive(MaalEntity maal) {
        db.update("UPDATE OPPFOLGINGSTATUS " +
                        " SET " + GJELDENDE_MAL + " = ?," +
                        " oppdatert = CURRENT_TIMESTAMP, " +
                        " FEED_ID = null " +
                        "WHERE " + AKTOR_ID + " = ?",
                maal.getId(),
                maal.getAktorId()
        );
    }

    @SneakyThrows
    private static MaalEntity mapMaalEntity(ResultSet result, int row) {
        return new MaalEntity()
                .setId(result.getLong("id"))
                .setAktorId(result.getString("aktor_id"))
                .setMal(result.getString("mal"))
                .setEndretAv(result.getString("endret_av"))
                .setDato(hentZonedDateTime(result, "dato"));
    }

}
