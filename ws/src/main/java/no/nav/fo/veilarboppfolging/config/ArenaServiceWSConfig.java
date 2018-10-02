package no.nav.fo.veilarboppfolging.config;


import no.nav.sbl.dialogarena.common.cxf.SamlPropagatingOutInterceptor;
import no.nav.tjeneste.virksomhet.behandlearbeidssoeker.v1.binding.BehandleArbeidssoekerV1;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v2.binding.OppfoelgingsstatusV2;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.YtelseskontraktV3;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static no.nav.fo.veilarboppfolging.config.ArenaBehandleArbeidssokerWSConfig.behandleArbeidssokerPortType;
import static no.nav.fo.veilarboppfolging.config.ArenaServiceConfig.*;
import static no.nav.sbl.dialogarena.common.cxf.TimeoutFeature.DEFAULT_CONNECTION_TIMEOUT;

@Configuration
public class ArenaServiceWSConfig {

    private static final int BEHANDLE_ARBEIDSSOKER_RECEIVE_TIMEOUT = 300000;

    @Bean
    public YtelseskontraktV3 ytelseskontraktV3() {
        return ytelseskontraktPortType()
                .withOutInterceptor(new SamlPropagatingOutInterceptor())
                .build();
    }

    @Bean
    public OppfoelgingPortType oppfoelgingV1() {
        return oppfoelgingPortType()
                .withOutInterceptor(new SamlPropagatingOutInterceptor())
                .build();
    }

    @Bean
    public OppfoelgingsstatusV2 oppfoelgingsstatusV2() {
        return oppfoelgingstatusV2PortType()
                .withOutInterceptor(new SamlPropagatingOutInterceptor())
                .build();
    }

    @Bean
    public BehandleArbeidssoekerV1 behandleArbeidssoekerV1() {
        return behandleArbeidssokerPortType()
                .configureStsForSystemUserInFSS()
                .timeout(DEFAULT_CONNECTION_TIMEOUT, BEHANDLE_ARBEIDSSOKER_RECEIVE_TIMEOUT)
                .build();
    }

}
