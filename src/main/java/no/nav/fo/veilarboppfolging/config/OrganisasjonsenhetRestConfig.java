package no.nav.fo.veilarboppfolging.config;

import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.binding.OrganisasjonEnhetV2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrganisasjonsenhetRestConfig {

    @Bean
    public OrganisasjonEnhetV2 organisasjonEnhetPortType() {
        return OrganisasjonsenhetConfig.organisasjonEnhetPortType()
                .configureStsForOnBehalfOfWithJWT()
                .build();
    }

}
