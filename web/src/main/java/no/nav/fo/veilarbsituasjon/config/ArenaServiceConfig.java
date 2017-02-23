package no.nav.fo.veilarbsituasjon.config;


import no.nav.fo.veilarbsituasjon.mock.OppfoelgingV1Mock;
import no.nav.fo.veilarbsituasjon.mock.YtelseskontraktV3Mock;
import no.nav.modig.security.ws.SystemSAMLOutInterceptor;
import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.YtelseskontraktV3;
import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.lang.System.getProperty;
import static no.nav.sbl.dialogarena.common.cxf.InstanceSwitcher.createMetricsProxyWithInstanceSwitcher;
import static no.nav.sbl.dialogarena.types.Pingable.Ping.feilet;
import static no.nav.sbl.dialogarena.types.Pingable.Ping.lyktes;
import static org.slf4j.LoggerFactory.getLogger;

@Configuration
public class ArenaServiceConfig {
    private static final String ARENASERVICE_YTELSESKONTRAKT_MOCK_KEY = "ytelseskontrakt.arenaservice.withmock";
    private static final String ARENASERVICE_OPPFOELGING_MOCK_KEY = "oppfoelging.arenaservice.withmock";
    private static final Logger LOG = getLogger(ArenaServiceConfig.class);

    @Bean
    public YtelseskontraktV3 ytelseskontraktV3() {
        YtelseskontraktV3 prod = ytelseskontraktPortType()
                .configureStsForOnBehalfOfWithJWT()
                .build();
        YtelseskontraktV3 mock = new YtelseskontraktV3Mock();
        return createMetricsProxyWithInstanceSwitcher("Ytelseskontrakt-ArenaService", prod, mock, ARENASERVICE_YTELSESKONTRAKT_MOCK_KEY, YtelseskontraktV3.class);
    }

    private CXFClient<YtelseskontraktV3> ytelseskontraktPortType() {
        final String url = getProperty("ytelseskontrakt.endpoint.url");
        return new CXFClient<>(YtelseskontraktV3.class)
                .address(url);
    }

    @Bean
    public OppfoelgingPortType oppfoelgingV1() {
        OppfoelgingPortType prod = oppfoelgingPortType()
                .configureStsForOnBehalfOfWithJWT()
                .build();
        OppfoelgingPortType mock = new OppfoelgingV1Mock();
        return createMetricsProxyWithInstanceSwitcher("Oppfoelging-ArenaService", prod, mock, ARENASERVICE_OPPFOELGING_MOCK_KEY, OppfoelgingPortType.class);
    }

    private CXFClient<OppfoelgingPortType> oppfoelgingPortType() {
        final String url = getProperty("oppfoelging.endpoint.url");
        LOG.info("URL for Oppfoelging_V1 er {}", url);
        return new CXFClient<>(OppfoelgingPortType.class)
                .address(url);
    }

    @Bean
    Pingable ytelseskontraktPing() {
        final YtelseskontraktV3 ytelseskontraktPing = ytelseskontraktPortType()
                .withOutInterceptor(new SystemSAMLOutInterceptor())
                .build();
        return () -> {
            try {
                ytelseskontraktPing.ping();
                return lyktes("YTELSESKONTRAKT_V3");
            } catch (Exception e) {
                return feilet("YTELSESKONTRAKT_V3", e);
            }
        };
    }

    @Bean
    Pingable oppfoelgingPing() {
        final String url = getProperty("oppfoelging.endpoint.url");
        LOG.info("URL for Oppfoelging_V1 er {}", url);
        OppfoelgingPortType oppfoelgingPing = oppfoelgingPortType()
                .withOutInterceptor(new SystemSAMLOutInterceptor())
                .build();

        return () -> {
            try {
                oppfoelgingPing.ping();
                return lyktes("OPPFOELGING_V1");
            } catch (Exception e) {
                return feilet("OPPFOELGING_V1", e);
            }
        };
    }
}
