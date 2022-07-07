package no.nav.veilarboppfolging.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.norg2.Enhet;
import no.nav.common.client.norg2.Norg2Client;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.client.behandle_arbeidssoker.BehandleArbeidssokerClient;
import no.nav.veilarboppfolging.client.dkif.DkifClient;
import no.nav.veilarboppfolging.client.dkif.DkifKontaktinfo;
import no.nav.veilarboppfolging.client.veilarbarena.ArenaOppfolging;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolging;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClient;
import no.nav.veilarboppfolging.client.ytelseskontrakt.YtelseskontraktClient;
import no.nav.veilarboppfolging.client.ytelseskontrakt.YtelseskontraktResponse;
import no.nav.veilarboppfolging.controller.request.Innsatsgruppe;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.xml.datatype.XMLGregorianCalendar;
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
    public BehandleArbeidssokerClient behandleArbeidssokerClient() {
        return new BehandleArbeidssokerClient() {
            @Override
            public void opprettBrukerIArena(Fnr fnr, Innsatsgruppe innsatsgruppe) {

            }

            @Override
            public void reaktiverBrukerIArena(Fnr fnr) {

            }

            @Override
            public HealthCheckResult checkHealth() {
                return HealthCheckResult.healthy();
            }
        };
    }

    @Bean
    public DkifClient dkifClient() {
        return new DkifClient() {
            @Override
            public Optional<DkifKontaktinfo> hentKontaktInfo(Fnr fnr) {
                return Optional.empty();
            }

            @Override
            public HealthCheckResult checkHealth() {
                return HealthCheckResult.healthy();
            }
        };
    }

    @Bean
    public VeilarbarenaClient veilarbarenaClient() {
        return new VeilarbarenaClient() {
            @Override
            public Optional<VeilarbArenaOppfolging> hentOppfolgingsbruker(Fnr fnr) {
                return Optional.of(
                        new VeilarbArenaOppfolging()
                                .setFodselsnr(fnr.get())
                );
            }

            @Override
            public Optional<ArenaOppfolging> getArenaOppfolgingsstatus(Fnr fnr) {
                return Optional.empty();
            }

            @Override
            public HealthCheckResult checkHealth() {
                return HealthCheckResult.healthy();
            }
        };
    }

    @Bean
    public YtelseskontraktClient ytelseskontraktClient() {
        return new YtelseskontraktClient() {
            @Override
            public YtelseskontraktResponse hentYtelseskontraktListe(XMLGregorianCalendar periodeFom, XMLGregorianCalendar periodeTom, Fnr personId) {
                return null;
            }

            @Override
            public YtelseskontraktResponse hentYtelseskontraktListe(Fnr personId) {
                return null;
            }

            @Override
            public HealthCheckResult checkHealth() {
                return HealthCheckResult.healthy();
            }
        };
    }

}
