package no.nav.veilarboppfolging.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.client.aktorregister.IdentOppslag;
import no.nav.common.client.norg2.Enhet;
import no.nav.common.client.norg2.Norg2Client;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import no.nav.veilarboppfolging.client.behandle_arbeidssoker.BehandleArbeidssokerClient;
import no.nav.veilarboppfolging.client.dkif.DkifClient;
import no.nav.veilarboppfolging.client.dkif.DkifKontaktinfo;
import no.nav.veilarboppfolging.client.oppfolging.OppfolgingClient;
import no.nav.veilarboppfolging.client.oppfolging.OppfolgingskontraktData;
import no.nav.veilarboppfolging.client.varseloppgave.VarseloppgaveClient;
import no.nav.veilarboppfolging.client.veilarbaktivitet.ArenaAktivitetDTO;
import no.nav.veilarboppfolging.client.veilarbaktivitet.VeilarbaktivitetClient;
import no.nav.veilarboppfolging.client.veilarbarena.ArenaOppfolging;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolging;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClient;
import no.nav.veilarboppfolging.client.veilarbportefolje.OppfolgingEnhetPageDTO;
import no.nav.veilarboppfolging.client.veilarbportefolje.VeilarbportefoljeClient;
import no.nav.veilarboppfolging.client.ytelseskontrakt.YtelseskontraktClient;
import no.nav.veilarboppfolging.client.ytelseskontrakt.YtelseskontraktResponse;
import no.nav.veilarboppfolging.domain.Fnr;
import no.nav.veilarboppfolging.domain.Innsatsgruppe;
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
            public Enhet hentTilhorendeEnhet(String geografiskOmrade) {
                return null;
            }

            @Override
            public HealthCheckResult checkHealth() {
                return HealthCheckResult.healthy();
            }
        };
    }

    @Bean
    public AktorregisterClient aktorregisterClient() {
        return new AktorregisterClient() {
            @Override
            public String hentFnr(String aktorId) {
                return null;
            }

            @Override
            public String hentAktorId(String fnr) {
                return null;
            }

            @Override
            public List<IdentOppslag> hentFnr(List<String> aktorIdListe) {
                return null;
            }

            @Override
            public List<IdentOppslag> hentAktorId(List<String> fnrListe) {
                return null;
            }

            @Override
            public HealthCheckResult checkHealth() {
                return HealthCheckResult.healthy();
            }
        };
    }

    @Bean
    public MetricsClient metricsClient() {
        return new MetricsClient() {
            @Override
            public void report(Event event) {}

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
            public void opprettBrukerIArena(String fnr, Innsatsgruppe innsatsgruppe) {

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
            public DkifKontaktinfo hentKontaktInfo(String fnr) {
                return null;
            }

            @Override
            public HealthCheckResult checkHealth() {
                return HealthCheckResult.healthy();
            }
        };
    }

    @Bean
    public OppfolgingClient oppfolgingClient() {
        return new OppfolgingClient() {
            @Override
            public List<OppfolgingskontraktData> hentOppfolgingskontraktListe(XMLGregorianCalendar fom, XMLGregorianCalendar tom, String fnr) {
                return null;
            }

            @Override
            public String finnEnhetId(String fnr) {
                return null;
            }

            @Override
            public HealthCheckResult checkHealth() {
                return HealthCheckResult.healthy();
            }
        };
    }

    @Bean
    public VarseloppgaveClient varseloppgaveClient() {
        return new VarseloppgaveClient() {
            @Override
            public void sendEskaleringsvarsel(String aktorId, long dialogId) {

            }

            @Override
            public HealthCheckResult checkHealth() {
                return HealthCheckResult.healthy();
            }
        };
    }

    @Bean
    public VeilarbaktivitetClient veilarbaktivitetClient() {
        return new VeilarbaktivitetClient() {
            @Override
            public List<ArenaAktivitetDTO> hentArenaAktiviteter(String fnr) {
                return null;
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
            public Optional<VeilarbArenaOppfolging> hentOppfolgingsbruker(String fnr) {
                return Optional.empty();
            }

            @Override
            public Optional<ArenaOppfolging> getArenaOppfolgingsstatus(String fnr) {
                return Optional.empty();
            }

            @Override
            public HealthCheckResult checkHealth() {
                return HealthCheckResult.healthy();
            }
        };
    }

    @Bean
    public VeilarbportefoljeClient veilarbportefoljeClient() {
        return new VeilarbportefoljeClient() {
            @Override
            public OppfolgingEnhetPageDTO hentEnhetPage(int pageNumber, int pageSize) {
                return null;
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
            public YtelseskontraktResponse hentYtelseskontraktListe(XMLGregorianCalendar periodeFom, XMLGregorianCalendar periodeTom, String personId) {
                return null;
            }

            @Override
            public YtelseskontraktResponse hentYtelseskontraktListe(String personId) {
                return null;
            }

            @Override
            public HealthCheckResult checkHealth() {
                return HealthCheckResult.healthy();
            }
        };
    }

}
