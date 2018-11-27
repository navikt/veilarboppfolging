package no.nav.fo.veilarboppfolging.config;

import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.tjeneste.virksomhet.varseloppgave.v1.VarseloppgaveV1;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static no.nav.fo.veilarboppfolging.config.ApplicationConfig.VARSELOPPGAVE_V1_PROPERTY;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Configuration
public class VarseloppgaveConfig {

    @Bean
    public VarseloppgaveV1 varseloppgaveV1() {
        return factory();
    }

    public VarseloppgaveV1 factory() {
        return varselBuilder()
                .configureStsForOnBehalfOfWithJWT()
                .build();
    }

    public VarseloppgaveV1 pingFactory() {
        return varselBuilder()
                .configureStsForSystemUser()
                .build();
    }

    private CXFClient<VarseloppgaveV1> varselBuilder() {
        return new CXFClient<>(VarseloppgaveV1.class)
                .address(getRequiredProperty(VARSELOPPGAVE_V1_PROPERTY))
                .withMetrics();
    }

    @Bean
    Helsesjekk varseloppgaveHelsesjekk() {
        return new Helsesjekk() {
            @Override
            public void helsesjekk() {
                pingFactory().ping();
            }

            @Override
            public HelsesjekkMetadata getMetadata() {
                return new HelsesjekkMetadata("varseloppgave",
                        getRequiredProperty(VARSELOPPGAVE_V1_PROPERTY),
                        "Brukes for å sende eskaleringsvarsel",
                        false
                );
            }
        };
    }

}
