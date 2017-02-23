package no.nav.fo.veilarbsituasjon;

import no.nav.modig.core.context.StaticSubjectHandler;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import org.apache.activemq.broker.BrokerService;

import static java.lang.System.setProperty;
import static no.nav.fo.veilarbsituasjon.config.JndiLocalContextConfig.setupInMemoryDatabase;
import static no.nav.fo.veilarbsituasjon.config.JndiLocalContextConfig.setupJndiLocalContext;
import static no.nav.modig.core.context.SubjectHandler.SUBJECTHANDLER_KEY;
import static no.nav.sbl.dialogarena.common.jetty.Jetty.usingWar;
import static no.nav.sbl.dialogarena.common.jetty.JettyStarterUtils.*;

class StartJetty {
    private static final int PORT = 8486;

    public static void main(String[] args) throws Exception {
        setProperty(SUBJECTHANDLER_KEY, StaticSubjectHandler.class.getName());
        if (true) {
            setupInMemoryDatabase();
        } else {
            setupJndiLocalContext();
        }

        final BrokerService broker = new BrokerService();
        broker.getSystemUsage().getTempUsage().setLimit(100 * 1024 * 1024 * 100);
        broker.getSystemUsage().getStoreUsage().setLimit(100 * 1024 * 1024 * 100);
        broker.addConnector("tcp://localhost:61616");
        broker.start();

        Jetty jetty = usingWar()
                .at("/veilarbsituasjon")
                .port(PORT)
                .loadProperties("/environment-test.properties")
                .overrideWebXml()
                .buildJetty();
        jetty.startAnd(first(waitFor(gotKeypress())).then(jetty.stop));
    }
}
