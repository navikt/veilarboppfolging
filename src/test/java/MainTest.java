import no.nav.apiapp.ApiApp;
import no.nav.fo.veilarboppfolging.TestContext;
import no.nav.fo.veilarboppfolging.config.ApplicationConfig;
import no.nav.testconfig.ApiAppTest;

import java.io.IOException;

import static java.lang.System.setProperty;
import static no.nav.brukerdialog.security.Constants.OIDC_REDIRECT_URL_PROPERTY_NAME;
import static no.nav.dialogarena.aktor.AktorConfig.AKTOER_ENDPOINT_URL;
import static no.nav.fo.veilarboppfolging.config.ApplicationConfig.*;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;
import static no.nav.testconfig.ApiAppTest.setupTestContext;

public class MainTest {

    private static final String PORT = "8587";

    public static void main(String[] args) throws IOException {
        setupTestContext(ApiAppTest.Config.builder().applicationName(APPLICATION_NAME).build());
        TestContext.setup();

        setProperty(AKTOER_ENDPOINT_URL, getRequiredProperty(AKTOER_V2_URL_PROPERTY));
        setProperty(OIDC_REDIRECT_URL_PROPERTY_NAME, getRequiredProperty(VEILARBLOGIN_REDIRECT_URL_URL_PROPERTY));

        ApiApp.runApp(ApplicationConfig.class, new String[]{PORT});
    }

}
