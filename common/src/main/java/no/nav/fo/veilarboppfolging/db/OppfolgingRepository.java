package no.nav.fo.veilarboppfolging.db;


import lombok.val;
import no.nav.apiapp.feil.Feil;
import no.nav.fo.veilarboppfolging.domain.*;
import no.nav.sbl.jdbc.Database;
import org.springframework.transaction.annotation.Transactional;

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
        OppfolgingTable t = statusRepository.fetch(aktorId);
        if (t == null) {
            return Optional.empty();
        }

        Oppfolging o = new Oppfolging()
                .setAktorId(t.getAktorId())
                .setVeilederId(t.getVeilederId())
                .setUnderOppfolging(t.isUnderOppfolging());

        if (t.getGjeldendeBrukervilkarId() != 0) {
            o.setGjeldendeBrukervilkar(brukervilkarRepository.fetch(t.getGjeldendeBrukervilkarId()));
        }

        if (t.getGjeldendeEskaleringsvarselId() != 0) {
            o.setGjeldendeEskaleringsvarsel(eskaleringsvarselRepository.fetch(t.getGjeldendeEskaleringsvarselId()));
        }

        if (t.getGjeldendeMaalId() != 0) {
            o.setGjeldendeMal(maalRepository.fetch(t.getGjeldendeMaalId()));
        }

        if (t.getGjeldendeManuellStatusId() != 0) {
            o.setGjeldendeManuellStatus(manuellStatusRepository.fetch(t.getGjeldendeManuellStatusId()));
        }

        o.setOppfolgingsperioder(periodeRepository.hentOppfolgingsperioder(t.getAktorId()));

        return Optional.of(o);
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
