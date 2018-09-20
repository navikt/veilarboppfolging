package no.nav.fo.veilarboppfolging.config;

import no.nav.sbl.dialogarena.common.cxf.SamlPropagatingOutInterceptor;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.binding.OrganisasjonEnhetV2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrganisasjonsenhetWSConfig {

    @Bean
    public OrganisasjonEnhetV2 organisasjonEnhetPortType() {
        return OrganisasjonsenhetConfig.organisasjonEnhetPortType()
                .withOutInterceptor(new SamlPropagatingOutInterceptor())
                .build();
    }

}
