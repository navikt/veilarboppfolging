package no.nav.fo.veilarbsituasjon.db;


import no.nav.fo.veilarbsituasjon.domain.OppfolgingBruker;
import org.springframework.jdbc.core.JdbcTemplate;

public class BrukerRepository {

    private JdbcTemplate db;

    public BrukerRepository(JdbcTemplate db) {
        this.db = db;
    }

    private Boolean eksistererAktoerID(String aktoerid) {
        return !db.queryForList("select AKTOERID from AKTOER_ID_TO_VEILEDER where AKTOERID="+ aktoerid).isEmpty();
    }

    public void leggTilEllerOppdaterBruker(OppfolgingBruker oppfolgingBruker) {
        String aktoerid = oppfolgingBruker.getAktoerid();
        String veileder = oppfolgingBruker.getVeileder();
        String sql;


        if(eksistererAktoerID(aktoerid)){
            sql = "UPDATE AKTOER_ID_TO_VEILEDER " +
                    "SET " +
                    "VEILEDER = ? " +
                    ",OPPDATERT = CURRENT_TIMESTAMP " +
                    "WHERE AKTOERID = ?";

            db.update(sql, veileder, aktoerid);
        } else {
            sql = "INSERT INTO AKTOER_ID_TO_VEILEDER  "+
                    "VALUES (?,?,CURRENT_TIMESTAMP)";
            db.update(sql, aktoerid, veileder);
        }
    }
}
