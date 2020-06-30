package no.nav.veilarboppfolging.config;


import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.sbl.dialogarena.types.Pingable.Ping.PingMetadata;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.YtelseskontraktV3;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

import static no.nav.veilarboppfolging.config.ApplicationConfig.*;
import static no.nav.sbl.dialogarena.types.Pingable.Ping.feilet;
import static no.nav.sbl.dialogarena.types.Pingable.Ping.lyktes;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;
import static org.slf4j.LoggerFactory.getLogger;

@Configuration
public class ArenaServiceConfig {
    private static final Logger LOG = getLogger(ArenaServiceConfig.class);

    public static CXFClient<YtelseskontraktV3> ytelseskontraktPortType() {
        final String url = getRequiredProperty(VIRKSOMHET_YTELSESKONTRAKT_V3_PROPERTY);
        return new CXFClient<>(YtelseskontraktV3.class)
                .withOutInterceptor(new LoggingOutInterceptor())
                .address(url);
    }

    public static CXFClient<OppfoelgingPortType> oppfoelgingPortType() {
        final String url = getRequiredProperty(VIRKSOMHET_OPPFOLGING_V1_PROPERTY);
        LOG.info("URL for Oppfoelging_V1 er {}", url);
        return new CXFClient<>(OppfoelgingPortType.class)
                .withOutInterceptor(new LoggingOutInterceptor())
                .address(url);
    }

    @Bean
    Pingable ytelseskontraktPing() {
        final YtelseskontraktV3 ytelseskontraktPing = ytelseskontraktPortType()
                .configureStsForSystemUserInFSS()
                .build();

        PingMetadata metadata = new PingMetadata(UUID.randomUUID().toString(),
                "YTELSESKONTRAKT_V3 via " + getRequiredProperty(VIRKSOMHET_YTELSESKONTRAKT_V3_PROPERTY),
                "Ping av ytelseskontrakt_V3. Henter informasjon om ytelser fra arena.",
                false
        );

        return () -> {
            try {
                ytelseskontraktPing.ping();
                return lyktes(metadata);
            } catch (Exception e) {
                return feilet(metadata, e);
            }
        };
    }

    @Bean
    Pingable oppfoelgingPing() {
        final String url = getRequiredProperty(VIRKSOMHET_OPPFOLGING_V1_PROPERTY);
        LOG.info("URL for Oppfoelging_V1 er {}", url);
        OppfoelgingPortType oppfoelgingPing = oppfoelgingPortType()
                .configureStsForSystemUserInFSS()
                .build();

        PingMetadata metadata = new PingMetadata(UUID.randomUUID().toString(),
                "OPPFOELGING_V1 via " + getRequiredProperty(VIRKSOMHET_OPPFOLGING_V1_PROPERTY),
                "Ping av oppfolging_v1. Henter informasjon om oppfølgingsstatus fra arena.",
                true
        );

        return () -> {
            try {
                oppfoelgingPing.ping();
                return lyktes(metadata);
            } catch (Exception e) {
                return feilet(metadata, e);
            }
        };
    }

}
