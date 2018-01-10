package no.nav.fo.veilarboppfolging.db;


import lombok.val;
import no.nav.apiapp.feil.Feil;
import no.nav.fo.veilarboppfolging.domain.*;
import no.nav.fo.veilarboppfolging.services.EnhetPepClient;
import no.nav.sbl.jdbc.Database;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static no.nav.apiapp.feil.Feil.Type.UGYLDIG_HANDLING;

public class OppfolgingRepository {

    @Inject
    private EnhetPepClient enhetPepClient;

    private final OppfolgingsStatusRepository statusRepository;
    private final OppfolgingsPeriodeRepository periodeRepository;
    private final MaalRepository maalRepository;
    private final ManuellStatusRepository manuellStatusRepository;
    private final BrukervilkarRepository brukervilkarRepository;
    private final EskaleringsvarselRepository eskaleringsvarselRepository;
    private final KvpRepository kvpRepository;

    public OppfolgingRepository(Database database) {
        statusRepository = new OppfolgingsStatusRepository(database);
        periodeRepository = new OppfolgingsPeriodeRepository(database);
        maalRepository = new MaalRepository(database);
        manuellStatusRepository = new ManuellStatusRepository(database);
        brukervilkarRepository = new BrukervilkarRepository(database);
        eskaleringsvarselRepository = new EskaleringsvarselRepository(database);
        kvpRepository = new KvpRepository(database);
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

        if (t.getGjeldendeKvpId() != 0) {
            Kvp kvp = kvpRepository.fetch(t.getGjeldendeKvpId());
            if (enhetPepClient.harTilgang(kvp.getEnhet())) {
                o.setGjeldendeKvp(kvp);
            }
        }

        o.setOppfolgingsperioder(periodeRepository.hentOppfolgingsperioder(t.getAktorId()));

        return Optional.of(o);
    }

    @Transactional
    public void startOppfolgingHvisIkkeAlleredeStartet(String aktorId) {
        if (!statusRepository.erOppfolgingsflaggSattForBruker(aktorId)) {
            periodeRepository.start(aktorId);
        }
    }

    @Transactional
    public void startEskalering(String aktorId, String opprettetAv, String opprettetBegrunnelse, long tilhorendeDialogId) {
        if (eskaleringsvarselRepository.fetchByAktorId(aktorId) != null) {
            throw new Feil(UGYLDIG_HANDLING, "Brukeren har allerede et aktivt eskaleringsvarsel.");
        }

        val e = EskaleringsvarselData.builder()
                .aktorId(aktorId)
                .opprettetAv(opprettetAv)
                .opprettetBegrunnelse(opprettetBegrunnelse)
                .tilhorendeDialogId(tilhorendeDialogId)
                .build();
        eskaleringsvarselRepository.create(e);
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
    }

    // FIXME: go directly to the repository instead.
    public void avsluttOppfolging(String aktorId, String veileder, String begrunnelse) {
        periodeRepository.avslutt(aktorId, veileder, begrunnelse);
    }

    // FIXME: OPPFOLGINGSSTATUS table should have a foreign key constraint on GJELDENDE_MANUELL_STATUS
    // FIXME: go directly to the repository instead.
    public void opprettManuellStatus(ManuellStatus manuellStatus) {
        manuellStatusRepository.create(manuellStatus);
    }

    // FIXME: go directly to the repository instead.
    public void opprettBrukervilkar(Brukervilkar brukervilkar) {
        brukervilkarRepository.create(brukervilkar);
    }

    // FIXME: go directly to the repository instead.
    public Oppfolging opprettOppfolging(String aktorId) {
        return statusRepository.create(aktorId);
    }

    // FIXME: go directly to the repository instead.
    public List<Brukervilkar> hentHistoriskeVilkar(String aktorId) {
        return brukervilkarRepository.history(aktorId);
    }

    // FIXME: go directly to the repository instead.
    public List<AvsluttetOppfolgingFeedData> hentAvsluttetOppfolgingEtterDato(Timestamp timestamp, int pageSize) {
        return periodeRepository.fetchAvsluttetEtterDato(timestamp, pageSize);
    }

    // FIXME: go directly to the repository instead.
    public List<ManuellStatus> hentManuellHistorikk(String aktorId) {
        return manuellStatusRepository.history(aktorId);
    }

    // FIXME: go directly to the repository instead.
    public List<Oppfolgingsperiode> hentAvsluttetOppfolgingsperioder(String aktorId) {
        return periodeRepository.hentAvsluttetOppfolgingsperioder(aktorId);
    }

    // FIXME: go directly to the repository instead.
    public List<EskaleringsvarselData> hentEskaleringhistorikk(String aktorId) {
        return eskaleringsvarselRepository.history(aktorId);
    }

    // FIXME: go directly to maalRepository instead.
    public List<MalData> hentMalList(String aktorId) {
        return maalRepository.aktorMal(aktorId);
    }

    // FIXME: OPPFOLGINGSSTATUS table should have a foreign key constraint on GJELDENDE_MAL
    // FIXME: go directly to maalRepository instead.
    public void opprettMal(MalData mal) {
        maalRepository.opprett(mal);
    }

    // FIXME: go directly to maalRepository instead.
    public void slettMalForAktorEtter(String aktorId, Date date) {
        maalRepository.slettForAktorEtter(aktorId, date);
    }
}
