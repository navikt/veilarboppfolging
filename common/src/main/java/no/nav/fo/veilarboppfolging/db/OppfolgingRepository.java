package no.nav.fo.veilarboppfolging.db;


import lombok.val;
import no.nav.apiapp.feil.Feil;
import no.nav.fo.veilarboppfolging.domain.*;
import no.nav.sbl.jdbc.Database;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static no.nav.apiapp.feil.Feil.Type.UGYLDIG_HANDLING;

public class OppfolgingRepository {

    private Database database;

    private final OppfolgingsStatusRepository statusRepository;
    private final OppfolgingsPeriodeRepository periodeRepository;
    private final MaalRepository maalRepository;
    private final ManuellStatusRepository manuellStatusRepository;
    private final BrukervilkarRepository brukervilkarRepository;
    private final EskaleringsvarselRepository eskaleringsvarselRepository;

    public OppfolgingRepository(Database database) {
        this.database = database;
        statusRepository = new OppfolgingsStatusRepository(database);
        periodeRepository = new OppfolgingsPeriodeRepository(database);
        maalRepository = new MaalRepository(database);
        manuellStatusRepository = new ManuellStatusRepository(database);
        brukervilkarRepository = new BrukervilkarRepository(database);
        eskaleringsvarselRepository = new EskaleringsvarselRepository(database);
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

    @Transactional
    public void opprettBrukervilkar(Brukervilkar brukervilkar) {
        brukervilkar.setId(nesteFraSekvens("brukervilkar_seq"));
        brukervilkarRepository.create(brukervilkar);
        statusRepository.oppdaterOppfolgingBrukervilkar(brukervilkar);
    }

    public Oppfolging opprettOppfolging(String aktorId) {
        statusRepository.opprettOppfolging(aktorId);

        // FIXME: return the actual database object.
        return new Oppfolging().setAktorId(aktorId).setUnderOppfolging(false);
    }

    // FIXME: go directly to the repository instead.
    public List<Brukervilkar> hentHistoriskeVilkar(String aktorId) {
        return brukervilkarRepository.history(aktorId);
    }

    // FIXME: go directly to the repository instead.
    public List<AvsluttetOppfolgingFeedData> hentAvsluttetOppfolgingEtterDato(Timestamp timestamp, int pageSize) {
        return periodeRepository.hentAvsluttetOppfolgingEtterDato(timestamp, pageSize);
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
                                .map(b -> b != 0 ? BrukervilkarRepository.map(resultat) : null)
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
                                .map(e -> e != 0 ? EskaleringsvarselRepository.map(resultat) : null)
                                .orElse(null)
                );
    }

    // FIXME: go directly to the repository instead.
    public List<Oppfolgingsperiode> hentAvsluttetOppfolgingsperioder(String aktorId) {
        return periodeRepository.hentAvsluttetOppfolgingsperioder(aktorId);
    }

    // FIXME: go directly to the repository instead.
    public List<EskaleringsvarselData> hentEskaleringhistorikk(String aktorId) {
        return eskaleringsvarselRepository.history(aktorId);
    }

    @Transactional
    public void startEskalering(String aktorId, String opprettetAv, String opprettetBegrunnelse, long tilhorendeDialogId) {
        if (eskaleringsvarselRepository.fetchByAktorId(aktorId) != null) {
            throw new Feil(UGYLDIG_HANDLING, "Brukeren har allerede et aktivt eskaleringsvarsel.");
        }

        long id = nesteFraSekvens("ESKALERINGSVARSEL_SEQ");

        val e = EskaleringsvarselData.builder()
                .aktorId(aktorId)
                .opprettetAv(opprettetAv)
                .opprettetBegrunnelse(opprettetBegrunnelse)
                .tilhorendeDialogId(tilhorendeDialogId)
                .build();
        eskaleringsvarselRepository.create(e);
        statusRepository.setGjeldendeEskaleringsvarsel(aktorId, id);
    }

    @Transactional
    public void stoppEskalering(String aktorId, String avsluttetAv, String avsluttetBegrunnelse) {
        EskaleringsvarselData eskalering = eskaleringsvarselRepository.fetchByAktorId(aktorId);
        if (eskalering == null) {
            throw new Feil(UGYLDIG_HANDLING, "Brukeren har ikke et aktivt eskaleringsvarsel.");
        }

        eskalering = eskalering
                .withAvsluttetAv(avsluttetAv)
                .withAvsluttetBegrunnelse(avsluttetBegrunnelse);

        eskaleringsvarselRepository.finish(eskalering);
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
}
