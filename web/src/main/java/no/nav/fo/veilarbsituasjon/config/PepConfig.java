package no.nav.fo.veilarbsituasjon.config;

import no.nav.fo.veilarbsituasjon.services.PepClient;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.context.AbacContext;
import org.springframework.context.annotation.*;

@Configuration
@Import({AbacContext.class})
public class PepConfig {

    @Bean
    PepClient pepClient(Pep pep) {
        return new PepClient(pep);
    }

}
