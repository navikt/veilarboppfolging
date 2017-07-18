package no.nav.fo.veilarbsituasjon.db;


import lombok.SneakyThrows;
import no.nav.fo.veilarbsituasjon.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static no.nav.apiapp.util.EnumUtils.getName;
import static no.nav.apiapp.util.EnumUtils.valueOfOptional;

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
                        "  SITUASJON.GJELDENDE_MAL AS GJELDENDE_MAL, " +
                        "  STATUS.ID AS STATUS_ID, " +
                        "  STATUS.AKTORID AS STATUS_AKTORID, " +
                        "  STATUS.MANUELL AS STATUS_MANUELL, " +
                        "  STATUS.DATO AS STATUS_DATO, " +
                        "  STATUS.BEGRUNNELSE AS STATUS_BEGRUNNELSE, " +
                        "  STATUS.OPPRETTET_AV AS STATUS_OPPRETTET_AV, " +
                        "  STATUS.OPPRETTET_AV_BRUKERID AS STATUS_OPPRETTET_AV_BRUKERID, " +
                        "  BRUKERVILKAR.ID AS BRUKERVILKAR_ID, " +
                        "  BRUKERVILKAR.AKTORID AS BRUKERVILKAR_AKTORID, " +
                        "  BRUKERVILKAR.DATO AS BRUKERVILKAR_DATO, " +
                        "  BRUKERVILKAR.VILKARSTATUS AS BRUKERVILKAR_VILKARSTATUS, " +
                        "  BRUKERVILKAR.TEKST AS BRUKERVILKAR_TEKST, " +
                        "  BRUKERVILKAR.HASH AS BRUKERVILKAR_HASH, " +
                        "  MAL.ID AS MAL_ID, " +
                        "  MAL.AKTORID AS MAL_AKTORID, " +
                        "  MAL.MAL AS MAL_MAL, " +
                        "  MAL.ENDRET_AV AS MAL_ENDRET_AV, " +
                        "  MAL.DATO AS MAL_DATO " +
                        "FROM situasjon " +
                        "LEFT JOIN status ON SITUASJON.GJELDENDE_STATUS = STATUS.ID " +
                        "LEFT JOIN brukervilkar ON SITUASJON.GJELDENDE_BRUKERVILKAR = BRUKERVILKAR.ID " +
                        "LEFT JOIN MAL ON SITUASJON.GJELDENDE_MAL = MAL.ID " +
                        "WHERE situasjon.aktorid = ? ",
                (result, n) -> mapTilSituasjon(result),
                aktorId
        );

        return situasjon.isEmpty() ? Optional.empty() : situasjon.stream().findAny();
    }

    public void oppdaterOppfolgingStatus(Situasjon oppdatertSituasjon) {
        String aktorId = oppdatertSituasjon.getAktorId();
        boolean oppfolging = oppdatertSituasjon.isOppfolging();
        oppdaterOppfolgingStatus(aktorId, oppfolging);
    }

    public void oppdaterOppfolgingStatus(String aktorId, boolean oppfolging) {
        jdbcTemplate.update("UPDATE situasjon SET oppfolging = ?, OPPDATERT = CURRENT_TIMESTAMP WHERE aktorid = ?",
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
        opprettSituasjonBrukervilkar(brukervilkar);
        oppdaterSituasjonBrukervilkar(brukervilkar);
    }

    public void opprettMal(MalData mal) {
        mal.setId(nesteFraSekvens("MAL_SEQ"));
        oppdaterSituasjonMal(mal);
        opprettSituasjonMal(mal);
    }

    @Transactional
    public Situasjon opprettSituasjon(Situasjon situasjon) {
        jdbcTemplate.update(
                "INSERT INTO situasjon(aktorid, oppfolging, gjeldende_status, gjeldende_brukervilkar, oppfolging_utgang, gjeldende_mal) " +
                        "VALUES(?, ?, ?, ?, ?, ?)",
                situasjon.getAktorId(),
                situasjon.isOppfolging(),
                null,
                null,
                null,
                null
        );
        Optional.ofNullable(situasjon.getGjeldendeBrukervilkar()).ifPresent(this::opprettBrukervilkar);
        Optional.ofNullable(situasjon.getGjeldendeStatus()).ifPresent(this::opprettStatus);
        Optional.ofNullable(situasjon.getGjeldendeMal()).ifPresent(this::opprettMal);
        return situasjon;
    }

    public List<MalData> hentMalList(String aktorId) {
        return jdbcTemplate.query("" +
                        "SELECT" +
                        "  ID AS MAL_ID, " +
                        "  AKTORID AS MAL_AKTORID, " +
                        "  MAL AS MAL_MAL, " +
                        "  ENDRET_AV AS MAL_ENDRET_AV, " +
                        "  DATO AS MAL_DATO " +
                        "FROM MAL " +
                        "WHERE AKTORID = ? " +
                        "ORDER BY ID DESC",
                (result, n) -> mapTilMal(result),
                aktorId);
    }

    public boolean situasjonFinnes(Situasjon situasjon) {
        return !jdbcTemplate.queryForList(
                "SELECT aktorid FROM situasjon WHERE aktorid=?",
                situasjon.getAktorId()
        ).isEmpty();
    }

    public List<Brukervilkar> hentHistoriskeVilkar(String aktorId) {
        String sql =
                "SELECT " +
                "ID AS BRUKERVILKAR_ID, " +
                "AKTORID AS AKTORID, " +
                "DATO AS BRUKERVILKAR_DATO, " +
                "VILKARSTATUS AS BRUKERVILKAR_VILKARSTATUS, " +
                "TEKST AS BRUKERVILKAR_TEKST, " +
                "HASH AS BRUKERVILKAR_HASH " +
                "FROM BRUKERVILKAR " +
                "WHERE AKTORID = ? " +
                "ORDER BY DATO DESC";
        return jdbcTemplate.query(sql, (result, n) -> mapTilBrukervilkar(result), aktorId);
    }

    public void opprettOppfolgingsperiode(Oppfolgingsperiode oppfolgingperiode) {
        jdbcTemplate.update("" +
                "INSERT INTO OPPFOLGINGSPERIODE(aktorId, veileder, sluttDato, begrunnelse, oppdatert) " +
                "VALUES (?,?,?,?,CURRENT_TIMESTAMP)",
                oppfolgingperiode.getAktorId(),
                oppfolgingperiode.getVeileder(),
                oppfolgingperiode.getSluttDato(),
                oppfolgingperiode.getBegrunnelse());
    }

    public List<AvsluttetOppfolgingFeedData> hentAvsluttetOppfolgingEtterDato(Timestamp timestamp) {
        return jdbcTemplate
                .query("SELECT aktorid, sluttdato, oppdatert " +
                            "FROM OPPFOLGINGSPERIODE " +
                            "WHERE oppdatert >= ?",
                        (result, n) -> mapRadTilAvsluttetOppfolging(result),
                        timestamp);
    }

    @SneakyThrows
    private AvsluttetOppfolgingFeedData mapRadTilAvsluttetOppfolging(ResultSet rs) {
        return AvsluttetOppfolgingFeedData.builder()
                .aktoerid(rs.getString("aktorid"))
                .sluttdato(rs.getTimestamp("sluttdato"))
                .oppdatert(rs.getTimestamp("oppdatert"))
                .build();
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

    private void oppdaterSituasjonMal(MalData mal) {
        jdbcTemplate.update("UPDATE SITUASJON SET GJELDENDE_MAL = ? WHERE AKTORID = ?",
                mal.getId(),
                mal.getAktorId()
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
                "INSERT INTO STATUS(id, aktorid, manuell, dato, begrunnelse, opprettet_av, opprettet_av_brukerid) " +
                     "VALUES(?, ?, ?, ?, ?, ?, ?)",
                status.getId(),
                status.getAktorId(),
                status.isManuell(),
                status.getDato(),
                status.getBegrunnelse(),
                getName(status.getOpprettetAv()),
                status.getOpprettetAvBrukerId()
        );
    }

    public List<InnstillingsHistorikkData> hentManuellHistorikk(String aktorId) {
        return jdbcTemplate.query(
                        "SELECT manuell, dato, begrunnelse, opprettet_av, opprettet_av_brukerid " +
                        "FROM STATUS " +
                        "WHERE aktorid = ?",
                (result, n) -> mapRadTilInnstillingsHistorikkData(result),
                aktorId);
    }

    private void opprettSituasjonMal(MalData mal) {
        jdbcTemplate.update(
                "INSERT INTO MAL VALUES(?, ?, ?, ?, ?)",
                mal.getId(),
                mal.getAktorId(),
                mal.getMal(),
                mal.getEndretAv(),
                mal.getDato()
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
        String aktorId = resultat.getString("aktorid");
        return new Situasjon()
                .setAktorId(aktorId)
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
                .setGjeldendeMal(
                        Optional.ofNullable(resultat.getLong("GJELDENDE_MAL"))
                                .map(m -> m != 0 ? mapTilMal(resultat) : null)
                                .orElse(null)
                )
                .setOppfolgingsperioder(hentOppfolgingsperioder(aktorId));
    }

    public List<Oppfolgingsperiode> hentOppfolgingsperioder(String aktorid) {
        return jdbcTemplate.query("" +
                        "SELECT " +
                        " AKTORID, " +
                        " VEILEDER, " +
                        " SLUTTDATO, " +
                        " BEGRUNNELSE " +
                        "FROM OPPFOLGINGSPERIODE " +
                        "WHERE AKTORID = ?",
                (result, n) -> mapTilOppfolgingsperiode(result),
                aktorid
        );
    }

    private Oppfolgingsperiode mapTilOppfolgingsperiode(ResultSet result) throws SQLException {
        return Oppfolgingsperiode.builder()
                .aktorId(result.getString("aktorid"))
                .veileder(result.getString("veileder"))
                .sluttDato(hentDato(result, "sluttdato"))
                .begrunnelse(result.getString("begrunnelse"))
                .build();
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
                result.getString("STATUS_BEGRUNNELSE"),
                valueOfOptional(KodeverkBruker.class, result.getString("STATUS_OPPRETTET_AV")).orElse(null),
                result.getString("STATUS_OPPRETTET_AV_BRUKERID")
        ).setId(result.getLong("STATUS_ID"));
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private MalData mapTilMal(ResultSet result) {
        return new MalData()
                .setId(result.getLong("MAL_ID"))
                .setAktorId(result.getString("MAL_AKTORID"))
                .setMal(result.getString("MAL_MAL"))
                .setEndretAv(result.getString("MAL_ENDRET_AV"))
                .setDato(result.getTimestamp("MAL_DATO"));
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private InnstillingsHistorikkData mapRadTilInnstillingsHistorikkData(ResultSet result) {
        return new InnstillingsHistorikkData()
                .setManuell(result.getBoolean("MANUELL"))
                .setDato(result.getTimestamp("DATO"))
                .setBegrunnelse(result.getString("BEGRUNNELSE"))
                .setOpprettetAv(valueOfOptional(KodeverkBruker.class, result.getString("OPPRETTET_AV")).orElse(null))
                .setOpprettetAvBrukerId(result.getString("OPPRETTET_AV_BRUKERID"));

    }
}
