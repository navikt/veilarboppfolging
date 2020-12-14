package no.nav.veilarboppfolging.repository;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarboppfolging.utils.DbUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Repository
public class BrukerOppslagFlereOppfolgingAktorRepository {
    private final JdbcTemplate db;

    @Autowired
    public BrukerOppslagFlereOppfolgingAktorRepository(JdbcTemplate db){
        this.db = db;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void insertBrukerHvisNy(String norskIdent){
        if (getBrukerLogget(norskIdent)) {
            return;
        }
        insertBrukerOppslag(norskIdent);
    }

    private void insertBrukerOppslag(String norskIdent) {
        long id = DbUtils.nesteFraSekvens(db, "BRUKER_OPPSLAG_MED_FLERE_AKTORID_SEQ");
        db.update("INSERT INTO BRUKER_OPPSLAG_MED_FLERE_AKTORID(" +
                        "BRUKER_SEQ, " +
                        "OPPSLAG_BRUKER_ID, " +
                        "CREATED) " +
                        "VALUES(?, ?, CURRENT_TIMESTAMP)",
                id,
                norskIdent);
    }

    private boolean getBrukerLogget(String norskIdent) {
        int hits = db.queryForObject("SELECT count(*) " +
                "FROM BRUKER_OPPSLAG_MED_FLERE_AKTORID " +
                "WHERE OPPSLAG_BRUKER_ID=?",
                new Object[] {norskIdent},
                Integer.class);

        return hits != 0;
    }
}
