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
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient;
import no.nav.common.token_client.client.TokenXOnBehalfOfTokenClient;
import no.nav.veilarboppfolging.client.digdir_krr.DigdirClient;
import no.nav.veilarboppfolging.client.digdir_krr.DigdirClientImpl;
import no.nav.veilarboppfolging.client.tiltakshistorikk.TiltakshistorikkClient;
import no.nav.veilarboppfolging.client.ungdomsprogram.UngdomsprogramClient;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClient;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClientImpl;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.tokenClient.ErrorMappedAzureAdMachineToMachineTokenClient;
import no.nav.veilarboppfolging.tokenClient.ErrorMappedAzureAdOnBehalfOfTokenClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.function.Supplier;

@Profile("!test")
@Configuration
public class ClientConfig {

    @Value("${app.env.veilarbarenaUrl}")
    private String veilarbarenaUrl;
    @Value("${app.env.veilarbarenaAzureScope}")
    private String veilarbarenaAzureScope;
    @Value("${app.env.pdlUrl}")
    private String pdlUrl;
    @Value("${app.env.pdlScope}")
    private String pdlScope;

    @Bean
    public AktorOppslagClient aktorOppslagClient(ErrorMappedAzureAdMachineToMachineTokenClient tokenClient) {
        PdlAktorOppslagClient pdlClient = new PdlAktorOppslagClient(
                pdlUrl,
                () -> tokenClient.createMachineToMachineToken(pdlScope));
        return new CachedAktorOppslagClient(pdlClient);
    }

    @Bean
    public Norg2Client norg2Client(EnvironmentProperties properties) {
        return new CachedNorg2Client(new NorgHttp2Client(properties.getNorg2Url()));
    }

    @Bean
    public DigdirClient digdirClient(EnvironmentProperties properties, ErrorMappedAzureAdMachineToMachineTokenClient tokenClient, AuthService authService) {
		return new DigdirClientImpl(properties.getDigdirKrrProxyUrl(),
				() -> tokenClient.createMachineToMachineToken(properties.getDigdirKrrProxyScope()),
				() -> authService.getAadOboTokenForTjeneste(properties.getDigdirKrrProxyScope()),
				authService
		);
    }

    @Bean
    public TiltakshistorikkClient tiltakshistorikkClient(EnvironmentProperties properties, ErrorMappedAzureAdMachineToMachineTokenClient tokenClient) {
        return new TiltakshistorikkClient(properties.getTiltakshistorikkUrl(),
                () -> tokenClient.createMachineToMachineToken(properties.getTiltakshistorikkScope()),
                RestClient.baseClient()
        );
    }

    @Bean
    public UngdomsprogramClient ungdomsprogramClient(EnvironmentProperties properties, ErrorMappedAzureAdMachineToMachineTokenClient tokenClient, AuthService authService) {
        Supplier<String> tokenSupplier = () -> authService.erInternBruker() ? authService.getAadOboTokenForTjeneste(properties.getUngdomsprogramScope())
                : tokenClient.createMachineToMachineToken(properties.getUngdomsprogramScope());

        return new UngdomsprogramClient(properties.getUngdomsprogramUrl(),
                tokenSupplier,
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
    public ErrorMappedAzureAdOnBehalfOfTokenClient errorMappedAzureAdOnBehalfOfTokenClient() {
        return new ErrorMappedAzureAdOnBehalfOfTokenClient();
    }

    @Bean
    public TokenXOnBehalfOfTokenClient tokenXOnBehalfOfTokenClient() {
        return TokenXTokenClientBuilder.builder()
                .withNaisDefaults()
                .buildOnBehalfOfTokenClient();
    }
}
