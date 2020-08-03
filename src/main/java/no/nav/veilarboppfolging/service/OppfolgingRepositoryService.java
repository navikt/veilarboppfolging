package no.nav.veilarboppfolging.service;


import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.veilarboppfolging.domain.*;
import no.nav.veilarboppfolging.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static no.nav.veilarboppfolging.utils.KvpUtils.sjekkTilgangGittKvp;

@Slf4j
@Service
public class OppfolgingRepositoryService {

    // TODO: Merge med OppfolgingService

    private final AuthService authService;
    private final OppfolgingsStatusRepository statusRepository;
    private final OppfolgingsPeriodeRepository periodeRepository;
    private final MaalRepository maalRepository;
    private final ManuellStatusRepository manuellStatusRepository;
    private final EskaleringsvarselRepository eskaleringsvarselRepository;
    private final KvpRepository kvpRepository;
    private final NyeBrukereFeedRepository nyeBrukereFeedRepository;

    @Autowired
    public OppfolgingRepositoryService(
            AuthService authService,
            OppfolgingsStatusRepository statusRepository,
            OppfolgingsPeriodeRepository periodeRepository,
            MaalRepository maalRepository,
            ManuellStatusRepository manuellStatusRepository,
            EskaleringsvarselRepository eskaleringsvarselRepository,
            KvpRepository kvpRepository,
            NyeBrukereFeedRepository nyeBrukereFeedRepository
    ) {
        this.authService = authService;
        this.statusRepository = statusRepository;
        this.periodeRepository = periodeRepository;
        this.maalRepository = maalRepository;
        this.manuellStatusRepository = manuellStatusRepository;
        this.eskaleringsvarselRepository = eskaleringsvarselRepository;
        this.kvpRepository = kvpRepository;
        this.nyeBrukereFeedRepository = nyeBrukereFeedRepository;
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

        Kvp kvp = null;
        if (t.getGjeldendeKvpId() != 0) {
            kvp = kvpRepository.fetch(t.getGjeldendeKvpId());
            if (authService.harTilgangTilEnhet(kvp.getEnhet())) {
                o.setGjeldendeKvp(kvp);
            }
        }

        // Gjeldende eskaleringsvarsel inkluderes i resultatet kun hvis den innloggede veilederen har tilgang til brukers enhet.
        if (t.getGjeldendeEskaleringsvarselId() != 0) {
            EskaleringsvarselData varsel = eskaleringsvarselRepository.fetch(t.getGjeldendeEskaleringsvarselId());
            if (sjekkTilgangGittKvp(authService, kvp, varsel::getOpprettetDato)) {
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
                        .build()
        );
    }

    @Transactional
    public void startOppfolgingHvisIkkeAlleredeStartet(Oppfolgingsbruker oppfolgingsbruker) {
        String aktoerId = oppfolgingsbruker.getAktoerId();

        Oppfolging oppfolgingsstatus = hentOppfolging(aktoerId).orElseGet(() -> {
            // Siden det blir gjort mange kall samtidig til flere noder kan det oppstå en race condition
            // hvor oppfølging har blitt insertet av en annen node etter at den har sjekket at oppfølging
            // ikke ligger i databasen.
            try {
                return statusRepository.opprettOppfolging(aktoerId);
            } catch (DuplicateKeyException e) {
                log.info("Race condition oppstod under oppretting av ny oppfølging for bruker: " + aktoerId);
                return hentOppfolging(aktoerId).orElse(null);
            }
        });

        if (oppfolgingsstatus != null && !oppfolgingsstatus.isUnderOppfolging()) {
            periodeRepository.start(aktoerId);
            nyeBrukereFeedRepository.leggTil(oppfolgingsbruker);
        }
    }

    @Transactional
    public void startEskalering(String aktorId, String opprettetAv, String opprettetBegrunnelse, long tilhorendeDialogId) {
        long gjeldendeEskaleringsvarselId = statusRepository.fetch(aktorId).getGjeldendeEskaleringsvarselId();

        if (gjeldendeEskaleringsvarselId > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Brukeren har allerede et aktivt eskaleringsvarsel.");
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
        long gjeldendeEskaleringsvarselId = statusRepository.fetch(aktorId).getGjeldendeEskaleringsvarselId();

        if(gjeldendeEskaleringsvarselId == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Brukeren har ikke et aktivt eskaleringsvarsel.");
        }

        eskaleringsvarselRepository.finish(aktorId, gjeldendeEskaleringsvarselId, avsluttetAv, avsluttetBegrunnelse);
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
        return authService.harTilgangTilEnhet(kvp.getEnhet())
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

}
