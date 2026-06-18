package no.nav.veilarboppfolging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.env")
public record EnvironmentProperties(
    String naisAadDiscoveryUrl,
    String naisAadClientId,
    String naisAadIssuer,
    String tokenxClientId,
    String tokenxDiscoveryUrl,
    String norg2Url,
    String aktorregisterUrl,
    String arbeidsrettetDialogUrl,
    String kafkaBrokersUrl,
	String poaoTilgangUrl,
	String poaoTilgangScope,
	String digdirKrrProxyUrl,
	String digdirKrrProxyScope,
    String tiltakshistorikkUrl,
    String tiltakshistorikkScope,
    String oppgaveUrl,
    String oppgaveScope,
    String ungdomsprogramUrl,
    String ungdomsprogramScope,
    String arbeidssoekerregisteretUrl,
    String arbeidssoekerregisteretScope,
    String aapUrl,
    String aapScope
) {}
