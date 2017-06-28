package no.nav.fo.veilarbsituasjon.config;

import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.sbl.dialogarena.types.Pingable.Ping.PingMetadata;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v1.OrganisasjonEnhetV1;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.lang.System.getProperty;
import static no.nav.sbl.dialogarena.types.Pingable.Ping.feilet;
import static no.nav.sbl.dialogarena.types.Pingable.Ping.lyktes;


@Configuration
public class OrganisasjonsenhetConfig {
    private static final String ORGANISASJONSENHET_ENDPOINT_KEY = "organisasjonenhet.endpoint.url";


    public static CXFClient<OrganisasjonEnhetV1> organisasjonEnhetPortType() {
        return new CXFClient<>(OrganisasjonEnhetV1.class)
                .withOutInterceptor(new LoggingOutInterceptor())
                .address(getProperty(ORGANISASJONSENHET_ENDPOINT_KEY));
    }

    @Bean
    public Pingable organisasjonEnhetPing() {
        final OrganisasjonEnhetV1 organisasjonEnhetV1 = organisasjonEnhetPortType()
                .configureStsForSystemUser()
                .build();

        PingMetadata metadata = new PingMetadata(
                "ORGANISASJONSENHET_V1 via " + ORGANISASJONSENHET_ENDPOINT_KEY,
                "Ping av organisasjonsenhet_v1. Henter enheter for veileder.",
                false
        );

        return () -> {
            try {
                organisasjonEnhetV1.ping();
                return lyktes(metadata);
            } catch (Exception e) {
                return feilet(metadata, e);
            }
        };
    }
}
