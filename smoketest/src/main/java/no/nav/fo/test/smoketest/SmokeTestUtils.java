package no.nav.fo.test.smoketest;


import lombok.SneakyThrows;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.ServiceUser;
import no.nav.modig.core.context.AuthenticationLevelCredential;
import no.nav.modig.core.context.OpenAmTokenCredential;
import no.nav.modig.core.context.StaticSubjectHandler;
import no.nav.modig.core.context.SubjectHandler;
import no.nav.modig.core.domain.ConsumerId;
import no.nav.modig.core.domain.SluttBruker;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.Set;

import static java.lang.System.setProperty;
import static java.util.Optional.ofNullable;
import static no.nav.modig.core.context.ModigSecurityConstants.SYSTEMUSER_PASSWORD;
import static no.nav.modig.core.context.ModigSecurityConstants.SYSTEMUSER_USERNAME;
import static no.nav.modig.core.context.SubjectHandler.SUBJECTHANDLER_KEY;
import static no.nav.modig.testcertificates.TestCertificates.setupKeyAndTrustStore;
import static no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants.STS_URL_KEY;

public class SmokeTestUtils {

    public static final String MILJO_VARIABLE_NAME = "miljo";

    private static SslContextFactory SSL_CONTEXT_FACTORY = new SslContextFactory();

    static {
        SSL_CONTEXT_FACTORY.setTrustAll(true);
    }

    public static String getVariable(String variabelnavn) {
        return ofNullable(System.getProperty(variabelnavn, System.getenv(variabelnavn)))
                .orElseThrow(() -> new RuntimeException(String.format("mangler '%s'. Denne må settes som property eller miljøvariabel", variabelnavn)));
    }

    public static void setupOpenAmSubject() throws Exception {
        setProperty(SUBJECTHANDLER_KEY, StaticSubjectHandler.class.getName());
        setupKeyAndTrustStore();
        setProperty(STS_URL_KEY, "https://sts-" + getMiljo() + ".oera-t.local/SecurityTokenServiceProvider/");

        String miljo = getMiljo();
        String openAmUser = getVariable("openAmUser");
        String openAmPassword = getVariable("openAmPassword");


        HttpClient httpClient = new HttpClient(SSL_CONTEXT_FACTORY);
        httpClient.start();

        ContentResponse contentResponse = httpClient
                .newRequest("https://itjenester-" + miljo + ".oera.no/esso/identity/authenticate")
                .param("username", openAmUser)
                .param("password", openAmPassword)
                .send();

        String openAMResponse = contentResponse.getContentAsString();
        String sso = openAMResponse.substring(openAMResponse.indexOf('=') + 1).trim();

        Subject subject = new Subject();
        Set<Principal> principals = subject.getPrincipals();
        principals.add(SluttBruker.eksternBruker(openAmUser));
        principals.add(new ConsumerId(openAmUser));
        Set<Object> publicCredentials = subject.getPublicCredentials();
        publicCredentials.add(new OpenAmTokenCredential(sso));
        publicCredentials.add(new AuthenticationLevelCredential(4));

        StaticSubjectHandler subjectHandler = (StaticSubjectHandler) SubjectHandler.getSubjectHandler();
        subjectHandler.setSubject(subject);
    }

    @SneakyThrows
    public static void setupSystemUser(String username) {
        String app = "veilarboppfolgingproxy";
        String domain = "t6";

        ServiceUser serviceUser = FasitUtils.getServiceUser(username, app, domain);

        setProperty(SYSTEMUSER_USERNAME, serviceUser.username);
        setProperty(SYSTEMUSER_PASSWORD, serviceUser.password);
    }

    public static String getMiljo() {
        return getVariable(MILJO_VARIABLE_NAME);
    }

}
