package no.nav.fo.veilarbsituasjon.db;


import no.nav.fo.veilarbsituasjon.domain.OppfolgingBruker;
import no.nav.fo.veilarbsituasjon.utils.OppfolgingsbrukerUtil;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static no.nav.fo.veilarbsituasjon.utils.OppfolgingsbrukerUtil.mapRadTilOppfolgingsbruker;

public class BrukerRepository {

    private JdbcTemplate db;

    @Inject
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    String dateFormat = "'YYYY-MM-DD HH24:MI:SS.FF'";


    public BrukerRepository(JdbcTemplate db) {
        this.db = db;
    }

    public List<OppfolgingBruker> hentAlleVeiledertilordninger() {
        List<OppfolgingBruker> brukere = new ArrayList<>();
        db.setFetchSize(10000);
        db.query(hentAlleVeiledertilordningerSQL(), rs -> {
            brukere.add(mapRadTilOppfolgingsbruker(rs));
        });
        return brukere;
    }

    public List<OppfolgingBruker> hentTilordningerEtterTimestamp(Timestamp timestamp) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("timestamp", timestamp);

        return namedParameterJdbcTemplate
                .queryForList(hentVeilederTilordningerEtterTimestampSQL(), parameters)
                .stream()
                .map(OppfolgingsbrukerUtil::mapRadTilOppfolgingsbruker)
                .collect(toList());
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
        try {
            leggTilBruker(oppfolgingBruker);

        } catch(DuplicateKeyException e) {
            oppdaterBruker(oppfolgingBruker);
        }
    }

    void leggTilBruker(OppfolgingBruker oppfolgingBruker) {
        String aktoerid = oppfolgingBruker.getAktoerid();
        String veileder = oppfolgingBruker.getVeileder();
        String endretTimestamp = oppfolgingBruker.getEndretTimestamp().toString();
        db.update(leggTilBrukerSQL(), aktoerid, veileder, endretTimestamp);
    }

    void oppdaterBruker(OppfolgingBruker oppfolgingBruker) {
        String aktoerid = oppfolgingBruker.getAktoerid();
        String veileder = oppfolgingBruker.getVeileder();
        String endretTimestamp = oppfolgingBruker.getEndretTimestamp().toString();
        db.update(oppdaterBrukerSQL(), veileder, endretTimestamp, aktoerid);
    }

    String leggTilBrukerSQL() {
        return "INSERT INTO AKTOER_ID_TO_VEILEDER  "+
                "VALUES (?,?,TO_TIMESTAMP(?,"+dateFormat+"))";
    }

    String oppdaterBrukerSQL() {
        return "UPDATE AKTOER_ID_TO_VEILEDER " +
                "SET " +
                "VEILEDER = ? " +
                ",OPPDATERT = TO_TIMESTAMP(?,"+dateFormat+") " +
                "WHERE AKTOERID = ?";
    }

    String hentAlleVeiledertilordningerSQL() {
        return "SELECT AKTOERID, VEILEDER, OPPDATERT FROM AKTOER_ID_TO_VEILEDER";
    }

    String hentVeilederTilordningerEtterTimestampSQL() {
        return "SELECT AKTOERID, VEILEDER, OPPFOLGING, OPPDATERT " +
                "FROM AKTOER_ID_TO_VEILEDER tilordning " +
                "LEFT JOIN SITUASJON situasjon " +
                "ON tilordning.AKTOERID = situasjon.AKTORID" +
                "WHERE OPPDATERT > TO_TIMESTAMP((:timestamp), "+dateFormat+")";
    }
}
