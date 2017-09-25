package no.nav.fo.veilarbsituasjon.config;

import no.nav.apiapp.security.PepClient;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.context.AbacContext;
import no.nav.sbl.dialogarena.common.abac.pep.domain.ResourceType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({AbacContext.class})
public class PepConfig {

    @Bean
    public PepClient pepClient(Pep pep) {
        return new PepClient(pep, "veilarb");
    }

}
