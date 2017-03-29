package no.nav.fo.veilarbsituasjon.config;

import no.nav.fo.veilarbsituasjon.mock.OrganisasjonEnhetMock;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v1.OrganisasjonEnhetV1;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static no.nav.sbl.dialogarena.common.cxf.InstanceSwitcher.createMetricsProxyWithInstanceSwitcher;

@Configuration
public class OrganisasjonsenhetRestConfig {

    private static final String ORGANISASJONSENHET_MOCK_KEY = "organisasjonenhet.endpoint.url";

    @Bean
    public OrganisasjonEnhetV1 organisasjonEnhetPortType() {
        OrganisasjonEnhetV1 prod = OrganisasjonsenhetConfig.organisasjonEnhetPortType().configureStsForOnBehalfOfWithJWT().build();
        OrganisasjonEnhetV1 mock = new OrganisasjonEnhetMock();
        return createMetricsProxyWithInstanceSwitcher("Organisasjonsenhet", prod, mock, ORGANISASJONSENHET_MOCK_KEY, OrganisasjonEnhetV1.class);
    }

}
