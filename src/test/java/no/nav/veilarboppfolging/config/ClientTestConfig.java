package no.nav.veilarboppfolging.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.norg2.Enhet;
import no.nav.common.client.norg2.Norg2Client;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.client.digdir_krr.DigdirClient;
import no.nav.veilarboppfolging.client.digdir_krr.KRRData;
import no.nav.veilarboppfolging.client.veilarbarena.*;
import no.nav.veilarboppfolging.service.SisteEndringPaaOppfolgingBrukerService;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Configuration
public class ClientTestConfig {

    @Bean
    public Norg2Client norg2Client() {
        return new Norg2Client() {
            @Override
            public List<Enhet> alleAktiveEnheter() {
                return null;
            }

            @Override
            public Enhet hentEnhet(String enhetId) {
                return null;
            }

            @Override
            public Enhet hentTilhorendeEnhet(String geografiskOmrade, Diskresjonskode diskresjonskode, boolean b) {
                return null;
            }

            @Override
            public HealthCheckResult checkHealth() {
                return HealthCheckResult.healthy();
            }
        };
    }

    @Bean
    public AktorOppslagClient aktorOppslagClient() {
        return Mockito.mock(AktorOppslagClient.class);
    }

    @Bean
    public SisteEndringPaaOppfolgingBrukerService sisteEndringPaaOppfolgingBrukerService(){
        return Mockito.mock(SisteEndringPaaOppfolgingBrukerService.class);
    }

    @Bean
    public MetricsClient metricsClient() {
        return new MetricsClient() {
            @Override
            public void report(Event event) {
            }

            @Override
            public void report(String name, Map<String, Object> fields, Map<String, String> tags, long l) {
                log.info(String.format("sender event %s Fields: %s Tags: %s", name, fields.toString(), tags.toString()));
            }
        };
    }

	@Bean
    public DigdirClient digdirClient() {
        return new DigdirClient() {
            @Override
            public Optional<KRRData> hentKontaktInfo(Fnr fnr) {
                return Optional.empty();
            }
        };
    }

    @Bean
    public VeilarbarenaClient veilarbarenaClient() {
        return new VeilarbarenaClient() {
            @Override
            public Optional<VeilarbArenaOppfolgingsBruker> hentOppfolgingsbruker(Fnr fnr) {
                return Optional.of(
                        new VeilarbArenaOppfolgingsBruker()
                                .setFodselsnr(fnr.get())
                );
            }

            @Override
            public Optional<VeilarbArenaOppfolgingsStatus> getArenaOppfolgingsstatus(Fnr fnr) {
                return Optional.empty();
            }

            @Override
            public Optional<YtelserDTO> getArenaYtelser(Fnr fnr) {
                return Optional.empty();
            }

            @Override
            public Optional<RegistrerIkkeArbeidsokerRespons> registrerIkkeArbeidsoker(Fnr fnr) {
                return Optional.empty();
            }

            @Override
            public HealthCheckResult checkHealth() {
                return HealthCheckResult.healthy();
            }
        };
    }

}
