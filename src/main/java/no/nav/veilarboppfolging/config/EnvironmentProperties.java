package no.nav.veilarboppfolging.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.env")
public class EnvironmentProperties {

    private String openAmDiscoveryUrl;

    private String veilarbloginOpenAmClientId;

    private String modialoginOpenAmClientId;

    private String openAmRefreshUrl;

    private String openAmRedirectUrl;

    private String openAmIssoRpUsername;

    private String openAmIssoRpPassword;

    private String naisAadDiscoveryUrl;

    private String naisAadClientId;

    private String naisAadIssuer;


    private String tokenxClientId;

    private String tokenxDiscoveryUrl;


    private String loginserviceIdportenAudience;

    private String loginserviceIdportenDiscoveryUrl;


    private String naisStsDiscoveryUrl;

    private String naisStsClientId;


    private String abacUrl;

    private String norg2Url;

    private String aktorregisterUrl;

    private String soapStsUrl;

    private String arbeidsrettetDialogUrl;

    private String kafkaBrokersUrl;

    private String unleashUrl;

    // SOAP Endpoints

    private String ytelseskontraktV3Endpoint;

    private String varselOppgaveV1Endpoint;

    private String behandleArbeidssoekerV1Endpoint;

}
