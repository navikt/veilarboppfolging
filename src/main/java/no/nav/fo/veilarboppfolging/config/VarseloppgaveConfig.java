package no.nav.fo.veilarboppfolging.config;

import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.tjeneste.virksomhet.varseloppgave.v1.VarseloppgaveV1;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.lang.System.getProperty;
import static no.nav.fo.veilarboppfolging.config.OppfolgingFeature.SKIP_VALIDERING_DIFI;
import static org.apache.cxf.phase.Phase.PRE_STREAM;

@Configuration
public class VarseloppgaveConfig {

    private static final String VARSELOPPGAVE_ENDPOINT_URL = "varseloppgave.endpoint.url";

    @Bean
    public VarseloppgaveV1 varseloppgaveV1() {
        return factory();
    }

    public VarseloppgaveV1 factory() {
        return varselBuilder()
                .configureStsForOnBehalfOfWithJWT()
                .withOutInterceptor(new TestInterceptor())
                .build();
    }

    public VarseloppgaveV1 pingFactory() {
        return varselBuilder()
                .configureStsForSystemUserInFSS()
                .build();
    }

    private CXFClient<VarseloppgaveV1> varselBuilder() {
        return new CXFClient<>(VarseloppgaveV1.class)
                .address(getProperty(VARSELOPPGAVE_ENDPOINT_URL))
                .withMetrics();
    }

    @Bean
    Helsesjekk varseloppgaveHelsesjekk() {
        return new Helsesjekk() {
            @Override
            public void helsesjekk() throws Throwable {
                pingFactory().ping();
            }

            @Override
            public HelsesjekkMetadata getMetadata() {
                return new HelsesjekkMetadata("varseloppgave",
                        getProperty(VARSELOPPGAVE_ENDPOINT_URL),
                        "Brukes for å sende eskaleringsvarsel",
                        false
                );
            }
        };
    }

    // TODO: Fjern denne når det ikke er behov for den lenger
    // Denne fanger legger på en header som skal gjøre det mulig å teste uten å bli stoppet av difi verifisering.
    private class TestInterceptor extends AbstractPhaseInterceptor<Message> {

        public TestInterceptor() {
            super(PRE_STREAM);
        }

        @Override
        public void handleMessage(Message message) throws Fault {
            if (SKIP_VALIDERING_DIFI.erAktiv()) {
                Map<String, List> headers = (Map<String, List>) message.get(Message.PROTOCOL_HEADERS);
                headers.put("SkipPaaloggingValidation", Collections.singletonList("hvasomhelst"));
            }
        }

    }
}
