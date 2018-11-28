package no.nav.fo.veilarboppfolging.config;

import no.nav.fo.veilarboppfolging.mock.OppfoelgingV1Mock;
import no.nav.fo.veilarboppfolging.mock.OppfoelgingsstatusV2Mock;
import no.nav.fo.veilarboppfolging.mock.YtelseskontraktV3Mock;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v2.binding.OppfoelgingsstatusV2;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.YtelseskontraktV3;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static no.nav.fo.veilarboppfolging.config.ArenaServiceConfig.*;
import static no.nav.sbl.dialogarena.common.cxf.InstanceSwitcher.createMetricsProxyWithInstanceSwitcher;

@Configuration
public class ArenaServiceRestConfig {
    private static final String ARENASERVICE_YTELSESKONTRAKT_MOCK_KEY = "ytelseskontrakt.arenaservice.withmock";
    private static final String ARENASERVICE_OPPFOELGING_MOCK_KEY = "oppfoelging.arenaservice.withmock";
    private static final String ARENASERVICE_OPPFOELGINGSSTATUS_V2__MOCK_KEY = "oppfoelgingsstatus.v2.arenaservice.withmock";

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
    public OppfoelgingsstatusV2 oppfoelgingsstatusV2() {
        OppfoelgingsstatusV2 prod = oppfoelgingstatusV2PortType()
                .configureStsForOnBehalfOfWithJWT()
                .build();
        OppfoelgingsstatusV2 mock = new OppfoelgingsstatusV2Mock();
        return createMetricsProxyWithInstanceSwitcher("Oppfoelgingsstatus-ArenaService", prod, mock, ARENASERVICE_OPPFOELGINGSSTATUS_V2__MOCK_KEY, OppfoelgingsstatusV2.class);
    }

}