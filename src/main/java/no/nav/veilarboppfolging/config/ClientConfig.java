package no.nav.veilarboppfolging.config;

import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.aktoroppslag.CachedAktorOppslagClient;
import no.nav.common.client.aktoroppslag.PdlAktorOppslagClient;
import no.nav.common.client.norg2.CachedNorg2Client;
import no.nav.common.client.norg2.Norg2Client;
import no.nav.common.client.norg2.NorgHttp2Client;
import no.nav.common.rest.client.RestClient;
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder;
import no.nav.common.token_client.builder.TokenXTokenClientBuilder;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient;
import no.nav.common.token_client.client.MachineToMachineTokenClient;
import no.nav.common.token_client.client.TokenXOnBehalfOfTokenClient;
import no.nav.common.utils.EnvironmentUtils;
import no.nav.veilarboppfolging.client.amttiltak.AmtTiltakClient;
import no.nav.veilarboppfolging.client.digdir_krr.DigdirClient;
import no.nav.veilarboppfolging.client.digdir_krr.DigdirClientImpl;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClient;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClientImpl;
import no.nav.veilarboppfolging.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static no.nav.common.utils.UrlUtils.createNaisAdeoIngressUrl;
import static no.nav.common.utils.UrlUtils.createNaisPreprodIngressUrl;
import static no.nav.common.utils.UrlUtils.createServiceUrl;

@Configuration
public class ClientConfig {

    @Value("${app.env.veilarbarenaUrl}")
    private String veilarbarenaUrl;
    @Value("${app.env.veilarbarenaAzureScope}")
    private String veilarbarenaAzureScope;

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
    public DigdirClient digdirClient(EnvironmentProperties properties, AzureAdMachineToMachineTokenClient tokenClient, AuthService authService) {
		return new DigdirClientImpl(properties.getDigdirKrrProxyUrl(),
				() -> tokenClient.createMachineToMachineToken(properties.getDigdirKrrProxyScope()),
				() -> authService.getAadOboTokenForTjeneste(properties.getDigdirKrrProxyScope()),
				authService
		);
    }

    @Bean
    public AmtTiltakClient amtTiltakClient(EnvironmentProperties properties, AzureAdMachineToMachineTokenClient tokenClient) {
        return new AmtTiltakClient(properties.getAmtTiltakUrl(),
                () -> tokenClient.createMachineToMachineToken(properties.getAmtTiltakScope()),
                RestClient.baseClient()
        );
    }

    @Bean
    public VeilarbarenaClient veilarbarenaClient(AuthService authService) {
        return new VeilarbarenaClientImpl(veilarbarenaUrl, veilarbarenaAzureScope, authService);
    }

    @Bean
    public AzureAdOnBehalfOfTokenClient azureAdOnBehalfOfTokenClient() {
        return AzureAdTokenClientBuilder.builder()
                .withNaisDefaults()
                .buildOnBehalfOfTokenClient();
    }

    @Bean
    public TokenXOnBehalfOfTokenClient tokenXOnBehalfOfTokenClient() {
        return TokenXTokenClientBuilder.builder()
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
