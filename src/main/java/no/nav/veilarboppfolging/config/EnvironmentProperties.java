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

    private String norg2Url;

    private String aktorregisterUrl;

    private String arbeidsrettetDialogUrl;

    private String kafkaBrokersUrl;

	private String poaoTilgangUrl;

	private String poaoTilgangScope;

	private String digdirKrrProxyUrl;

	private String digdirKrrProxyScope;

    private String amtDeltakerUrl;

    private String amtDeltakerScope;

}
