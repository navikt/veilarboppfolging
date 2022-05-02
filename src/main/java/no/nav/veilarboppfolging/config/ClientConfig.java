package no.nav.veilarboppfolging.config;

import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.aktoroppslag.CachedAktorOppslagClient;
import no.nav.common.client.aktoroppslag.PdlAktorOppslagClient;
import no.nav.common.client.norg2.CachedNorg2Client;
import no.nav.common.client.norg2.Norg2Client;
import no.nav.common.client.norg2.NorgHttp2Client;
import no.nav.common.client.pdl.PdlClientImpl;
import no.nav.common.cxf.StsConfig;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder;
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient;
import no.nav.common.utils.EnvironmentUtils;
import no.nav.common.utils.UrlUtils;
import no.nav.veilarboppfolging.client.behandle_arbeidssoker.BehandleArbeidssokerClient;
import no.nav.veilarboppfolging.client.behandle_arbeidssoker.BehandleArbeidssokerClientImpl;
import no.nav.veilarboppfolging.client.dkif.DkifClient;
import no.nav.veilarboppfolging.client.dkif.DkifClientImpl;
import no.nav.veilarboppfolging.client.varseloppgave.VarseloppgaveClient;
import no.nav.veilarboppfolging.client.varseloppgave.VarseloppgaveClientImpl;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClient;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClientImpl;
import no.nav.veilarboppfolging.client.ytelseskontrakt.YtelseskontraktClient;
import no.nav.veilarboppfolging.client.ytelseskontrakt.YtelseskontraktClientImpl;
import no.nav.veilarboppfolging.service.AuthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static no.nav.common.utils.UrlUtils.*;

@Configuration
public class ClientConfig {


    @Bean
    public AktorOppslagClient aktorOppslagClient(SystemUserTokenProvider systemUserTokenProvider) {
        String pdlUrl = isProduction()
                ? createProdInternalIngressUrl("pdl-api")
                : createDevInternalIngressUrl("pdl-api-q1");

        PdlClientImpl pdlClient = new PdlClientImpl(
                pdlUrl,
                systemUserTokenProvider::getSystemUserToken,
                systemUserTokenProvider::getSystemUserToken);

        return new CachedAktorOppslagClient(new PdlAktorOppslagClient(pdlClient));
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
    public DkifClient dkifClient(SystemUserTokenProvider systemUserTokenProvider) {
        String url = UrlUtils.createServiceUrl("dkif", "default", false);
        return new DkifClientImpl(url, systemUserTokenProvider);
    }

    @Bean
    public VarseloppgaveClient varseloppgaveClient(EnvironmentProperties properties, StsConfig stsConfig) {
        return new VarseloppgaveClientImpl(properties.getArbeidsrettetDialogUrl(), properties.getVarselOppgaveV1Endpoint(), stsConfig);
    }


    @Bean
    public VeilarbarenaClient veilarbarenaClient(AuthService authService) {
        String url = naisPreprodOrNaisAdeoIngress("veilarbarena", true);
        return new VeilarbarenaClientImpl(url, authService::getInnloggetBrukerToken, authService::getAadOboTokenForTjeneste);
    }

    @Bean
    public YtelseskontraktClient ytelseskontraktClient(EnvironmentProperties properties, StsConfig stsConfig) {
        return new YtelseskontraktClientImpl(properties.getYtelseskontraktV3Endpoint(), stsConfig);
    }

    @Bean
    public AzureAdOnBehalfOfTokenClient azureAdOnBehalfOfTokenClient() {
        return AzureAdTokenClientBuilder.builder()
                .withNaisDefaults()
                .buildOnBehalfOfTokenClient();
    }

    private static String naisPreprodOrNaisAdeoIngress(String appName, boolean withAppContextPath) {
        return EnvironmentUtils.isProduction().orElse(false)
                ? createNaisAdeoIngressUrl(appName, withAppContextPath)
                : createNaisPreprodIngressUrl(appName, "q1", withAppContextPath);
    }

    private static boolean isProduction() {
        return EnvironmentUtils.isProduction().orElseThrow();
    }

}
