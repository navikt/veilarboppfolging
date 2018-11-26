package no.nav.fo.veilarboppfolging;

import static no.nav.dialogarena.mock.MockServer.startMockServer;
import static no.nav.fo.veilarboppfolging.StartJetty.CONTEXT_NAME;
import static no.nav.fo.veilarboppfolging.StartJetty.PORT;

public class StartMock {

    public static void main(String[] args) throws Exception {
        startMockServer(CONTEXT_NAME, PORT);
    }

}
