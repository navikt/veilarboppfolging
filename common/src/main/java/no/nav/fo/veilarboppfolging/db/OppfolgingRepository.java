package no.nav.fo.veilarboppfolging.db;


import lombok.SneakyThrows;
import lombok.val;
import no.nav.fo.veilarboppfolging.domain.*;
import no.nav.sbl.jdbc.Database;
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

public class OppfolgingRepository {

    private Database database;

    private final OppfolgingsStatusRepository statusRepository;
    private final OppfolgingsPeriodeRepository periodeRepository;
    private final MaalRepository maalRepository;
    private final ManuellStatusRepository manuellStatusRepository;

    public OppfolgingRepository(Database database, JdbcTemplate jdbcTemplate) {
        this.database = database;
        statusRepository = new OppfolgingsStatusRepository(jdbcTemplate);
        periodeRepository = new OppfolgingsPeriodeRepository(database);
        maalRepository = new MaalRepository(database);
        manuellStatusRepository = new ManuellStatusRepository(database);
    }

    public Optional<Oppfolging> hentOppfolging(String aktorId) {
        List<Oppfolging> oppfolging = database.query("" +
                        "SELECT" +
                        "  OPPFOLGINGSTATUS.aktor_id AS aktor_id, " +
                        "  OPPFOLGINGSTATUS.veileder AS veileder, " +
                        "  OPPFOLGINGSTATUS.under_oppfolging AS under_oppfolging, " +
                        "  OPPFOLGINGSTATUS.gjeldende_manuell_status AS gjeldende_manuell_status, " +
                        "  OPPFOLGINGSTATUS.gjeldende_eskaleringsvarsel AS gjeldende_eskaleringsvarsel, " +
                        "  OPPFOLGINGSTATUS.gjeldende_brukervilkar AS gjeldende_brukervilkar, " +
                        "  OPPFOLGINGSTATUS.gjeldende_mal AS gjeldende_mal, " +
                        "  MANUELL_STATUS.id AS ms_id, " +
                        "  MANUELL_STATUS.aktor_id AS ms_aktor_id, " +
                        "  MANUELL_STATUS.manuell AS ms_manuell, " +
                        "  MANUELL_STATUS.opprettet_dato AS ms_opprettet_dato, " +
                        "  MANUELL_STATUS.begrunnelse AS ms_begrunnelse, " +
                        "  MANUELL_STATUS.opprettet_av AS ms_opprettet_av, " +
                        "  MANUELL_STATUS.opprettet_av_brukerid AS ms_opprettet_av_brukerid, " +
                        "  BRUKERVILKAR.id AS brukervilkar_id, " +
                        "  BRUKERVILKAR.aktor_id AS brukervilkar_aktor_id, " +
                        "  BRUKERVILKAR.dato AS brukervilkar_dato, " +
                        "  BRUKERVILKAR.vilkarstatus AS brukervilkar_vilkarstatus, " +
                        "  BRUKERVILKAR.tekst AS brukervilkar_tekst, " +
                        "  BRUKERVILKAR.hash AS brukervilkar_hash, " +
                        "  MAL.id AS mal_id, " +
                        "  MAL.aktor_id AS mal_aktor_id, " +
                        "  MAL.mal AS mal_mal, " +
                        "  MAL.endret_av AS mal_endret_av, " +
                        "  MAL.dato AS mal_dato, " +
                        "  ESKALERINGSVARSEL.varsel_id AS esk_id, " +
                        "  ESKALERINGSVARSEL.aktor_id AS esk_aktor_id, " +
                        "  ESKALERINGSVARSEL.opprettet_av AS esk_opprettet_av, " +
                        "  ESKALERINGSVARSEL.opprettet_dato AS esk_opprettet_dato, " +
                        "  ESKALERINGSVARSEL.avsluttet_dato AS esk_avsluttet_dato, " +
                        "  ESKALERINGSVARSEL.avsluttet_begrunnelse AS esk_avsluttet_begrunnelse, " +
                        "  ESKALERINGSVARSEL.opprettet_begrunnelse AS esk_opprettet_begrunnelse, " +
                        "  ESKALERINGSVARSEL.avsluttet_av AS esk_avsluttet_av, " +
                        "  ESKALERINGSVARSEL.tilhorende_dialog_id AS esk_tilhorende_dialog_id " +
                        "FROM OPPFOLGINGSTATUS " +
                        "LEFT JOIN MANUELL_STATUS ON OPPFOLGINGSTATUS.gjeldende_manuell_status = MANUELL_STATUS.id " +
                        "LEFT JOIN BRUKERVILKAR ON OPPFOLGINGSTATUS.gjeldende_brukervilkar = BRUKERVILKAR.id " +
                        "LEFT JOIN MAL ON OPPFOLGINGSTATUS.gjeldende_mal = MAL.id " +
                        "LEFT JOIN ESKALERINGSVARSEL ON OPPFOLGINGSTATUS.gjeldende_eskaleringsvarsel = ESKALERINGSVARSEL.varsel_id " +
                        "WHERE OPPFOLGINGSTATUS.aktor_id = ? ",
                this::mapTilOppfolging,
                aktorId
        );

        return oppfolging.isEmpty() ? Optional.empty() : oppfolging.stream().findAny();
    }

