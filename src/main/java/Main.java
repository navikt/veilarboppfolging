import no.nav.apiapp.ApiApp;

import no.nav.brukerdialog.tools.SecurityConstants;
import no.nav.common.utils.NaisUtils;
import no.nav.fo.veilarboppfolging.config.ApplicationConfig;
import no.nav.sbl.dialogarena.common.abac.pep.CredentialConstants;
import no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants;

import static java.lang.System.setProperty;
import static no.nav.brukerdialog.security.Constants.OIDC_REDIRECT_URL_PROPERTY_NAME;
import static no.nav.dialogarena.aktor.AktorConfig.AKTOER_ENDPOINT_URL;
import static no.nav.fo.veilarboppfolging.config.ApplicationConfig.*;
import static no.nav.fo.veilarboppfolging.config.DatabaseConfig.VEILARBOPPFOLGINGDB_PASSWORD_PROPERTY;
import static no.nav.fo.veilarboppfolging.config.DatabaseConfig.VEILARBOPPFOLGINGDB_USERNAME_PROPERTY;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

public class Main {

    public static void main(String... args) {
        setProperty(AKTOER_ENDPOINT_URL, getRequiredProperty(AKTOER_V2_URL_PROPERTY));
        setProperty(OIDC_REDIRECT_URL_PROPERTY_NAME, getRequiredProperty(VEILARBLOGIN_REDIRECT_URL_URL_PROPERTY));

        NaisUtils.Credentials serviceUser = NaisUtils.getCredentials("service_user");

        //ABAC
        System.setProperty(CredentialConstants.SYSTEMUSER_USERNAME, serviceUser.username);
        System.setProperty(CredentialConstants.SYSTEMUSER_PASSWORD, serviceUser.password);

        //CXF
        System.setProperty(StsSecurityConstants.SYSTEMUSER_USERNAME, serviceUser.username);
        System.setProperty(StsSecurityConstants.SYSTEMUSER_PASSWORD, serviceUser.password);

        //OIDC
        System.setProperty(SecurityConstants.SYSTEMUSER_USERNAME, serviceUser.username);
        System.setProperty(SecurityConstants.SYSTEMUSER_PASSWORD, serviceUser.password);


        NaisUtils.Credentials oracleCreds = NaisUtils.getCredentials("oracle_creds");
        System.setProperty(VEILARBOPPFOLGINGDB_USERNAME_PROPERTY, oracleCreds.username);
        System.setProperty(VEILARBOPPFOLGINGDB_PASSWORD_PROPERTY, oracleCreds.password);

        ApiApp.runApp(ApplicationConfig.class, args);
    }

}
