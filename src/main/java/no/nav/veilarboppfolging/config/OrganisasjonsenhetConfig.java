package no.nav.veilarboppfolging.config;

import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.sbl.dialogarena.types.Pingable.Ping.PingMetadata;
import no.nav.sbl.util.EnvironmentUtils;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.OrganisasjonEnhetV2;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static no.nav.veilarboppfolging.config.ApplicationConfig.VIRKSOMHET_ORGANISASJONENHET_V2_PROPERTY;
import static no.nav.sbl.dialogarena.types.Pingable.Ping.feilet;
import static no.nav.sbl.dialogarena.types.Pingable.Ping.lyktes;


@Configuration
public class OrganisasjonsenhetConfig {

    public static String getEndpointAddress() {
        return EnvironmentUtils.getRequiredProperty(VIRKSOMHET_ORGANISASJONENHET_V2_PROPERTY);
    }

    public static CXFClient<OrganisasjonEnhetV2> organisasjonEnhetPortType() {
        return new CXFClient<>(OrganisasjonEnhetV2.class)
                .withOutInterceptor(new LoggingOutInterceptor())
                .address(getEndpointAddress());
    }

    @Bean
    public Pingable organisasjonEnhetPing() {
        final OrganisasjonEnhetV2 organisasjonEnhetV1 = organisasjonEnhetPortType()
                .configureStsForSystemUser()
                .build();

        PingMetadata metadata = new PingMetadata(
                "ORGANISASJONSENHET_V2 via " + getEndpointAddress(),
                "Ping av organisasjonsenhet_v2. Henter enheter for veileder.",
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
