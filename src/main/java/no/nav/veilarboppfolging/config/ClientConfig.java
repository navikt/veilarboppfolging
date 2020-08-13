package no.nav.veilarboppfolging.config;

import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.client.aktorregister.AktorregisterHttpClient;
import no.nav.common.client.aktorregister.CachedAktorregisterClient;
import no.nav.common.client.norg2.CachedNorg2Client;
import no.nav.common.client.norg2.Norg2Client;
import no.nav.common.client.norg2.NorgHttp2Client;
import no.nav.common.cxf.StsConfig;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.veilarboppfolging.client.behandle_arbeidssoker.BehandleArbeidssokerClient;
import no.nav.veilarboppfolging.client.behandle_arbeidssoker.BehandleArbeidssokerClientImpl;
import no.nav.veilarboppfolging.client.dkif.DkifClient;
import no.nav.veilarboppfolging.client.dkif.DkifClientImpl;
import no.nav.veilarboppfolging.client.oppfolging.OppfolgingClient;
import no.nav.veilarboppfolging.client.oppfolging.OppfolgingClientImpl;
import no.nav.veilarboppfolging.client.varseloppgave.VarseloppgaveClient;
import no.nav.veilarboppfolging.client.varseloppgave.VarseloppgaveClientImpl;
import no.nav.veilarboppfolging.client.veilarbaktivitet.VeilarbaktivitetClient;
import no.nav.veilarboppfolging.client.veilarbaktivitet.VeilarbaktivitetClientImpl;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClient;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClientImpl;
import no.nav.veilarboppfolging.client.veilarbportefolje.VeilarbportefoljeClient;
import no.nav.veilarboppfolging.client.veilarbportefolje.VeilarbportefoljeClientImpl;
import no.nav.veilarboppfolging.client.ytelseskontrakt.YtelseskontraktClient;
import no.nav.veilarboppfolging.client.ytelseskontrakt.YtelseskontraktClientImpl;
import no.nav.veilarboppfolging.service.AuthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static no.nav.common.utils.UrlUtils.clusterUrlForApplication;
import static no.nav.veilarboppfolging.config.ApplicationConfig.APPLICATION_NAME;

@Configuration
public class ClientConfig {

    @Bean
    public AktorregisterClient aktorregisterClient(EnvironmentProperties properties, SystemUserTokenProvider tokenProvider) {
        AktorregisterClient aktorregisterClient = new AktorregisterHttpClient(
                properties.getAktorregisterUrl(), APPLICATION_NAME, tokenProvider::getSystemUserToken
        );
        return new CachedAktorregisterClient(aktorregisterClient);
    }

    @Bean
    public Norg2Client norg2Client(EnvironmentProperties properties) {
        return new CachedNorg2Client(new NorgHttp2Client(properties.getNorg2Url()));
    }

    @Bean
    public BehandleArbeidssokerClient behandleArbeidssokerClient(EnvironmentProperties properties, StsConfig stsConfig) {
        return new BehandleArbeidssokerClientImpl(properties.getBehandleArbeidssoekerV1Endpoint(), stsConfig);
    }

    @Bean
    public DkifClient dkifClient() {
        return new DkifClientImpl("http://dkif.default.svc.nais.local");
    }

    @Bean
    public OppfolgingClient oppfolgingClient(EnvironmentProperties properties, StsConfig stsConfig) {
        return new OppfolgingClientImpl(properties.getVirksomhetOppfolgingV1Endpoint(), stsConfig);
    }

    @Bean
    public VarseloppgaveClient varseloppgaveClient(EnvironmentProperties properties, StsConfig stsConfig) {
        return new VarseloppgaveClientImpl(properties.getArbeidsrettetDialogUrl(), properties.getVarselOppgaveV1Endpoint(), stsConfig);
    }

    @Bean
    public VeilarbaktivitetClient veilarbaktivitetClient(SystemUserTokenProvider openAmTokenProvider) {
        String url = clusterUrlForApplication("veilarbaktivitet", true);
        return new VeilarbaktivitetClientImpl(url, openAmTokenProvider);
    }

    @Bean
    public VeilarbarenaClient veilarbarenaClient() {
        String url = clusterUrlForApplication("veilarbarena", true);
        return new VeilarbarenaClientImpl(url, AuthService::getInnloggetBrukerToken);
    }

    @Bean
    public VeilarbportefoljeClient veilarbportefoljeClient(SystemUserTokenProvider openAmTokenProvider) {
        String url = clusterUrlForApplication("veilarbportefolje", true);
        return new VeilarbportefoljeClientImpl(url, openAmTokenProvider);
    }

    @Bean
    public YtelseskontraktClient ytelseskontraktClient(EnvironmentProperties properties, StsConfig stsConfig) {
        return new YtelseskontraktClientImpl(properties.getYtelseskontraktV3Endpoint(), stsConfig);
    }

}
