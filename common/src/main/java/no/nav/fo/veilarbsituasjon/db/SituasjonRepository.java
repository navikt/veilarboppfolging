package no.nav.fo.veilarbsituasjon.db;


import lombok.SneakyThrows;
import no.nav.fo.veilarbsituasjon.domain.Brukervilkar;
import no.nav.fo.veilarbsituasjon.domain.Situasjon;
import no.nav.fo.veilarbsituasjon.domain.Status;
import no.nav.fo.veilarbsituasjon.domain.VilkarStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class SituasjonRepository {
    private static final String DIALECT_PROPERTY = "db.dialect";
    private static final String HSQLDB_DIALECT = "hsqldb";

    private JdbcTemplate jdbcTemplate;

    public SituasjonRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Situasjon> hentSituasjon(String aktorId) {
        List<Situasjon> situasjon = jdbcTemplate.query("" +
                        "SELECT * FROM situasjon " +
                        "LEFT JOIN status ON situasjon.aktorid = status.aktorid " +
                        "LEFT JOIN brukervilkar ON situasjon.aktorid = brukervilkar.aktorid " +
                        "WHERE situasjon.aktorid = ?",
                (result, n) -> mapTilSituasjon(result),
                aktorId
        );

        return situasjon.isEmpty() ? Optional.empty() : situasjon.stream().findAny();
    }


    public void oppdaterSituasjon(Situasjon oppdatertSituasjon) {
        String aktorId = oppdatertSituasjon.getAktorId();
        boolean oppfolging = oppdatertSituasjon.isOppfolging();
        jdbcTemplate.update("UPDATE situasjon SET oppfolging = ? WHERE aktorid = ?",
                oppfolging,
                aktorId
        );
    }

    public void opprettStatus(Status status) {
        status.setId(nesteFraSekvens("status_seq"));
        oppdaterSituasjonStatus(status);
        opprettSituasjonStatus(status);
    }

    public void opprettBrukervilkar(Brukervilkar brukervilkar) {
        brukervilkar.setId(nesteFraSekvens("brukervilkar_seq"));
        oppdaterSituasjonBrukervilkar(brukervilkar);
        opprettSituasjonBrukervilkar(brukervilkar);
    }

    @Transactional // TODO trengs egentlig denne?
    public Situasjon opprettSituasjon(Situasjon situasjon) {
        jdbcTemplate.update(
                "INSERT INTO situasjon(aktorid, oppfolging, gjeldende_status, gjeldende_brukervilkar) VALUES(?, ?, ?, ?)",
                situasjon.getAktorId(),
                situasjon.isOppfolging(),
                null,
                null
        );
        Optional.ofNullable(situasjon.getGjeldendeBrukervilkar()).ifPresent(this::opprettBrukervilkar);
        Optional.ofNullable(situasjon.getGjeldendeStatus()).ifPresent(this::opprettStatus);
        return situasjon;
    }

    public boolean situasjonFinnes(Situasjon situasjon) {
        return !jdbcTemplate.queryForList(
                "SELECT aktorid FROM situasjon WHERE aktorid=?",
                situasjon.getAktorId()
        ).isEmpty();
    }

    private void oppdaterSituasjonBrukervilkar(Brukervilkar gjeldendeBrukervilkar) {
        jdbcTemplate.update("UPDATE situasjon SET gjeldende_brukervilkar = ? WHERE aktorid = ?",
                gjeldendeBrukervilkar.getId(),
                gjeldendeBrukervilkar.getAktorId()
        );
    }

    private void oppdaterSituasjonStatus(Status gjeldendeStatus) {
        jdbcTemplate.update("UPDATE situasjon SET gjeldende_status = ? WHERE aktorid = ?",
                gjeldendeStatus.getId(),
                gjeldendeStatus.getAktorId()
        );
    }

    private void opprettSituasjonBrukervilkar(Brukervilkar vilkar) {
        jdbcTemplate.update(
                "INSERT INTO brukervilkar(id, aktorid, dato, vilkarstatus, tekst, hash) VALUES(?, ?, ?, ?, ?, ?)",
                vilkar.getId(),
                vilkar.getAktorId(),
                vilkar.getDato(),
                vilkar.getVilkarstatus().name(),
                vilkar.getTekst(),
                vilkar.getHash()

        );
    }


    private void opprettSituasjonStatus(Status status) {
        jdbcTemplate.update(
                "INSERT INTO status(id, aktorid, manuell, dato, begrunnelse) VALUES(?, ?, ?, ?, ?)",
                status.getId(),
                status.getAktorId(),
                status.isManuell(),
                status.getDato(),
                status.getBegrunnelse()
        );
    }

    private long nesteFraSekvens(String sekvensNavn) {
        String sekvensQuery;
        if (HSQLDB_DIALECT.equals(System.getProperty(DIALECT_PROPERTY))) {
            sekvensQuery = "call next value for " + sekvensNavn;
        } else {
            sekvensQuery = "select " + sekvensNavn + ".nextval from dual";
        }
        return jdbcTemplate.queryForObject(sekvensQuery, Long.class);
    }

    private Situasjon mapTilSituasjon(ResultSet resultat) throws SQLException {
        return new Situasjon()
                .setAktorId(resultat.getString("aktorid"))
                .setOppfolging(resultat.getBoolean("oppfolging"))
                .setGjeldendeStatus(
                        Optional.ofNullable(resultat.getLong("gjeldende_status"))
                                .map(s -> s != 0 ? mapTilStatus(resultat) : null)
                                .orElse(null)
                )
                .setGjeldendeBrukervilkar(
                        Optional.ofNullable(resultat.getLong("gjeldende_brukervilkar"))
                                .map(b -> b != 0 ? mapTilBrukervilkar(resultat) : null)
                                .orElse(null)
                )
                ;
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private Brukervilkar mapTilBrukervilkar(ResultSet result) {
        return new Brukervilkar(
                result.getString("aktorid"),
                result.getTimestamp("brukervilkar.dato"),
                VilkarStatus.valueOf(result.getString("vilkarstatus")),
                result.getString("tekst"),
                result.getString("hash")
        ).setId(result.getLong("id"));
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private Status mapTilStatus(ResultSet result) {
        return new Status(
                result.getString("aktorid"),
                result.getBoolean("manuell"),
                result.getTimestamp("dato"),
                result.getString("begrunnelse")
        ).setId(result.getLong("id"));
    }

}
