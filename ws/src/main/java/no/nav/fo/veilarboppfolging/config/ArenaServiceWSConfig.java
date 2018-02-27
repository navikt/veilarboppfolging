package no.nav.fo.veilarboppfolging.config;


import no.nav.modig.security.ws.UserSAMLOutInterceptor;
import no.nav.tjeneste.virksomhet.behandlearbeidssoeker.v1.binding.BehandleArbeidssoekerV1;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.YtelseskontraktV3;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static no.nav.fo.veilarboppfolging.config.ArenaBehandleArbeidssokerWSConfig.behandleArbeidssokerPortType;
import static no.nav.fo.veilarboppfolging.config.ArenaServiceConfig.oppfoelgingPortType;
import static no.nav.fo.veilarboppfolging.config.ArenaServiceConfig.ytelseskontraktPortType;
import static no.nav.sbl.dialogarena.common.cxf.TimeoutFeature.DEFAULT_CONNECTION_TIMEOUT;

@Configuration
public class ArenaServiceWSConfig {

    private static final int BEHANDLE_ARBEIDSSOKER_RECEIVE_TIMEOUT = 60000;

    @Bean
    public YtelseskontraktV3 ytelseskontraktV3() {
        return ytelseskontraktPortType()
                .withOutInterceptor(new UserSAMLOutInterceptor())
                .build();
    }

    @Bean
    public OppfoelgingPortType oppfoelgingV1() {
        return oppfoelgingPortType()
                .withOutInterceptor(new UserSAMLOutInterceptor())
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
