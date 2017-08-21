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
                        "  SITUASJON.VEILEDER AS VEILEDER, " +
                        "  SITUASJON.OPPFOLGING AS OPPFOLGING, " +
                        "  SITUASJON.GJELDENDE_STATUS AS GJELDENDE_STATUS, " +
                        "  SITUASJON.GJELDENDE_ESKALERINGSVARSEL AS GJELDENDE_ESKALERINGSVARSEL, " +
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
                        "  MAL.DATO AS MAL_DATO, " +
                        "  ESKALERINGSVARSEL.VARSEL_ID AS ESK_ID, " +
                        "  ESKALERINGSVARSEL.AKTOR_ID AS ESK_AKTOR_ID, " +
                        "  ESKALERINGSVARSEL.OPPRETTET_AV AS ESK_OPPRETTET_AV, " +
                        "  ESKALERINGSVARSEL.OPPRETTET_DATO AS ESK_OPPRETTET_DATO, " +
                        "  ESKALERINGSVARSEL.AVSLUTTET_DATO AS ESK_AVSLUTTET_DATO, " +
                        "  ESKALERINGSVARSEL.TILHORENDE_DIALOG_ID AS ESK_TILHORENDE_DIALOG_ID " +
                        "FROM situasjon " +
                        "LEFT JOIN status ON SITUASJON.GJELDENDE_STATUS = STATUS.ID " +
                        "LEFT JOIN brukervilkar ON SITUASJON.GJELDENDE_BRUKERVILKAR = BRUKERVILKAR.ID " +
                        "LEFT JOIN MAL ON SITUASJON.GJELDENDE_MAL = MAL.ID " +
                        "LEFT JOIN ESKALERINGSVARSEL ON SITUASJON.GJELDENDE_ESKALERINGSVARSEL = ESKALERINGSVARSEL.VARSEL_ID " +
                        "WHERE situasjon.aktorid = ? ",
                (result, n) -> mapTilSituasjon(result),
                aktorId
        );

        return situasjon.isEmpty() ? Optional.empty() : situasjon.stream().findAny();
    }

    public void startOppfolging(String aktorId) {
        jdbcTemplate.update("UPDATE situasjon SET oppfolging = 1, OPPDATERT = CURRENT_TIMESTAMP WHERE aktorid = ?",
                aktorId
        );
    }

    public void avsluttOppfolging(String aktorId) {
        jdbcTemplate.update("UPDATE situasjon SET oppfolging = 0, "
                + "veileder = null, "
                + "GJELDENDE_STATUS = null, "
                + "GJELDENDE_MAL = null, "
                + "GJELDENDE_BRUKERVILKAR = null, "
                + "OPPDATERT = CURRENT_TIMESTAMP "
                + "WHERE aktorid = ?",
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

    public Situasjon opprettSituasjon(String aktorId) {
        jdbcTemplate.update("INSERT INTO situasjon(aktorid, oppfolging, oppdatert) VALUES(?, ?, CURRENT_TIMESTAMP)", aktorId, false);
        return new Situasjon().setAktorId(aktorId).setOppfolging(false);
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


    public void oppdaterOppfolgingsperiode(Oppfolgingsperiode oppfolgingperiode) {
        jdbcTemplate.update("" +
                        "UPDATE OPPFOLGINGSPERIODE " +
                        "SET veileder=?, sluttDato=?, begrunnelse=?, oppdatert=CURRENT_TIMESTAMP " +
                        "WHERE aktorId = ? AND sluttDato IS NULL",
                oppfolgingperiode.getVeileder(),
                oppfolgingperiode.getSluttDato(),
                oppfolgingperiode.getBegrunnelse(),
                oppfolgingperiode.getAktorId());
    }


    public void opprettOppfolgingsperiode(Oppfolgingsperiode oppfolgingperiode) {
        jdbcTemplate.update("" +
                        "INSERT INTO OPPFOLGINGSPERIODE(aktorId, veileder, startDato, sluttDato, begrunnelse, oppdatert) " +
                        "VALUES (?,?,?,?,?,CURRENT_TIMESTAMP)",
                oppfolgingperiode.getAktorId(),
                null,
                oppfolgingperiode.getStartDato(),
                null,
                null);
    }

    public List<AvsluttetOppfolgingFeedData> hentAvsluttetOppfolgingEtterDato(Timestamp timestamp) {
        return jdbcTemplate
                .query("SELECT aktorid, sluttdato, oppdatert " +
                                "FROM OPPFOLGINGSPERIODE " +
                                "WHERE oppdatert >= ? and sluttdato is not null",
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
        jdbcTemplate.update("UPDATE situasjon SET gjeldende_brukervilkar = ?, OPPDATERT = CURRENT_TIMESTAMP WHERE aktorid = ?",
                gjeldendeBrukervilkar.getId(),
                gjeldendeBrukervilkar.getAktorId()
        );
    }

    private void oppdaterSituasjonStatus(Status gjeldendeStatus) {
        jdbcTemplate.update("UPDATE situasjon SET gjeldende_status = ?, OPPDATERT = CURRENT_TIMESTAMP WHERE aktorid = ?",
                gjeldendeStatus.getId(),
                gjeldendeStatus.getAktorId()
        );
    }

    private void oppdaterSituasjonMal(MalData mal) {
        jdbcTemplate.update("UPDATE SITUASJON SET GJELDENDE_MAL = ?, OPPDATERT = CURRENT_TIMESTAMP WHERE AKTORID = ?",
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
                .setVeilederId(resultat.getString("veileder"))
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
                .setOppfolgingsperioder(hentOppfolgingsperioder(aktorId))
                .setGjeldendeEskaleringsvarsel(
                        Optional.ofNullable(resultat.getLong("gjeldende_eskaleringsvarsel"))
                                .map(e -> e != 0 ? mapTilEskaleringstatusData(resultat) : null)
                                .orElse(null)
                );
    }


    private static String hentOppfolingsperioderSQL =
            "SELECT AKTORID, VEILEDER, STARTDATO, SLUTTDATO, BEGRUNNELSE " +
            "FROM OPPFOLGINGSPERIODE ";

    public List<Oppfolgingsperiode> hentOppfolgingsperioder(String aktorid) {
        return jdbcTemplate.query(hentOppfolingsperioderSQL +
                        "WHERE AKTORID = ?",
                (result, n) -> mapTilOppfolgingsperiode(result),
                aktorid
        );
    }

    public List<Oppfolgingsperiode> hentAvsluttetOppfolgingsperioder(String aktorid) {
        return jdbcTemplate.query(hentOppfolingsperioderSQL +
                        "WHERE AKTORID = ? AND SLUTTDATO is not null",
                (result, n) -> mapTilOppfolgingsperiode(result),
                aktorid
        );
    }


    public EskaleringstatusData hentEskaleringstatus(String aktorId) {
        return jdbcTemplate.query("" +
                "SELECT * FROM ESKALERINGSVARSEL " +
                "WHERE varsel_id IN (SELECT gjeldende_eskaleringsvarsel FROM SITUASJON WHERE SITUASJON.aktorid = ?)",
                this::mapTilEskaleringstatusData,
                aktorId
        );
    }

    public List<EskaleringstatusData> hentEskaleringhistorikk(String aktorId) {
        return jdbcTemplate.query("" +
                "SELECT * FROM ESKALERINGSVARSEL " +
                "WHERE aktor_id = ?",
                (result, n) -> mapTilEskaleringstatusData(result),
                aktorId
        );
    }

    @Transactional
    public void startEskalering(String aktorId, String opprettetAv, int tilhorendeDialogId) {
        if (hentEskaleringstatus(aktorId) != null) {
            throw new RuntimeException();
        }
        jdbcTemplate.update("" +
                "INSERT INTO ESKALERINGSVARSEL(aktor_id, opprettet_av, opprettet_dato, tilhorende_dialog_id) " +
                "VALUES(?, ?, CURRENT_TIMESTAMP, ?)",
                aktorId,
                opprettetAv,
                tilhorendeDialogId
        );
        jdbcTemplate.update("" +
                "UPDATE SITUASJON " +
                "SET gjeldende_eskaleringsvarsel = (SELECT MAX(varsel_id) FROM ESKALERINGSVARSEL) " +
                "WHERE aktorid = ?",
                aktorId
        );
    }

    @Transactional
    public void stoppEskalering(String aktorId) {
        if(hentEskaleringstatus(aktorId) == null) {
            throw new RuntimeException();
        }
        jdbcTemplate.update("" +
                "UPDATE ESKALERINGSVARSEL " +
                "SET avsluttet_dato = CURRENT_TIMESTAMP " +
                "WHERE aktor_id = ?",
                aktorId
        );
        jdbcTemplate.update("" +
                "UPDATE SITUASJON " +
                "SET gjeldende_eskaleringsvarsel = null " +
                "WHERE aktorid = ?",
                aktorId
        );
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private EskaleringstatusData mapTilEskaleringstatusData(ResultSet result) {
        if (!result.isBeforeFirst()) {
            return null;
        }
        return EskaleringstatusData.builder()
                .varselId(result.getLong("varsel_id"))
                .aktorId(result.getString("aktor_id"))
                .opprettetAv(result.getString("opprettet_av"))
                .opprettetDato(hentDato(result, "opprettet_dato"))
                .avsluttetDato(hentDato(result, "avsluttet_dato"))
                .tilhorendeDialogId(result.getLong("tilhorende_dialog_id"))
                .build();
    }

    private Oppfolgingsperiode mapTilOppfolgingsperiode(ResultSet result) throws SQLException {
        return Oppfolgingsperiode.builder()
                .aktorId(result.getString("aktorid"))
                .veileder(result.getString("veileder"))
                .startDato(hentDato(result, "startdato"))
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
