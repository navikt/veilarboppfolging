package no.nav.veilarboppfolging.config;

import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.aktoroppslag.CachedAktorOppslagClient;
import no.nav.common.client.aktoroppslag.PdlAktorOppslagClient;
import no.nav.common.client.norg2.CachedNorg2Client;
import no.nav.common.client.norg2.Norg2Client;
import no.nav.common.client.norg2.NorgHttp2Client;
import no.nav.common.cxf.StsConfig;
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder;
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient;
import no.nav.common.token_client.client.MachineToMachineTokenClient;
import no.nav.common.utils.EnvironmentUtils;
import no.nav.veilarboppfolging.client.behandle_arbeidssoker.BehandleArbeidssokerClient;
import no.nav.veilarboppfolging.client.behandle_arbeidssoker.BehandleArbeidssokerClientImpl;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClient;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClientImpl;
import no.nav.veilarboppfolging.client.ytelseskontrakt.YtelseskontraktClient;
import no.nav.veilarboppfolging.client.ytelseskontrakt.YtelseskontraktClientImpl;
import no.nav.veilarboppfolging.client.digdir_krr.DigdirClient;
import no.nav.veilarboppfolging.client.digdir_krr.DigdirClientImpl;
import no.nav.veilarboppfolging.service.AuthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static no.nav.common.utils.UrlUtils.*;

@Configuration
public class ClientConfig {

    @Bean
    public AktorOppslagClient aktorOppslagClient(MachineToMachineTokenClient tokenClient) {
        String tokenScop = String.format("api://%s-fss.pdl.pdl-api/.default",
                isProduction() ? "prod" : "dev"
        );

        PdlAktorOppslagClient pdlClient = new PdlAktorOppslagClient(
                createServiceUrl("pdl-api", "pdl", false),
                () -> tokenClient.createMachineToMachineToken(tokenScop));

        return new CachedAktorOppslagClient(pdlClient);
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
    public DigdirClient digdirClient(EnvironmentProperties properties) {
        return new DigdirClientImpl(properties.getDigdirKrrProxyUrl());
    }

    @Bean
    public VeilarbarenaClient veilarbarenaClient(AuthService authService) {
        String url = naisPreprodOrNaisAdeoIngress("veilarbarena", true);
        return new VeilarbarenaClientImpl(url, authService::getMachineTokenForTjeneste, authService::getAadOboTokenForTjeneste, authService);
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
