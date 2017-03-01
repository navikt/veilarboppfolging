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

    public String hentVeilederForAktoer(String aktoerId) {
        return db.queryForList("SELECT VEILEDER FROM  AKTOER_ID_TO_VEILEDER WHERE AKTOERID = ?", aktoerId)
                .stream()
                .findFirst()
                .map(x -> x.get("VEILEDER"))
                .map(Object::toString)
                .orElse(null);
    }

    public void leggTilEllerOppdaterBruker(OppfolgingBruker oppfolgingBruker) {
        String aktoerid = oppfolgingBruker.getAktoerid();
        String veileder = oppfolgingBruker.getVeileder();
        String endretTimestamp = oppfolgingBruker.getEndretTimestamp().toString();
        String dateFormat = "'YYYY-MM-DD HH24:MI:SS.FF'";
        String sql;

        if(eksistererAktoerID(aktoerid)){
            sql = "UPDATE AKTOER_ID_TO_VEILEDER " +
                    "SET " +
                    "VEILEDER = ? " +
                    ",OPPDATERT = TO_TIMESTAMP(?,"+dateFormat+") " +
                    "WHERE AKTOERID = ?";

            db.update(sql,veileder,endretTimestamp,aktoerid);
        } else {
            sql = "INSERT INTO AKTOER_ID_TO_VEILEDER  "+
                    "VALUES (?,?,TO_TIMESTAMP(?,"+dateFormat+"))";
            db.update(sql, aktoerid, veileder, endretTimestamp);
        }
    }
}
