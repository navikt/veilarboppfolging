package no.nav.veilarboppfolging.repository;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.utils.DbUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class BrukerOppslagFlereOppfolgingAktorRepository {
    private final JdbcTemplate db;

    @Autowired
    public BrukerOppslagFlereOppfolgingAktorRepository(JdbcTemplate db){
        this.db = db;
    }

    public void insertBrukerHvisNy(Fnr norskIdent){
        try {
            insertBrukerOppslag(norskIdent);
        } catch (DuplicateKeyException e) {
            //Flere som kjører paralelt
            //Eller allerede registrert
        }
    }

    private void insertBrukerOppslag(Fnr norskIdent) {
        long id = DbUtils.nesteFraSekvens(db, "BRUKER_MED_FLERE_AKTORID_SEQ");
        db.update("INSERT INTO BRUKER_MED_FLERE_AKTORID(" +
                        "BRUKER_SEQ, " +
                        "OPPSLAG_BRUKER_ID, " +
                        "CREATED) " +
                        "VALUES(?, ?, CURRENT_TIMESTAMP)",
                id,
                norskIdent.get());
    }
}
