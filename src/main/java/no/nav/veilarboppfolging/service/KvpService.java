package no.nav.veilarboppfolging.service;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarboppfolging.client.oppfolging.OppfolgingClient;
import no.nav.veilarboppfolging.domain.KodeverkBruker;
import no.nav.veilarboppfolging.domain.Kvp;
import no.nav.veilarboppfolging.domain.OppfolgingTable;
import no.nav.veilarboppfolging.domain.kafka.VeilarbArenaOppfolgingEndret;
import no.nav.veilarboppfolging.repository.EskaleringsvarselRepository;
import no.nav.veilarboppfolging.repository.KvpRepository;
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static java.lang.String.format;
import static no.nav.veilarboppfolging.domain.KodeverkBruker.NAV;
import static no.nav.veilarboppfolging.domain.KodeverkBruker.SYSTEM;

@Slf4j
@Service
public class KvpService {

    static final String ESKALERING_AVSLUTTET_FORDI_KVP_BLE_AVSLUTTET = "Eskalering avsluttet fordi KVP ble avsluttet";

    private final KafkaProducerService kafkaProducerService;

    private final MetricsService metricsService;

    private final KvpRepository kvpRepository;

    private final OppfolgingClient oppfolgingClient;

    private final OppfolgingsStatusRepository oppfolgingsStatusRepository;

    private final EskaleringsvarselRepository eskaleringsvarselRepository;

    private final AuthService authService;

    public KvpService(
            KafkaProducerService kafkaProducerService,
            MetricsService metricsService,
            KvpRepository kvpRepository,
            OppfolgingClient oppfolgingClient,
            OppfolgingsStatusRepository oppfolgingsStatusRepository,
            EskaleringsvarselRepository eskaleringsvarselRepository,
            AuthService authService
    ) {
        this.kafkaProducerService = kafkaProducerService;
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
        if (!authService.harTilgangTilEnhet(enhet)) {
            log.warn(format("Ingen tilgang til enhet '%s'", enhet));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        if (oppfolgingTable.getGjeldendeKvpId() != 0) {
            log.warn(format("Aktøren er allerede under en KVP-periode. AktorId: %s", aktorId));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        String veilederId = authService.getInnloggetVeilederIdent();

        kvpRepository.startKvp(aktorId, enhet, veilederId, begrunnelse);

        log.info("KVP startet for bruker med aktorId {} på enhet {} av veileder {}", aktorId, enhet, veilederId);

        kafkaProducerService.publiserKvpStartet(aktorId, enhet, veilederId, begrunnelse);
        metricsService.kvpStartet();
    }

    @SneakyThrows
    public void stopKvp(String fnr, String begrunnelse) {
        String aktorId = authService.getAktorIdOrThrow(fnr);

        authService.sjekkLesetilgangMedAktorId(aktorId);

        if (!authService.harTilgangTilEnhet(oppfolgingClient.finnEnhetId(fnr))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        stopKvpUtenEnhetSjekk(aktorId, begrunnelse, NAV);
    }

    public boolean erUnderKvp(String aktorId) {
        return erUnderKvp(kvpRepository.gjeldendeKvp(aktorId));
    }

    // Kan være statisk
    public boolean erUnderKvp(long kvpId) {
        return kvpId != 0L;
    }

    public void stopKvpUtenEnhetSjekk(String aktorId, String begrunnelse, KodeverkBruker kodeverkBruker) {
        String veilederId = authService.getInnloggetVeilederIdent();
        OppfolgingTable oppfolgingTable = oppfolgingsStatusRepository.fetch(aktorId);
        long gjeldendeKvpId = oppfolgingTable.getGjeldendeKvpId();

        if (gjeldendeKvpId == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        if (oppfolgingTable.getGjeldendeEskaleringsvarselId() != 0) {
            eskaleringsvarselRepository.finish(
                    aktorId, 
                    oppfolgingTable.getGjeldendeEskaleringsvarselId(), 
                    veilederId, 
                    ESKALERING_AVSLUTTET_FORDI_KVP_BLE_AVSLUTTET);
        }

        // TODO: Ikke sikkert at vi trenger å hente gjeldende KVP for å sende med enhetId på Kafka
        Kvp gjeldendeKvp = kvpRepository.fetch(gjeldendeKvpId);

        kvpRepository.stopKvp(gjeldendeKvpId, aktorId, veilederId, begrunnelse, kodeverkBruker);

        log.info("KVP avsluttet for bruker med aktorId {} på enhet {} av veileder {}", aktorId, gjeldendeKvp.getEnhet(), veilederId);

        kafkaProducerService.publiserKvpAvsluttet(aktorId, gjeldendeKvp.getEnhet(), veilederId, begrunnelse);
        metricsService.kvpStoppet();
    }

    public void avsluttKvpVedEnhetBytte(VeilarbArenaOppfolgingEndret endretBruker) {
        Kvp gjeldendeKvp = gjeldendeKvp(endretBruker.aktoerid);

        if (gjeldendeKvp == null) {
            return;
        }

        boolean harByttetKontor = !endretBruker.getNav_kontor().equals(gjeldendeKvp.getEnhet());

        if (harByttetKontor) {
            stopKvpUtenEnhetSjekk(endretBruker.aktoerid, "KVP avsluttet automatisk pga. endret Nav-enhet", SYSTEM);
            metricsService.stopKvpDueToChangedUnit();
        }
    }

    Kvp gjeldendeKvp(String aktorId) {
        return kvpRepository.fetch(kvpRepository.gjeldendeKvp(aktorId));
    }

}
