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

@Configuration
public class ArenaServiceWSConfig {

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
                .withOutInterceptor(new UserSAMLOutInterceptor())
                .build();
    }

}
