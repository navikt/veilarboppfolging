import no.nav.fo.veilarboppfolging.TestContext;
import no.nav.testconfig.ApiAppTest;

import java.io.IOException;

import static no.nav.fo.veilarboppfolging.config.ApplicationConfig.APPLICATION_NAME;
import static no.nav.testconfig.ApiAppTest.setupTestContext;

public class MainTest {

    private static final String PORT = "8587";

    public static void main(String[] args) throws IOException {
        setupTestContext(ApiAppTest.Config.builder().applicationName(APPLICATION_NAME).build());
        TestContext.setup();
        Main.main(PORT);
    }

}
