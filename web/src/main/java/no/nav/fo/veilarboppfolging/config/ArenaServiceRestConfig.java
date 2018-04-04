package no.nav.fo.veilarboppfolging.config;


import no.nav.fo.veilarboppfolging.mock.OppfoelgingV1Mock;
import no.nav.fo.veilarboppfolging.mock.OppfoelgingsstatusV1Mock;
import no.nav.fo.veilarboppfolging.mock.YtelseskontraktV3Mock;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.binding.OppfoelgingsstatusV1;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.YtelseskontraktV3;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static no.nav.fo.veilarboppfolging.config.ArenaServiceConfig.oppfoelgingPortType;
import static no.nav.fo.veilarboppfolging.config.ArenaServiceConfig.oppfoelgingstatusV1PortType;
import static no.nav.fo.veilarboppfolging.config.ArenaServiceConfig.ytelseskontraktPortType;
import static no.nav.sbl.dialogarena.common.cxf.InstanceSwitcher.createMetricsProxyWithInstanceSwitcher;

@Configuration
public class ArenaServiceRestConfig {
    private static final String ARENASERVICE_YTELSESKONTRAKT_MOCK_KEY = "ytelseskontrakt.arenaservice.withmock";
    private static final String ARENASERVICE_OPPFOELGING_MOCK_KEY = "oppfoelging.arenaservice.withmock";
    private static final String ARENASERVICE_OPPFOELGINGSSTATUS_MOCK_KEY = "oppfoelgingsstatus.arenaservice.withmock";

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

    @Bean
    public OppfoelgingsstatusV1 oppfoelgingsstatusV1() {
        OppfoelgingsstatusV1 prod = oppfoelgingstatusV1PortType()
                .configureStsForOnBehalfOfWithJWT()
                .build();
        OppfoelgingsstatusV1 mock = new OppfoelgingsstatusV1Mock();
        return createMetricsProxyWithInstanceSwitcher("Oppfoelgingsstatus-ArenaService", prod, mock, ARENASERVICE_OPPFOELGING_MOCK_KEY, OppfoelgingsstatusV1.class);
    }

}
