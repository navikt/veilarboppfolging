package no.nav.fo.veilarboppfolging.config;

import no.nav.sbl.dialogarena.common.cxf.SamlPropagatingOutInterceptor;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v1.OrganisasjonEnhetV1;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrganisasjonsenhetWSConfig {

    @Bean
    public OrganisasjonEnhetV1 organisasjonEnhetPortType() {
        return OrganisasjonsenhetConfig.organisasjonEnhetPortType()
                .withOutInterceptor(new SamlPropagatingOutInterceptor())
                .build();
    }

}
