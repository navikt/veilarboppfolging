package no.nav.fo.veilarbsituasjon;

import static no.nav.dialogarena.mock.MockServer.startMockServer;
import static no.nav.fo.veilarbsituasjon.StartJetty.CONTEXT_NAME;
import static no.nav.fo.veilarbsituasjon.StartJetty.PORT;

public class StartMock {

    public static void main(String[] args) throws Exception {
        startMockServer(CONTEXT_NAME, PORT);
    }

}