    @Transactional
    public void startOppfolgingHvisIkkeAlleredeStartet(String aktorId) {
        if (!statusRepository.erOppfolgingsflaggSattForBruker(aktorId)) {
            statusRepository.setUnderOppfolging(aktorId);
            periodeRepository.opprettOppfolgingsperiode(aktorId);
        }
    }

    @Transactional
    public void avsluttOppfolging(String aktorId, String veileder, String begrunnelse) {
        statusRepository.avsluttOppfolging(aktorId);
        periodeRepository.avsluttOppfolgingsperiode(aktorId, veileder, begrunnelse);
    }

    // FIXME: OPPFOLGINGSSTATUS table should have a foreign key constraint on GJELDENDE_MANUELL_STATUS
    @Transactional
    public void opprettManuellStatus(ManuellStatus manuellStatus) {
        manuellStatus.setId(nesteFraSekvens("status_seq"));
        manuellStatusRepository.create(manuellStatus);
        statusRepository.oppdaterManuellStatus(manuellStatus);
    }

    public void opprettBrukervilkar(Brukervilkar brukervilkar) {
        brukervilkar.setId(nesteFraSekvens("brukervilkar_seq"));
        opprettOppfolgingBrukervilkar(brukervilkar);
        statusRepository.oppdaterOppfolgingBrukervilkar(brukervilkar);
    }

    public Oppfolging opprettOppfolging(String aktorId) {
        statusRepository.opprettOppfolging(aktorId);

        // FIXME: return the actual database object.
        return new Oppfolging().setAktorId(aktorId).setUnderOppfolging(false);
    }

    public List<Brukervilkar> hentHistoriskeVilkar(String aktorId) {
        String sql =
                "SELECT " +
                "id AS brukervilkar_id, " +
                "aktor_id AS aktor_id, " +
                "dato AS brukervilkar_dato, " +
                "vilkarstatus AS brukervilkar_vilkarstatus, " +
                "tekst AS brukervilkar_tekst, " +
                "hash AS brukervilkar_hash " +
                "FROM BRUKERVILKAR " +
                "WHERE aktor_id = ? " +
                "ORDER BY dato DESC";
        return database.query(sql, this::mapTilBrukervilkar, aktorId);
    }

    // FIXME: go directly to the repository instead.
    public List<AvsluttetOppfolgingFeedData> hentAvsluttetOppfolgingEtterDato(Timestamp timestamp, int pageSize) {
        return periodeRepository.hentAvsluttetOppfolgingEtterDato(timestamp, pageSize);
    }

    private void opprettOppfolgingBrukervilkar(Brukervilkar vilkar) {
        database.update(
                "INSERT INTO BRUKERVILKAR(id, aktor_id, dato, vilkarstatus, tekst, hash) VALUES(?, ?, ?, ?, ?, ?)",
                vilkar.getId(),
                vilkar.getAktorId(),
                vilkar.getDato(),
                vilkar.getVilkarstatus().name(),
                vilkar.getTekst(),
                vilkar.getHash()

        );
    }

    // FIXME: go directly to the repository instead.
    public List<ManuellStatus> hentManuellHistorikk(String aktorId) {
        return manuellStatusRepository.history(aktorId);
    }

    private long nesteFraSekvens(String sekvensNavn) {
        return database.nesteFraSekvens(sekvensNavn);
    }

    private Oppfolging mapTilOppfolging(ResultSet resultat) throws SQLException {
        String aktorId = resultat.getString("aktor_id");
        return new Oppfolging()
                .setAktorId(aktorId)
                .setVeilederId(resultat.getString("veileder"))
                .setUnderOppfolging(resultat.getBoolean("under_oppfolging"))
                .setGjeldendeManuellStatus(
                        Optional.ofNullable(resultat.getLong("gjeldende_manuell_status"))
                                .map(s -> s != 0 ? ManuellStatusRepository.map(resultat) : null)
                                .orElse(null)
                )
                .setGjeldendeBrukervilkar(
                        Optional.ofNullable(resultat.getLong("gjeldende_brukervilkar"))
                                .map(b -> b != 0 ? mapTilBrukervilkar(resultat) : null)
                                .orElse(null)
                )
                .setGjeldendeMal(
                        Optional.ofNullable(resultat.getLong("gjeldende_mal"))
                                .map(m -> m != 0 ? MaalRepository.map(resultat) : null)
                                .orElse(null)
                )
                .setOppfolgingsperioder(periodeRepository.hentOppfolgingsperioder(aktorId))
                .setGjeldendeEskaleringsvarsel(
                        Optional.ofNullable(resultat.getLong("gjeldende_eskaleringsvarsel"))
                                .map(e -> e != 0 ? mapTilEskaleringsvarselData(resultat) : null)
                                .orElse(null)
                );
    }

