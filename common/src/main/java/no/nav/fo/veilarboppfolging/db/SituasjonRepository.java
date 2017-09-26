package no.nav.fo.veilarboppfolging.db;


import lombok.SneakyThrows;
import lombok.val;
import no.nav.fo.veilarboppfolging.domain.*;
import no.nav.sbl.jdbc.Database;
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

    private Database database;

    public SituasjonRepository(Database database) {
        this.database = database;
    }

    public Optional<Situasjon> hentSituasjon(String aktorId) {
        List<Situasjon> situasjon = database.query("" +
                        "SELECT" +
                        "  SITUASJON.AKTORID AS AKTORID, " +
                        "  SITUASJON.VEILEDER AS VEILEDER, " +
                        "  SITUASJON.OPPFOLGING AS OPPFOLGING, " +
                        "  SITUASJON.GJELDENDE_STATUS AS GJELDENDE_STATUS, " +
                        "  SITUASJON.GJELDENDE_ESKALERINGSVARSEL AS GJELDENDE_ESKALERINGSVARSEL, " +
                        "  SITUASJON.GJELDENDE_BRUKERVILKAR AS GJELDENDE_BRUKERVILKAR, " +
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
                        "  ESKALERINGSVARSEL.AVSLUTTET_BEGRUNNELSE AS ESK_AVSLUTTET_BEGRUNNELSE, " +
                        "  ESKALERINGSVARSEL.OPPRETTET_BEGRUNNELSE AS ESK_OPPRETTET_BEGRUNNELSE, " +
                        "  ESKALERINGSVARSEL.AVSLUTTET_AV AS ESK_AVSLUTTET_AV, " +
                        "  ESKALERINGSVARSEL.TILHORENDE_DIALOG_ID AS ESK_TILHORENDE_DIALOG_ID " +
                        "FROM situasjon " +
                        "LEFT JOIN status ON SITUASJON.GJELDENDE_STATUS = STATUS.ID " +
                        "LEFT JOIN brukervilkar ON SITUASJON.GJELDENDE_BRUKERVILKAR = BRUKERVILKAR.ID " +
                        "LEFT JOIN MAL ON SITUASJON.GJELDENDE_MAL = MAL.ID " +
                        "LEFT JOIN ESKALERINGSVARSEL ON SITUASJON.GJELDENDE_ESKALERINGSVARSEL = ESKALERINGSVARSEL.VARSEL_ID " +
                        "WHERE situasjon.aktorid = ? ",
                this::mapTilSituasjon,
                aktorId
        );

        return situasjon.isEmpty() ? Optional.empty() : situasjon.stream().findAny();
    }

    @Transactional
    public void startOppfolgingHvisIkkeAlleredeStartet(String aktorId) {
        if(!erOppfolgingsflaggSattForBruker(aktorId)) {
            database.update("UPDATE situasjon SET oppfolging = 1, OPPDATERT = CURRENT_TIMESTAMP WHERE aktorid = ?", aktorId);
            opprettOppfolgingsperiode(aktorId);
        }

    }

    private Boolean erOppfolgingsflaggSattForBruker(String aktorId) {
        return database.query("" +
                "SELECT " +
                "SITUASJON.OPPFOLGING AS OPPFOLGING " +
                "FROM situasjon " +
                "WHERE situasjon.aktorid = ? ",
                this::erUnderOppfolging,
                aktorId
        ).get(0);
    }

    private Boolean erUnderOppfolging(ResultSet result) throws SQLException {
        return result.getBoolean("OPPFOLGING");
    }

    @Transactional
    public void avsluttOppfolging(String aktorId, String veileder, String begrunnelse) {
        database.update("UPDATE situasjon SET oppfolging = 0, "
                + "veileder = null, "
                + "GJELDENDE_STATUS = null, "
                + "GJELDENDE_MAL = null, "
                + "GJELDENDE_BRUKERVILKAR = null, "
                + "OPPDATERT = CURRENT_TIMESTAMP "
                + "WHERE aktorid = ?",
                aktorId
        );
        avsluttOppfolgingsperiode(aktorId, veileder, begrunnelse);
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
        database.update("INSERT INTO situasjon(aktorid, oppfolging, oppdatert) VALUES(?, ?, CURRENT_TIMESTAMP)", aktorId, false);
        return new Situasjon().setAktorId(aktorId).setOppfolging(false);
    }

    public List<MalData> hentMalList(String aktorId) {
        return database.query("" +
                        "SELECT" +
                        "  ID AS MAL_ID, " +
                        "  AKTORID AS MAL_AKTORID, " +
                        "  MAL AS MAL_MAL, " +
                        "  ENDRET_AV AS MAL_ENDRET_AV, " +
                        "  DATO AS MAL_DATO " +
                        "FROM MAL " +
                        "WHERE AKTORID = ? " +
                        "ORDER BY ID DESC",
                this::mapTilMal,
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
        return database.query(sql, this::mapTilBrukervilkar, aktorId);
    }

    private void avsluttOppfolgingsperiode(String aktorId, String veileder, String begrunnelse) {
        database.update("" +
                        "UPDATE OPPFOLGINGSPERIODE " +
                        "SET avslutt_veileder = ?, " +
                        "avslutt_begrunnelse = ?, " +
                        "sluttDato = CURRENT_TIMESTAMP, " +
                        "oppdatert = CURRENT_TIMESTAMP " +
                        "WHERE aktorId = ? " +
                        "AND sluttDato IS NULL",
                veileder,
                begrunnelse,
                aktorId);
    }

    private void opprettOppfolgingsperiode(String aktorId) {
        database.update("" +
                        "INSERT INTO OPPFOLGINGSPERIODE(aktorId, startDato, oppdatert) " +
                        "VALUES (?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                aktorId);
    }

    public List<AvsluttetOppfolgingFeedData> hentAvsluttetOppfolgingEtterDato(Timestamp timestamp) {
        return database
                .query("SELECT aktorid, sluttdato, oppdatert " +
                                "FROM OPPFOLGINGSPERIODE " +
                                "WHERE oppdatert >= ? and sluttdato is not null",
                        this::mapRadTilAvsluttetOppfolging,
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
        database.update("UPDATE situasjon SET gjeldende_brukervilkar = ?, OPPDATERT = CURRENT_TIMESTAMP WHERE aktorid = ?",
                gjeldendeBrukervilkar.getId(),
                gjeldendeBrukervilkar.getAktorId()
        );
    }

    private void oppdaterSituasjonStatus(Status gjeldendeStatus) {
        database.update("UPDATE situasjon SET gjeldende_status = ?, OPPDATERT = CURRENT_TIMESTAMP WHERE aktorid = ?",
                gjeldendeStatus.getId(),
                gjeldendeStatus.getAktorId()
        );
    }

    private void oppdaterSituasjonMal(MalData mal) {
        database.update("UPDATE SITUASJON SET GJELDENDE_MAL = ?, OPPDATERT = CURRENT_TIMESTAMP WHERE AKTORID = ?",
                mal.getId(),
                mal.getAktorId()
        );
    }

    private void opprettSituasjonBrukervilkar(Brukervilkar vilkar) {
        database.update(
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
        database.update(
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
        return database.query(
                "SELECT manuell, dato, begrunnelse, opprettet_av, opprettet_av_brukerid " +
                        "FROM STATUS " +
                        "WHERE aktorid = ?",
                this::mapRadTilInnstillingsHistorikkData,
                aktorId);
    }

    private void opprettSituasjonMal(MalData mal) {
        database.update(
                "INSERT INTO MAL VALUES(?, ?, ?, ?, ?)",
                mal.getId(),
                mal.getAktorId(),
                mal.getMal(),
                mal.getEndretAv(),
                mal.getDato()
        );
    }

    @Transactional
    public void slettMalForAktorEtter(String aktorId, Date date) {
        database.update("UPDATE SITUASJON SET GJELDENDE_MAL = NULL WHERE AKTORID = ?", aktorId);
        database.update("DELETE FROM MAL WHERE AKTORID = ? AND DATO > ?", aktorId, date);
    }

    private long nesteFraSekvens(String sekvensNavn) {
        return database.nesteFraSekvens(sekvensNavn);
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
                .setGjeldendeMal(
                        Optional.ofNullable(resultat.getLong("GJELDENDE_MAL"))
                                .map(m -> m != 0 ? mapTilMal(resultat) : null)
                                .orElse(null)
                )
                .setOppfolgingsperioder(hentOppfolgingsperioder(aktorId))
                .setGjeldendeEskaleringsvarsel(
                        Optional.ofNullable(resultat.getLong("gjeldende_eskaleringsvarsel"))
                                .map(e -> e != 0 ? mapTilEskaleringsvarselData(resultat) : null)
                                .orElse(null)
                );
    }


    private static String hentOppfolingsperioderSQL =
            "SELECT AKTORID, AVSLUTT_VEILEDER, STARTDATO, SLUTTDATO, AVSLUTT_BEGRUNNELSE " +
            "FROM OPPFOLGINGSPERIODE ";

    public List<Oppfolgingsperiode> hentOppfolgingsperioder(String aktorid) {
        return database.query(hentOppfolingsperioderSQL +
                        "WHERE AKTORID = ?",
                this::mapTilOppfolgingsperiode,
                aktorid
        );
    }

    public List<Oppfolgingsperiode> hentAvsluttetOppfolgingsperioder(String aktorid) {
        return database.query(hentOppfolingsperioderSQL +
                        "WHERE AKTORID = ? AND SLUTTDATO is not null",
                this::mapTilOppfolgingsperiode,
                aktorid
        );
    }


    private EskaleringsvarselData hentEskaleringsvarsel(String aktorId) {
        List<EskaleringsvarselData> eskalering = database.query("" +
                "SELECT " +
                "VARSEL_ID AS ESK_ID, " +
                "AKTOR_ID AS ESK_AKTOR_ID, " +
                "OPPRETTET_AV AS ESK_OPPRETTET_AV, " +
                "OPPRETTET_DATO AS ESK_OPPRETTET_DATO, " +
                "AVSLUTTET_DATO AS ESK_AVSLUTTET_DATO, " +
                "AVSLUTTET_AV AS ESK_AVSLUTTET_AV, " +
                "TILHORENDE_DIALOG_ID AS ESK_TILHORENDE_DIALOG_ID, " +
                "OPPRETTET_BEGRUNNELSE AS ESK_OPPRETTET_BEGRUNNELSE, " +
                "AVSLUTTET_BEGRUNNELSE AS ESK_AVSLUTTET_BEGRUNNELSE " +
                "FROM ESKALERINGSVARSEL " +
                "WHERE varsel_id IN (SELECT gjeldende_eskaleringsvarsel FROM SITUASJON WHERE SITUASJON.aktorid = ?)",
                this::mapTilEskaleringsvarselData,
                aktorId
        );

        return eskalering.stream()
                .findAny()
                .orElse(null);

    }

    public List<EskaleringsvarselData> hentEskaleringhistorikk(String aktorId) {
        return database.query("SELECT " +
                        "VARSEL_ID AS ESK_ID, " +
                        "AKTOR_ID AS ESK_AKTOR_ID, " +
                        "OPPRETTET_AV AS ESK_OPPRETTET_AV, " +
                        "OPPRETTET_DATO AS ESK_OPPRETTET_DATO, " +
                        "AVSLUTTET_AV AS ESK_AVSLUTTET_AV, " +
                        "AVSLUTTET_DATO AS ESK_AVSLUTTET_DATO, " +
                        "TILHORENDE_DIALOG_ID AS ESK_TILHORENDE_DIALOG_ID, " +
                        "AVSLUTTET_BEGRUNNELSE AS ESK_AVSLUTTET_BEGRUNNELSE, " +
                        "OPPRETTET_BEGRUNNELSE AS ESK_OPPRETTET_BEGRUNNELSE " +
                        "FROM ESKALERINGSVARSEL " +
                        "WHERE aktor_id = ?",
                this::mapTilEskaleringsvarselData,
                aktorId
        );
    }

    @Transactional
    public void startEskalering(String aktorId, String opprettetAv, String opprettetBegrunnelse, long tilhorendeDialogId) {
        val harEksisterendeEskalering = hentEskaleringsvarsel(aktorId) != null;
        if (harEksisterendeEskalering) {
            throw new RuntimeException();
        }

        val id = nesteFraSekvens("ESKALERINGSVARSEL_SEQ");

        database.update("" +
                "INSERT INTO ESKALERINGSVARSEL(varsel_id, " +
                        "aktor_id, " +
                        "opprettet_av, " +
                        "opprettet_dato, " +
                        "opprettet_begrunnelse, " +
                        "tilhorende_dialog_id)" +
                "VALUES(?, ?, ?, CURRENT_TIMESTAMP, ?, ?)",
                id,
                aktorId,
                opprettetAv,
                opprettetBegrunnelse,
                tilhorendeDialogId
        );

        database.update("" +
                "UPDATE SITUASJON " +
                "SET gjeldende_eskaleringsvarsel = ?, " +
                "OPPDATERT = CURRENT_TIMESTAMP " +
                "WHERE aktorid = ?",
                id,
                aktorId
        );
    }

    @Transactional
    public void stoppEskalering(String aktorId, String avsluttetAv, String avsluttetBegrunnelse) {
        val eskalering = hentEskaleringsvarsel(aktorId);
        val harIkkeEnEksisterendeEskalering = eskalering == null;
        if(harIkkeEnEksisterendeEskalering) {
            throw new RuntimeException();
        }

        database.update("" +
                "UPDATE ESKALERINGSVARSEL " +
                "SET avsluttet_dato = CURRENT_TIMESTAMP, avsluttet_begrunnelse = ?, avsluttet_av = ? " +
                "WHERE VARSEL_ID = ?",
                avsluttetBegrunnelse,
                avsluttetAv,
                eskalering.getVarselId()
        );
        database.update("" +
                "UPDATE SITUASJON " +
                "SET gjeldende_eskaleringsvarsel = null, " +
                "OPPDATERT = CURRENT_TIMESTAMP " +
                "WHERE aktorid = ?",
                aktorId
        );
    }

    @SneakyThrows
    private EskaleringsvarselData mapTilEskaleringsvarselData(ResultSet result) {
        return EskaleringsvarselData.builder()
                .varselId(result.getLong("ESK_ID"))
                .aktorId(result.getString("ESK_AKTOR_ID"))
                .opprettetAv(result.getString("ESK_OPPRETTET_AV"))
                .opprettetDato(hentDato(result, "ESK_OPPRETTET_DATO"))
                .opprettetBegrunnelse(result.getString("ESK_OPPRETTET_BEGRUNNELSE"))
                .avsluttetDato(hentDato(result, "ESK_AVSLUTTET_DATO"))
                .avsluttetBegrunnelse(result.getString( "ESK_AVSLUTTET_BEGRUNNELSE"))
                .avsluttetAv(result.getString( "ESK_AVSLUTTET_AV"))
                .tilhorendeDialogId(result.getLong("ESK_TILHORENDE_DIALOG_ID"))
                .build();
    }

    private Oppfolgingsperiode mapTilOppfolgingsperiode(ResultSet result) throws SQLException {
        return Oppfolgingsperiode.builder()
                .aktorId(result.getString("aktorid"))
                .veileder(result.getString("avslutt_veileder"))
                .startDato(hentDato(result, "startdato"))
                .sluttDato(hentDato(result, "sluttdato"))
                .begrunnelse(result.getString("avslutt_begrunnelse"))
                .build();
    }

    private static Date hentDato(ResultSet rs, String kolonneNavn) throws SQLException {
        return ofNullable(rs.getTimestamp(kolonneNavn))
                .map(Timestamp::getTime)
                .map(Date::new)
                .orElse(null);
    }

    @SneakyThrows
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
    private MalData mapTilMal(ResultSet result) {
        return new MalData()
                .setId(result.getLong("MAL_ID"))
                .setAktorId(result.getString("MAL_AKTORID"))
                .setMal(result.getString("MAL_MAL"))
                .setEndretAv(result.getString("MAL_ENDRET_AV"))
                .setDato(result.getTimestamp("MAL_DATO"));
    }

    @SneakyThrows
    private InnstillingsHistorikkData mapRadTilInnstillingsHistorikkData(ResultSet result) {
        return new InnstillingsHistorikkData()
                .setManuell(result.getBoolean("MANUELL"))
                .setDato(result.getTimestamp("DATO"))
                .setBegrunnelse(result.getString("BEGRUNNELSE"))
                .setOpprettetAv(valueOfOptional(KodeverkBruker.class, result.getString("OPPRETTET_AV")).orElse(null))
                .setOpprettetAvBrukerId(result.getString("OPPRETTET_AV_BRUKERID"));

    }

}
