package no.nav.veilarboppfolging.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.env")
public class EnvironmentProperties {
    private String naisAadDiscoveryUrl;

    private String naisAadClientId;

    private String naisAadIssuer;

    private String tokenxClientId;

    private String tokenxDiscoveryUrl;

    private String loginserviceIdportenAudience;

    private String loginserviceIdportenDiscoveryUrl;

    private String norg2Url;

    private String aktorregisterUrl;

    private String arbeidsrettetDialogUrl;

    private String kafkaBrokersUrl;

    // SOAP Endpoints

    private String ytelseskontraktV3Endpoint;

    private String varselOppgaveV1Endpoint;

    private String behandleArbeidssoekerV1Endpoint;

	private String poaoTilgangUrl;

	private String poaoTilgangScope;

	private String digdirKrrProxyUrl;

	private String digdirKrrProxyScope;

    private String amtTiltakUrl;

    private String amtTiltakScope;

}