    // FIXME: go directly to the repository instead.
    public List<Oppfolgingsperiode> hentAvsluttetOppfolgingsperioder(String aktorId) {
        return periodeRepository.hentAvsluttetOppfolgingsperioder(aktorId);
    }


    private EskaleringsvarselData hentEskaleringsvarsel(String aktorId) {
        List<EskaleringsvarselData> eskalering = database.query("" +
                "SELECT " +
                "varsel_id AS esk_id, " +
                "aktor_id AS esk_aktor_id, " +
                "opprettet_av AS esk_opprettet_av, " +
                "opprettet_dato AS esk_opprettet_dato, " +
                "avsluttet_dato AS esk_avsluttet_dato, " +
                "avsluttet_av AS esk_avsluttet_av, " +
                "tilhorende_dialog_id AS esk_tilhorende_dialog_id, " +
                "opprettet_begrunnelse AS esk_opprettet_begrunnelse, " +
                "avsluttet_begrunnelse AS esk_avsluttet_begrunnelse " +
                "FROM eskaleringsvarsel " +
                "WHERE varsel_id IN (SELECT gjeldende_eskaleringsvarsel FROM OPPFOLGINGSTATUS WHERE aktor_id = ?)",
                this::mapTilEskaleringsvarselData,
                aktorId
        );

        return eskalering.stream()
                .findAny()
                .orElse(null);

    }

    public List<EskaleringsvarselData> hentEskaleringhistorikk(String aktorId) {
        return database.query("SELECT " +
                        "varsel_id AS esk_id, " +
                        "aktor_id AS esk_aktor_id, " +
                        "opprettet_av AS esk_opprettet_av, " +
                        "opprettet_dato AS esk_opprettet_dato, " +
                        "avsluttet_av AS esk_avsluttet_av, " +
                        "avsluttet_dato AS esk_avsluttet_dato, " +
                        "tilhorende_dialog_id AS esk_tilhorende_dialog_id, " +
                        "avsluttet_begrunnelse AS esk_avsluttet_begrunnelse, " +
                        "opprettet_begrunnelse AS esk_opprettet_begrunnelse " +
                        "FROM eskaleringsvarsel " +
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

        long id = nesteFraSekvens("ESKALERINGSVARSEL_SEQ");

        database.update("" +
                "INSERT INTO ESKALERINGSVARSEL(" +
                        "varsel_id, " +
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
                "UPDATE OPPFOLGINGSTATUS " +
                "SET gjeldende_eskaleringsvarsel = ?, " +
                "oppdatert = CURRENT_TIMESTAMP " +
                "WHERE aktor_id = ?",
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
                "WHERE varsel_id = ?",
                avsluttetBegrunnelse,
                avsluttetAv,
                eskalering.getVarselId()
        );
        statusRepository.fjernEskalering(aktorId);
    }

    // FIXME: go directly to maalRepository instead.
    public List<MalData> hentMalList(String aktorId) {
        return maalRepository.aktorMal(aktorId);
    }

    // FIXME: OPPFOLGINGSSTATUS table should have a foreign key constraint on GJELDENDE_MAL
    @Transactional
    public void opprettMal(MalData mal) {
        mal.setId(nesteFraSekvens("MAL_SEQ"));
        statusRepository.oppdaterOppfolgingMal(mal);
        maalRepository.opprett(mal);
    }

    @Transactional
    public void slettMalForAktorEtter(String aktorId, Date date) {
        statusRepository.fjernMaal(aktorId);
        maalRepository.slettForAktorEtter(aktorId, date);
    }

    private static Date hentDato(ResultSet rs, String kolonneNavn) throws SQLException {
        return ofNullable(rs.getTimestamp(kolonneNavn))
                .map(Timestamp::getTime)
                .map(Date::new)
                .orElse(null);
    }

    @SneakyThrows
    private EskaleringsvarselData mapTilEskaleringsvarselData(ResultSet result) {
        return EskaleringsvarselData.builder()
                .varselId(result.getLong("esk_id"))
                .aktorId(result.getString("esk_aktor_id"))
                .opprettetAv(result.getString("esk_opprettet_av"))
                .opprettetDato(hentDato(result, "esk_opprettet_dato"))
                .opprettetBegrunnelse(result.getString("esk_opprettet_begrunnelse"))
                .avsluttetDato(hentDato(result, "esk_avsluttet_dato"))
                .avsluttetBegrunnelse(result.getString( "esk_avsluttet_begrunnelse"))
                .avsluttetAv(result.getString( "esk_avsluttet_av"))
                .tilhorendeDialogId(result.getLong("esk_tilhorende_dialog_id"))
                .build();
    }

    @SneakyThrows
    private Brukervilkar mapTilBrukervilkar(ResultSet result) {
        return new Brukervilkar(
                result.getString("aktor_id"),
                result.getTimestamp("brukervilkar_dato"),
                VilkarStatus.valueOf(result.getString("brukervilkar_vilkarstatus")),
                result.getString("brukervilkar_tekst"),
                result.getString("brukervilkar_hash")
        ).setId(result.getLong("brukervilkar_id"));
    }
}
