package no.nav.veilarboppfolging.service;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV1;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolging;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClient;
import no.nav.veilarboppfolging.domain.KodeverkBruker;
import no.nav.veilarboppfolging.domain.Kvp;
import no.nav.veilarboppfolging.domain.OppfolgingTable;
import no.nav.veilarboppfolging.repository.EskaleringsvarselRepository;
import no.nav.veilarboppfolging.repository.KvpRepository;
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;

import static java.lang.String.format;
import static no.nav.veilarboppfolging.config.ApplicationConfig.SYSTEM_USER_NAME;
import static no.nav.veilarboppfolging.domain.KodeverkBruker.NAV;
import static no.nav.veilarboppfolging.domain.KodeverkBruker.SYSTEM;

@Slf4j
@Service
public class KvpService {

    static final String ESKALERING_AVSLUTTET_FORDI_KVP_BLE_AVSLUTTET = "Eskalering avsluttet fordi KVP ble avsluttet";

    private final KafkaProducerService kafkaProducerService;

    private final MetricsService metricsService;

    private final KvpRepository kvpRepository;

    private final VeilarbarenaClient veilarbarenaClient;

    private final OppfolgingsStatusRepository oppfolgingsStatusRepository;

    private final EskaleringsvarselRepository eskaleringsvarselRepository;

    private final AuthService authService;

    private final TransactionTemplate transactor;

    public KvpService(
            KafkaProducerService kafkaProducerService,
            MetricsService metricsService,
            KvpRepository kvpRepository,
            VeilarbarenaClient veilarbarenaClient,
            OppfolgingsStatusRepository oppfolgingsStatusRepository,
            EskaleringsvarselRepository eskaleringsvarselRepository,
            AuthService authService,
            TransactionTemplate transactor
    ) {
        this.kafkaProducerService = kafkaProducerService;
        this.metricsService = metricsService;
        this.kvpRepository = kvpRepository;
        this.veilarbarenaClient = veilarbarenaClient;
        this.oppfolgingsStatusRepository = oppfolgingsStatusRepository;
        this.eskaleringsvarselRepository = eskaleringsvarselRepository;
        this.authService = authService;
        this.transactor = transactor;
    }

    @SneakyThrows
    public void startKvp(Fnr fnr, String begrunnelse) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);

        authService.sjekkLesetilgangMedAktorId(aktorId);

        OppfolgingTable oppfolgingTable = oppfolgingsStatusRepository.fetch(aktorId);

        if (oppfolgingTable == null || !oppfolgingTable.isUnderOppfolging()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        String enhet = veilarbarenaClient.hentOppfolgingsbruker(fnr)
                .map(VeilarbArenaOppfolging::getNav_kontor).orElse(null);
        if (!authService.harTilgangTilEnhet(enhet)) {
            log.warn(format("Ingen tilgang til enhet '%s'", enhet));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        if (oppfolgingTable.getGjeldendeKvpId() != 0) {
            log.warn(format("Aktøren er allerede under en KVP-periode. AktorId: %s", aktorId));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        String veilederId = authService.getInnloggetVeilederIdent();

        transactor.executeWithoutResult((ignored) -> {
            ZonedDateTime startDato = ZonedDateTime.now();

            kvpRepository.startKvp(aktorId, enhet, veilederId, begrunnelse, startDato);
            kafkaProducerService.publiserKvpStartet(aktorId, enhet, veilederId, begrunnelse, startDato);

            log.info("KVP startet for bruker med aktorId {} på enhet {} av veileder {}", aktorId, enhet, veilederId);
        });

        metricsService.kvpStartet();
    }

    @SneakyThrows
    public void stopKvp(Fnr fnr, String begrunnelse) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);

        authService.sjekkLesetilgangMedAktorId(aktorId);
        String enhet = veilarbarenaClient.hentOppfolgingsbruker(fnr)
                .map(VeilarbArenaOppfolging::getNav_kontor).orElse(null);

        if (!authService.harTilgangTilEnhet(enhet)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        String veilederId = authService.getInnloggetVeilederIdent();
        stopKvpUtenEnhetSjekk(veilederId, aktorId, begrunnelse, NAV);
    }

    public boolean erUnderKvp(AktorId aktorId) {
        return erUnderKvp(kvpRepository.gjeldendeKvp(aktorId));
    }

    // Kan være statisk
    public boolean erUnderKvp(long kvpId) {
        return kvpId != 0L;
    }

    private void stopKvpUtenEnhetSjekk(String avsluttetAv, AktorId aktorId, String begrunnelse, KodeverkBruker kodeverkBruker) {
        OppfolgingTable oppfolgingTable = oppfolgingsStatusRepository.fetch(aktorId);
        long gjeldendeKvpId = oppfolgingTable.getGjeldendeKvpId();

        if (gjeldendeKvpId == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        transactor.executeWithoutResult((ignored) -> {
            ZonedDateTime sluttDato = ZonedDateTime.now();

            if (oppfolgingTable.getGjeldendeEskaleringsvarselId() != 0) {
                eskaleringsvarselRepository.finish(
                        aktorId,
                        oppfolgingTable.getGjeldendeEskaleringsvarselId(),
                        avsluttetAv,
                        ESKALERING_AVSLUTTET_FORDI_KVP_BLE_AVSLUTTET,
                        sluttDato
                );
            }

            kvpRepository.stopKvp(gjeldendeKvpId, aktorId, avsluttetAv, begrunnelse, kodeverkBruker, sluttDato);
            kafkaProducerService.publiserKvpAvsluttet(aktorId, avsluttetAv, begrunnelse, sluttDato);

            log.info("KVP avsluttet for bruker med aktorId {} av {}", aktorId, avsluttetAv);
        });

        metricsService.kvpStoppet();
    }

    public void avsluttKvpVedEnhetBytte(EndringPaaOppfoelgingsBrukerV1 endretBruker) {
        AktorId aktorId = AktorId.of(endretBruker.getAktoerid());
        Kvp gjeldendeKvp = gjeldendeKvp(aktorId);

        if (gjeldendeKvp == null) {
            return;
        }

        boolean harByttetKontor = !endretBruker.getNav_kontor().equals(gjeldendeKvp.getEnhet());

        if (harByttetKontor) {
            stopKvpUtenEnhetSjekk(SYSTEM_USER_NAME, aktorId, "KVP avsluttet automatisk pga. endret Nav-enhet", SYSTEM);
            metricsService.stopKvpDueToChangedUnit();
        }
    }

    Kvp gjeldendeKvp(AktorId aktorId) {
        return kvpRepository.fetch(kvpRepository.gjeldendeKvp(aktorId));
    }

}
