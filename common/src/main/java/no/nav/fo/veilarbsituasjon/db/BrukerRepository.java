package no.nav.fo.veilarbsituasjon.db;


import lombok.val;
import no.nav.fo.veilarbsituasjon.rest.domain.OppfolgingBruker;
import no.nav.fo.veilarbsituasjon.utils.OppfolgingsbrukerUtil;
import org.springframework.jdbc.core.JdbcTemplate;

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

    public OppfolgingBruker hentTilordningForAktoer(String aktorId) {
        return db.queryForList(hentTilordningForAktoer(), aktorId)
                .stream()
                .findFirst()
                .map(OppfolgingsbrukerUtil::mapRadTilOppfolgingsbruker)
                .orElse(OppfolgingBruker
                        .builder()
                        .aktoerid(aktorId)
                        .build()
                );
    }

    public void upsertVeilederTilordning(OppfolgingBruker oppfolgingBruker) {
        String aktoerid = oppfolgingBruker.getAktoerid();
        String veileder = oppfolgingBruker.getVeileder();

        val rowsUpdated = db.update(
                "INSERT INTO SITUASJON(AKTORID, VEILEDER, OPPFOLGING, OPPDATERT) " +
                        "SELECT ?, ?, 1, CURRENT_TIMESTAMP FROM DUAL " +
                        "WHERE NOT EXISTS(SELECT * FROM SITUASJON WHERE AKTORID=?)",
                aktoerid, veileder, aktoerid);

        if (rowsUpdated == 0) {
            db.update("UPDATE SITUASJON SET VEILEDER = ?, OPPDATERT=CURRENT_TIMESTAMP WHERE AKTORID = ?",
                    veileder, aktoerid);
        }

    }

    private String hentTilordninger() {
        return "SELECT AKTORID, VEILEDER, OPPFOLGING, OPPDATERT " +
                "FROM SITUASJON";
    }

    private String hentTilordningForAktoer() {
        return hentTilordninger() +
                " WHERE AKTORID = ?";
    }

    private String hentVeilederTilordningerEtterTimestampSQL() {
        return hentTilordninger() +
                " WHERE OPPDATERT >= ?";
    }
}
