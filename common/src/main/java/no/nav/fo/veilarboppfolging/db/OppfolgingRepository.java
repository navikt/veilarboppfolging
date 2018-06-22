package no.nav.fo.veilarboppfolging.db;


import lombok.SneakyThrows;
import lombok.val;
import no.nav.apiapp.feil.Feil;
import no.nav.apiapp.security.PepClient;
import no.nav.fo.veilarboppfolging.domain.*;
import no.nav.sbl.jdbc.Database;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static no.nav.apiapp.feil.FeilType.UGYLDIG_HANDLING;
import static no.nav.fo.veilarboppfolging.utils.KvpUtils.sjekkTilgangGittKvp;

public class OppfolgingRepository {

    @Inject
    private PepClient pepClient;

    private final OppfolgingsStatusRepository statusRepository;
    private final OppfolgingsPeriodeRepository periodeRepository;
    private final MaalRepository maalRepository;
    private final ManuellStatusRepository manuellStatusRepository;
    private final BrukervilkarRepository brukervilkarRepository;
    private final EskaleringsvarselRepository eskaleringsvarselRepository;
    private final KvpRepository kvpRepository;
    private final NyeBrukereFeedRepository nyeBrukereFeedRepository;

    public OppfolgingRepository(Database database) {
        statusRepository = new OppfolgingsStatusRepository(database);
        periodeRepository = new OppfolgingsPeriodeRepository(database);
        maalRepository = new MaalRepository(database);
        manuellStatusRepository = new ManuellStatusRepository(database);
        brukervilkarRepository = new BrukervilkarRepository(database);
        eskaleringsvarselRepository = new EskaleringsvarselRepository(database);
        kvpRepository = new KvpRepository(database);
        nyeBrukereFeedRepository = new NyeBrukereFeedRepository(database);
    }

    @SneakyThrows
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

        Kvp kvp = null;
        if (t.getGjeldendeKvpId() != 0) {
            kvp = kvpRepository.fetch(t.getGjeldendeKvpId());
            if (pepClient.harTilgangTilEnhet(kvp.getEnhet())) {
                o.setGjeldendeKvp(kvp);
            }
        }

        // Gjeldende eskaleringsvarsel inkluderes i resultatet kun hvis den innloggede veilederen har tilgang til brukers enhet.
        if (t.getGjeldendeEskaleringsvarselId() != 0) {
            EskaleringsvarselData varsel = eskaleringsvarselRepository.fetch(t.getGjeldendeEskaleringsvarselId());
            if (sjekkTilgangGittKvp(pepClient, kvp, varsel::getOpprettetDato)) {
                o.setGjeldendeEskaleringsvarsel(varsel);
            }
        }

        if (t.getGjeldendeMaalId() != 0) {
            o.setGjeldendeMal(maalRepository.fetch(t.getGjeldendeMaalId()));
        }

        if (t.getGjeldendeManuellStatusId() != 0) {
            o.setGjeldendeManuellStatus(manuellStatusRepository.fetch(t.getGjeldendeManuellStatusId()));
        }

        List<Kvp> kvpPerioder = kvpRepository.hentKvpHistorikk(aktorId);
        o.setOppfolgingsperioder(populerKvpPerioder(periodeRepository.hentOppfolgingsperioder(t.getAktorId()), kvpPerioder));

        return Optional.of(o);
    }

    @Transactional
    public void startOppfolgingHvisIkkeAlleredeStartet(String aktorId) {
        startOppfolgingHvisIkkeAlleredeStartet(
                Oppfolgingsbruker.builder()
                        .aktoerId(aktorId)
                        .selvgaende(false)
                .build()
        );
    }

    @Transactional
    public void startOppfolgingHvisIkkeAlleredeStartet(Oppfolgingsbruker oppfolgingsbruker) {
        String aktoerId = oppfolgingsbruker.getAktoerId();
        Oppfolging oppfolgingsstatus = hentOppfolging(aktoerId).orElseGet(() -> opprettOppfolging(aktoerId));
        if (!oppfolgingsstatus.isUnderOppfolging()) {
            periodeRepository.start(aktoerId);
            nyeBrukereFeedRepository.leggTil(oppfolgingsbruker);
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

    private List<Oppfolgingsperiode> populerKvpPerioder(List<Oppfolgingsperiode> oppfolgingsPerioder, List<Kvp> kvpPerioder) {
        return oppfolgingsPerioder.stream()
                .map(periode -> periode.toBuilder().kvpPerioder(
                        kvpPerioder.stream()
                                .filter(kvp -> erKvpIPeriode(kvp, periode))
                                .collect(toList())
                ).build())
                .collect(toList());
    }

    private boolean erKvpIPeriode(Kvp kvp, Oppfolgingsperiode periode) {
        return harTilgangTilEnhet(kvp)
                && kvpEtterStartenAvPeriode(kvp, periode)
                && kvpForSluttenAvPeriode(kvp, periode);
    }

    private boolean kvpEtterStartenAvPeriode(Kvp kvp, Oppfolgingsperiode periode) {
        return !periode.getStartDato().after(kvp.getOpprettetDato());
    }

    private boolean kvpForSluttenAvPeriode(Kvp kvp, Oppfolgingsperiode periode) {
        return periode.getSluttDato() == null
                || !periode.getSluttDato().before(kvp.getOpprettetDato());
    }

    @SneakyThrows
    private boolean harTilgangTilEnhet(Kvp kvp) {
        return pepClient.harTilgangTilEnhet(kvp.getEnhet());
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
