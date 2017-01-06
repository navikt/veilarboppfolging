package no.nav.fo.veilarbsituasjon.rest;

import no.nav.modig.core.context.StaticSubjectHandler;
import no.nav.sbl.dialogarena.common.jetty.Jetty;

import static java.lang.System.setProperty;
import static no.nav.modig.core.context.SubjectHandler.SUBJECTHANDLER_KEY;
import static no.nav.sbl.dialogarena.common.jetty.Jetty.usingWar;
import static no.nav.sbl.dialogarena.common.jetty.JettyStarterUtils.*;

public class StartJetty {
    private static final int PORT = 8486;

    public static void main(String[] args) throws Exception {
        setProperty(SUBJECTHANDLER_KEY, StaticSubjectHandler.class.getName());

        Jetty jetty = usingWar()
                .at("/veilarbsituasjon")
                .port(PORT)
                .loadProperties("/environment-test.properties")
                .buildJetty();
        jetty.startAnd(first(waitFor(gotKeypress())).then(jetty.stop));
    }
}
