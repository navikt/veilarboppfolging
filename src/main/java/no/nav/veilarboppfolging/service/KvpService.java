package no.nav.veilarboppfolging.service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolging;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClient;
import no.nav.veilarboppfolging.repository.KvpRepository;
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository;
import no.nav.veilarboppfolging.repository.entity.KvpPeriodeEntity;
import no.nav.veilarboppfolging.repository.entity.OppfolgingEntity;
import no.nav.veilarboppfolging.repository.enums.KodeverkBruker;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;
import java.util.Optional;

import static java.lang.String.format;
import static no.nav.veilarboppfolging.config.ApplicationConfig.SYSTEM_USER_NAME;
import static no.nav.veilarboppfolging.repository.enums.KodeverkBruker.NAV;
import static no.nav.veilarboppfolging.repository.enums.KodeverkBruker.SYSTEM;

@Slf4j
@Service
@RequiredArgsConstructor
public class KvpService {

    private final KafkaProducerService kafkaProducerService;

    private final MetricsService metricsService;

    private final KvpRepository kvpRepository;

    private final VeilarbarenaClient veilarbarenaClient;

    private final OppfolgingsStatusRepository oppfolgingsStatusRepository;

    private final AuthService authService;

    private final TransactionTemplate transactor;

    @SneakyThrows
    public void startKvp(Fnr fnr, String begrunnelse) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);

        authService.sjekkLesetilgangMedAktorId(aktorId);

        Optional<OppfolgingEntity> maybeOppfolging = oppfolgingsStatusRepository.hentOppfolging(aktorId);

        if (maybeOppfolging.isEmpty() || !maybeOppfolging.get().isUnderOppfolging()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        String enhet = veilarbarenaClient.hentOppfolgingsbruker(fnr)
                .map(VeilarbArenaOppfolging::getNav_kontor).orElse(null);

        if (!authService.harTilgangTilEnhet(enhet)) {
            log.warn(format("Ingen tilgang til enhet '%s'", enhet));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        if (maybeOppfolging.get().getGjeldendeKvpId() != 0) {
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

    public Optional<KvpPeriodeEntity> hentGjeldendeKvpPeriode(AktorId aktorId) {
        return kvpRepository.hentGjeldendeKvpPeriode(aktorId);
    }

    public boolean erUnderKvp(AktorId aktorId) {
        return erUnderKvp(kvpRepository.gjeldendeKvp(aktorId));
    }

    // Kan være statisk
    public boolean erUnderKvp(long kvpId) {
        return kvpId != 0L;
    }

    private void stopKvpUtenEnhetSjekk(String avsluttetAv, AktorId aktorId, String begrunnelse, KodeverkBruker kodeverkBruker) {
        Optional<OppfolgingEntity> maybeOppfolging = oppfolgingsStatusRepository.hentOppfolging(aktorId);

        long gjeldendeKvpId = maybeOppfolging.map(OppfolgingEntity::getGjeldendeKvpId).orElse(0L);

        if (gjeldendeKvpId == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        transactor.executeWithoutResult((ignored) -> {
            ZonedDateTime sluttDato = ZonedDateTime.now();

            kvpRepository.stopKvp(gjeldendeKvpId, aktorId, avsluttetAv, begrunnelse, kodeverkBruker, sluttDato);
            kafkaProducerService.publiserKvpAvsluttet(aktorId, avsluttetAv, begrunnelse, sluttDato);

            log.info("KVP avsluttet for bruker med aktorId {} av {}", aktorId, avsluttetAv);
        });

        metricsService.kvpStoppet();
    }

    public void avsluttKvpVedEnhetBytte(EndringPaaOppfoelgingsBrukerV2 endretBruker) {
        AktorId aktorId = authService.getAktorIdOrThrow(Fnr.of(endretBruker.getFodselsnummer()));

        Optional<KvpPeriodeEntity> maybeGjeldendeKvpPeriode = hentGjeldendeKvpPeriode(aktorId);

        if (maybeGjeldendeKvpPeriode.isEmpty()) {
            return;
        }

        boolean harByttetKontor = !endretBruker.getOppfolgingsenhet().equals(maybeGjeldendeKvpPeriode.get().getEnhet());

        if (harByttetKontor) {
            stopKvpUtenEnhetSjekk(SYSTEM_USER_NAME, aktorId, "KVP avsluttet automatisk pga. endret Nav-enhet", SYSTEM);
            metricsService.stopKvpDueToChangedUnit();
        }
    }

}
