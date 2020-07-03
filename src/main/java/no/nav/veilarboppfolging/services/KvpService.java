package no.nav.veilarboppfolging.services;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarboppfolging.client.oppfolging.OppfolgingClient;
import no.nav.veilarboppfolging.domain.KodeverkBruker;
import no.nav.veilarboppfolging.domain.Kvp;
import no.nav.veilarboppfolging.domain.OppfolgingTable;
import no.nav.veilarboppfolging.repository.EskaleringsvarselRepository;
import no.nav.veilarboppfolging.repository.KvpRepository;
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static java.lang.String.format;
import static no.nav.veilarboppfolging.domain.KodeverkBruker.NAV;

@Slf4j
@Service
public class KvpService {

    static final String ESKALERING_AVSLUTTET_FORDI_KVP_BLE_AVSLUTTET = "Eskalering avsluttet fordi KVP ble avsluttet";

    private final MetricsService metricsService;

    private final KvpRepository kvpRepository;

    private final OppfolgingClient oppfolgingClient;

    private final OppfolgingsStatusRepository oppfolgingsStatusRepository;

    private final EskaleringsvarselRepository eskaleringsvarselRepository;

    private final AuthService authService;

    public KvpService(
            MetricsService metricsService,
            KvpRepository kvpRepository,
            OppfolgingClient oppfolgingClient,
            OppfolgingsStatusRepository oppfolgingsStatusRepository,
            EskaleringsvarselRepository eskaleringsvarselRepository,
            AuthService authService
    ) {
        this.metricsService = metricsService;
        this.kvpRepository = kvpRepository;
        this.oppfolgingClient = oppfolgingClient;
        this.oppfolgingsStatusRepository = oppfolgingsStatusRepository;
        this.eskaleringsvarselRepository = eskaleringsvarselRepository;
        this.authService = authService;
    }

    @SneakyThrows
    public void startKvp(String fnr, String begrunnelse) {
        String aktorId = authService.getAktorIdOrThrow(fnr);

        authService.sjekkLesetilgangMedAktorId(aktorId);

        OppfolgingTable oppfolgingTable = oppfolgingsStatusRepository.fetch(aktorId);

        if (oppfolgingTable == null || !oppfolgingTable.isUnderOppfolging()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        String enhet = oppfolgingClient.finnEnhetId(fnr);
        if(!authService.harTilgangTilEnhet(enhet)) {
            log.warn(format("Ingen tilgang til enhet '%s'", enhet));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        if (oppfolgingTable.getGjeldendeKvpId() != 0) {
            log.warn(format("Aktøren er allerede under en KVP-periode. AktorId: %s", aktorId));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        kvpRepository.startKvp(
                aktorId,
                enhet,
                authService.getInnloggetVeilederIdent(),
                begrunnelse);

        metricsService.startKvp();
    }

    @SneakyThrows
    public void stopKvp(String fnr, String begrunnelse) {
        String aktorId = authService.getAktorIdOrThrow(fnr);

        authService.sjekkLesetilgangMedAktorId(aktorId);

        if(!authService.harTilgangTilEnhet(oppfolgingClient.finnEnhetId(fnr))){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        stopKvpUtenEnhetSjekk(aktorId, begrunnelse, NAV);
    }

    public void stopKvpUtenEnhetSjekk(String aktorId, String begrunnelse, KodeverkBruker kodeverkBruker) {
        String veilederId = authService.getInnloggetVeilederIdent();
        OppfolgingTable oppfolgingTable = oppfolgingsStatusRepository.fetch(aktorId);
        long gjeldendeKvp = oppfolgingTable.getGjeldendeKvpId();

        if (gjeldendeKvp == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        if(oppfolgingTable.getGjeldendeEskaleringsvarselId() != 0) {
            eskaleringsvarselRepository.finish(
                    aktorId, 
                    oppfolgingTable.getGjeldendeEskaleringsvarselId(), 
                    veilederId, 
                    ESKALERING_AVSLUTTET_FORDI_KVP_BLE_AVSLUTTET);
        }

        kvpRepository.stopKvp(
                gjeldendeKvp,
                aktorId,
                veilederId,
                begrunnelse,
                kodeverkBruker);

        metricsService.stopKvp();
    }

    Kvp gjeldendeKvp(String aktorId) {
        return kvpRepository.fetch(kvpRepository.gjeldendeKvp(aktorId));
    }

}
