package no.nav.fo.veilarbsituasjon.db;


import no.nav.fo.veilarbsituasjon.domain.OppfolgingBruker;
import no.nav.fo.veilarbsituasjon.utils.OppfolgingsbrukerUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;

import java.sql.Timestamp;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class BrukerRepository {

    private JdbcTemplate db;

    public BrukerRepository(JdbcTemplate db) {
        this.db = db;
    }

    public List<OppfolgingBruker> hentTilordningerEtterTimestamp(Timestamp timestamp) {
        return db
                .queryForList(hentVeilederTilordningerEtterTimestampSQL(), timestamp)
                .stream()
                .map(OppfolgingsbrukerUtil::mapRadTilOppfolgingsbruker)
                .collect(toList());
    }

    public String hentVeilederForAktoer(String aktorId) {
        return db.queryForList("SELECT VEILEDER FROM SITUASJON WHERE AKTORID = ?", aktorId)
                .stream()
                .findFirst()
                .map(x -> x.get("VEILEDER"))
                .map(Object::toString)
                .orElse(null);
    }

    public void upsertVeilederTilordning(OppfolgingBruker oppfolgingBruker) {
        String aktoerid = oppfolgingBruker.getAktoerid();
        String veileder = oppfolgingBruker.getVeileder();
        db.execute(upsertTilordningSQL(), (PreparedStatementCallback<Boolean>) ps -> {
            ps.setString(1, aktoerid);
            ps.setString(2, veileder);
            ps.setString(3, aktoerid);
            ps.setString(4, veileder);
            return ps.execute();
        });
    }

    private String upsertTilordningSQL() {
        return "MERGE INTO SITUASJON USING DUAL ON (AKTORID = ?) WHEN MATCHED THEN UPDATE SET VEILEDER = ?, OPPDATERT = CURRENT_TIMESTAMP WHEN NOT MATCHED " +
                "THEN INSERT (AKTORID, VEILEDER, OPPDATERT, OPPFOLGING) " +
                "VALUES (?, ?, CURRENT_TIMESTAMP, 1)";
    }

    private String hentVeilederTilordningerEtterTimestampSQL() {
        return "SELECT AKTORID, VEILEDER, OPPFOLGING, OPPDATERT " +
                "FROM SITUASJON " +
                "WHERE OPPDATERT >= ?";
    }
}
