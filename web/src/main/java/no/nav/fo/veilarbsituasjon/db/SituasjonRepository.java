package no.nav.fo.veilarbsituasjon.db;


import no.nav.fo.veilarbsituasjon.domain.Brukervilkar;
import no.nav.fo.veilarbsituasjon.domain.Situasjon;
import no.nav.fo.veilarbsituasjon.domain.VilkarStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class SituasjonRepository {

    private JdbcTemplate jdbcTemplate;

    public SituasjonRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Situasjon> hentSituasjon(String aktorId) {
        List<Situasjon> situasjon = jdbcTemplate.query("SELECT * FROM situasjon WHERE aktorid = ?",
                (result, n) -> tilSituasjon(result),
                aktorId
        );
        return situasjon.stream().findFirst().map(this::leggTilVilkar);
    }

    public void oppdaterSituasjon(Situasjon oppdatertSituasjon) {
        String aktorId = oppdatertSituasjon.getAktorId();
        boolean oppfolging = oppdatertSituasjon.isOppfolging();
        boolean manuell = oppdatertSituasjon.isManuell();
        List<Brukervilkar> brukervilkar = oppdatertSituasjon.getBrukervilkar();
        if (situasjonHarAktorId(aktorId)) {
            jdbcTemplate.update("UPDATE situasjon SET oppfolging = ?, manuell = ? WHERE aktorid = ?",
                    oppfolging,
                    manuell,
                    aktorId
            );
            jdbcTemplate.update("DELETE FROM brukervilkar WHERE aktorid = ?", aktorId);
            opprettBrukervilkarForAktor(aktorId, brukervilkar);
        } else {
            jdbcTemplate.update("INSERT INTO situasjon(aktorid, oppfolging, manuell) VALUES(?, ?, ?)",
                    aktorId,
                    oppfolging,
                    manuell
            );
            opprettBrukervilkarForAktor(aktorId, brukervilkar);
        }
    }

    private Situasjon leggTilVilkar(Situasjon situasjon) {
        List<Brukervilkar> brukervilkar = jdbcTemplate.query(
                "SELECT dato, vilkarstatus, tekst FROM brukervilkar WHERE aktorid = ?",
                (result, antallRader) -> tilBrukervilkar(result),
                situasjon.getAktorId()
        );
        return situasjon.setBrukervilkar(brukervilkar);
    }

    private void opprettBrukervilkarForAktor(String aktorId, List<Brukervilkar> brukervilkar) {
        for (Brukervilkar vilkar : brukervilkar) {
            jdbcTemplate.update(
                    "INSERT INTO brukervilkar(aktorid, dato, vilkarstatus, tekst) VALUES(?, ?, ?, ?)",
                    aktorId,
                    vilkar.getDato(),
                    vilkar.getVilkarstatus().name(),
                    vilkar.getTekst()
            );
        }
    }

    private Boolean situasjonHarAktorId(String aktoerId) {
        return !jdbcTemplate.queryForList(
                "SELECT aktorid FROM situasjon WHERE aktorid=?",
                aktoerId
        ).isEmpty();
    }

    private Situasjon tilSituasjon(ResultSet result) throws SQLException {
        return new Situasjon()
                .setAktorId(result.getString("aktorid"))
                .setOppfolging(result.getBoolean("oppfolging"))
                ;
    }

    private Brukervilkar tilBrukervilkar(ResultSet result) throws SQLException {
        return new Brukervilkar()
                .setDato(result.getTimestamp("dato"))
                .setVilkarstatus(VilkarStatus.valueOf(result.getString("vilkarstatus")))
                .setTekst(result.getString("tekst"));
    }

}
