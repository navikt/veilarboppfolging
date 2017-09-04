package no.nav.fo.veilarbsituasjon.config;


import no.nav.fo.veilarbsituasjon.mock.OppfoelgingV1Mock;
import no.nav.fo.veilarbsituasjon.mock.YtelseskontraktV3Mock;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.YtelseskontraktV3;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static no.nav.fo.veilarbsituasjon.config.ArenaServiceConfig.oppfoelgingPortType;
import static no.nav.fo.veilarbsituasjon.config.ArenaServiceConfig.ytelseskontraktPortType;
import static no.nav.sbl.dialogarena.common.cxf.InstanceSwitcher.createMetricsProxyWithInstanceSwitcher;

@Configuration
public class ArenaServiceRestConfig {
    private static final String ARENASERVICE_YTELSESKONTRAKT_MOCK_KEY = "ytelseskontrakt.arenaservice.withmock";
    private static final String ARENASERVICE_OPPFOELGING_MOCK_KEY = "oppfoelging.arenaservice.withmock";

    @Bean
    public YtelseskontraktV3 ytelseskontraktV3() {
        YtelseskontraktV3 prod = ytelseskontraktPortType()
                .configureStsForOnBehalfOfWithJWT()
                .build();
        YtelseskontraktV3 mock = new YtelseskontraktV3Mock();
        return createMetricsProxyWithInstanceSwitcher("Ytelseskontrakt-ArenaService", prod, mock, ARENASERVICE_YTELSESKONTRAKT_MOCK_KEY, YtelseskontraktV3.class);
    }

    @Bean
    public OppfoelgingPortType oppfoelgingV1() {
        OppfoelgingPortType prod = oppfoelgingPortType()
                .configureStsForOnBehalfOfWithJWT()
                .build();
        OppfoelgingPortType mock = new OppfoelgingV1Mock();
        return createMetricsProxyWithInstanceSwitcher("Oppfoelging-ArenaService", prod, mock, ARENASERVICE_OPPFOELGING_MOCK_KEY, OppfoelgingPortType.class);
    }

}
