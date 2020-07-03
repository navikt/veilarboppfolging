package no.nav.veilarboppfolging.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.env")
public class EnvironmentProperties {

    private String openAmDiscoveryUrl;

    private String openAmClientId;

    private String openAmRefreshUrl;

    private String aadDiscoveryUrl;

    private String aadClientId;

    private String aadB2cDiscoveryUrl;

    private String aadB2cClientId;

    private String stsDiscoveryUrl;

    private String abacUrl;

    private String norg2Url;

    private String aktorregisterUrl;

    private String soapStsUrl;

    private String dbUrl;

    private String arbeidsrettetDialogUrl;

    private String kafkaBrokersUrl;

    // SOAP Endpoints

    private String ytelseskontraktV3Endpoint;

    private String virksomhetOppfolgingV1Endpoint;

    private String varselOppgaveV1Endpoint;

    private String behandleArbeidssoekerV1Endpoint;

}
