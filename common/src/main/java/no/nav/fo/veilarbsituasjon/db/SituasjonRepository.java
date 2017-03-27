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
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;

public class SituasjonRepository {
    private static final String DIALECT_PROPERTY = "db.dialect";
    private static final String HSQLDB_DIALECT = "hsqldb";

    private JdbcTemplate jdbcTemplate;

    public SituasjonRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Situasjon> hentSituasjon(String aktorId) {
        List<Situasjon> situasjon = jdbcTemplate.query("" +
                        "SELECT" +
                        "  SITUASJON.AKTORID AS AKTORID, " +
                        "  SITUASJON.OPPFOLGING AS OPPFOLGING, " +
                        "  SITUASJON.GJELDENDE_STATUS AS GJELDENDE_STATUS, " +
                        "  SITUASJON.GJELDENDE_BRUKERVILKAR AS GJELDENDE_BRUKERVILKAR, " +
                        "  SITUASJON.OPPFOLGING_UTGANG AS OPPFOLGING_UTGANG, " +
                        "  STATUS.ID AS STATUS_ID, " +
                        "  STATUS.AKTORID AS STATUS_AKTORID, " +
                        "  STATUS.MANUELL AS STATUS_MANUELL, " +
                        "  STATUS.DATO AS STATUS_DATO, " +
                        "  STATUS.BEGRUNNELSE AS STATUS_BEGRUNNELSE, " +
                        "  BRUKERVILKAR.ID AS BRUKERVILKAR_ID, " +
                        "  BRUKERVILKAR.AKTORID AS BRUKERVILKAR_AKTORID, " +
                        "  BRUKERVILKAR.DATO AS BRUKERVILKAR_DATO, " +
                        "  BRUKERVILKAR.VILKARSTATUS AS BRUKERVILKAR_VILKARSTATUS, " +
                        "  BRUKERVILKAR.TEKST AS BRUKERVILKAR_TEKST, " +
                        "  BRUKERVILKAR.HASH AS BRUKERVILKAR_HASH " +
                        "FROM situasjon " +
                        "LEFT JOIN status ON SITUASJON.GJELDENDE_STATUS = STATUS.ID " +
                        "LEFT JOIN brukervilkar ON SITUASJON.GJELDENDE_BRUKERVILKAR = BRUKERVILKAR.ID " +
                        "WHERE situasjon.aktorid = ? ",
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

    @Transactional
    public Situasjon opprettSituasjon(Situasjon situasjon) {
        jdbcTemplate.update(
                "INSERT INTO situasjon(aktorid, oppfolging, gjeldende_status, gjeldende_brukervilkar, oppfolging_utgang) " +
                        "VALUES(?, ?, ?, ?, ?)",
                situasjon.getAktorId(),
                situasjon.isOppfolging(),
                null,
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
                .setOppfolgingUtgang(hentDato(resultat, "oppfolging_utgang"))
                ;
    }

    private static Date hentDato(ResultSet rs, String kolonneNavn) throws SQLException {
        return ofNullable(rs.getTimestamp(kolonneNavn))
                .map(Timestamp::getTime)
                .map(Date::new)
                .orElse(null);
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private Brukervilkar mapTilBrukervilkar(ResultSet result) {
        return new Brukervilkar(
                result.getString("AKTORID"),
                result.getTimestamp("BRUKERVILKAR_DATO"),
                VilkarStatus.valueOf(result.getString("BRUKERVILKAR_VILKARSTATUS")),
                result.getString("BRUKERVILKAR_TEKST"),
                result.getString("BRUKERVILKAR_HASH")
        ).setId(result.getLong("BRUKERVILKAR_ID"));
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private Status mapTilStatus(ResultSet result) {
        return new Status(
                result.getString("AKTORID"),
                result.getBoolean("STATUS_MANUELL"),
                result.getTimestamp("STATUS_DATO"),
                result.getString("STATUS_BEGRUNNELSE")
        ).setId(result.getLong("STATUS_ID"));
    }

}
