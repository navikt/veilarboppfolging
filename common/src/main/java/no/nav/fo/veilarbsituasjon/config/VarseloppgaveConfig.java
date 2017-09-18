package no.nav.fo.veilarbsituasjon.config;

import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.tjeneste.virksomhet.varseloppgave.v1.VarseloppgaveV1;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.*;

import static java.lang.System.getProperty;
import static org.apache.cxf.phase.Phase.PRE_STREAM;

@Configuration
public class VarseloppgaveConfig {

    @Bean
    public VarseloppgaveV1 varseloppgaveV1() {
        return factory();
    }

    public VarseloppgaveV1 factory() {
        return new CXFClient<>(VarseloppgaveV1.class)
                .address(getProperty("varseloppgave.endpoint.url"))
                .configureStsForOnBehalfOfWithJWT()
                .withOutInterceptor(new TestInterceptor())
                .withMetrics()
                .build();
    }

    public VarseloppgaveV1 pingFactory() {
        return new CXFClient<>(VarseloppgaveV1.class)
                .address(getProperty("varseloppgave.endpoint.url"))
                .configureStsForSystemUserInFSS()
                .withMetrics()
                .build();
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
                return new HelsesjekkMetadata(getProperty("varseloppgave.endpoint.url"), "Brukes for å sende eskaleringsvarsel", false);
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
            Map<String, List> headers = (Map<String, List>) message.get(Message.PROTOCOL_HEADERS);
            headers.put("SkipPaaloggingValidation", Collections.singletonList("hvasomhelst"));
        }

    }
}
