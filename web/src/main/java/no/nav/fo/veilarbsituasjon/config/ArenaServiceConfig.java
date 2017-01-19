package no.nav.fo.veilarbsituasjon.config;


import no.nav.fo.veilarbsituasjon.mock.*;
import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.YtelseskontraktV3;
import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.xml.namespace.QName;

import static java.lang.System.getProperty;
import static no.nav.sbl.dialogarena.common.cxf.InstanceSwitcher.createMetricsProxyWithInstanceSwitcher;
import static org.slf4j.LoggerFactory.getLogger;

@Configuration
public class ArenaServiceConfig {
    private static final String ARENASERVICE_YTELSESKONTRAKT_MOCK_KEY = "ytelseskontrakt.arenaservice.withmock";
    private static final String ARENASERVICE_OPPFOELGING_MOCK_KEY = "oppfoelging.arenaservice.withmock";
    private static final Logger LOG = getLogger(ArenaServiceConfig.class);

    @Bean
    public YtelseskontraktV3 ytelseskontraktV3() {
        YtelseskontraktV3 prod = ytelseskontraktPortType().withOutInterceptor(new TestOutInterceptor()).ikkeMaskerSecurityHeader().build();
        YtelseskontraktV3 mock = new YtelseskontraktV3Mock();
        return createMetricsProxyWithInstanceSwitcher("Ytelseskontrakt-ArenaService", prod, mock, ARENASERVICE_YTELSESKONTRAKT_MOCK_KEY, YtelseskontraktV3.class);
    }

    private CXFClient<YtelseskontraktV3> ytelseskontraktPortType() {
        final String url = getProperty("ytelseskontrakt.endpoint.url");

        return new CXFClient<>(YtelseskontraktV3.class).address(url);
    }

    @Bean
    public OppfoelgingPortType oppfoelgingV1() {
        OppfoelgingPortType prod = oppfoelgingPortType().withOutInterceptor(new TestOutInterceptor()).build();
        OppfoelgingPortType mock = new OppfoelgingV1Mock();
        return createMetricsProxyWithInstanceSwitcher("Oppfoelging-ArenaService", prod, mock, ARENASERVICE_OPPFOELGING_MOCK_KEY, OppfoelgingPortType.class);
    }

    private CXFClient<OppfoelgingPortType> oppfoelgingPortType() {
        final String url = getProperty("oppfoelging.endpoint.url");
        LOG.info("URL for Oppfoelging_V1 er {}", url);
        return new CXFClient<>(OppfoelgingPortType.class)
                .endpointName(new QName("http://nav.no/tjeneste/virksomhet/oppfoelging/v1/", "Oppfoelging_v1"))
                .serviceName(new QName("http://nav.no/tjeneste/virksomhet/oppfoelging/v1/", "Oppfoelging_v1"))
                .address(url);

    }

}
