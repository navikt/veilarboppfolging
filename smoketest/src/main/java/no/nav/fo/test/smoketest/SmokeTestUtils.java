package no.nav.fo.test.smoketest;


import lombok.SneakyThrows;
import no.nav.modig.core.context.AuthenticationLevelCredential;
import no.nav.modig.core.context.OpenAmTokenCredential;
import no.nav.modig.core.context.StaticSubjectHandler;
import no.nav.modig.core.context.SubjectHandler;
import no.nav.modig.core.domain.ConsumerId;
import no.nav.modig.core.domain.SluttBruker;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.BasicAuthentication;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.security.auth.Subject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URI;
import java.security.Principal;
import java.util.Set;

import static java.lang.System.setProperty;
import static java.util.Optional.ofNullable;
import static no.nav.modig.core.context.ModigSecurityConstants.SYSTEMUSER_PASSWORD;
import static no.nav.modig.core.context.ModigSecurityConstants.SYSTEMUSER_USERNAME;
import static no.nav.modig.core.context.SubjectHandler.SUBJECTHANDLER_KEY;
import static no.nav.modig.testcertificates.TestCertificates.setupKeyAndTrustStore;
import static no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants.STS_URL_KEY;
import static org.slf4j.LoggerFactory.getLogger;

public class SmokeTestUtils {

    public static final String FASIT_USERNAME_VARIABLE_NAME = "domenebrukernavn";
    public static final String FASIT_PASSWORD_VARIABLE_NAME = "domenepassord";
    public static final String MILJO_VARIABLE_NAME = "miljo";

    private static final Logger LOG = getLogger(SmokeTestUtils.class);
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
        String app = "veilarbsituasjonproxy";
        String domain = "oera-t.local";

        ServiceBruker serviceBruker = getServiceBruker(username, app, domain);

        setProperty(SYSTEMUSER_USERNAME, serviceBruker.username);
        setProperty(SYSTEMUSER_PASSWORD, serviceBruker.password);
    }

    private static ServiceBruker getServiceBruker(String username, String app, String domain) throws Exception {
        ServiceBruker serviceBruker = new ServiceBruker();

        HttpClient httpClient = new HttpClient(SSL_CONTEXT_FACTORY);
        httpClient.getAuthenticationStore().addAuthentication(new FasitAuthenication());
        httpClient.start();
        String miljo = getMiljo();

        String resourceUrl = "https://fasit.adeo.no/conf/resources/bestmatch?envName=" + miljo + "&domain=" + domain + "&type=Credential&alias=" + username + "&app=" + app;
        LOG.info(resourceUrl);

        String resourceXml = httpClient
                .newRequest(resourceUrl)
                .send()
                .getContentAsString();

        LOG.info(resourceXml);

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document document = documentBuilder.parse(new InputSource(new StringReader(resourceXml)));
        NodeList properties = document.getElementsByTagName("property");
        serviceBruker.username = extractProperty(properties, "username");

        String passwordUrl = extractProperty(properties, "password");
        LOG.info(passwordUrl);

        serviceBruker.password = httpClient
                .newRequest(passwordUrl)
                .send()
                .getContentAsString();

        return serviceBruker;
    }


    private static String extractProperty(NodeList properties, String password) {
        for (int i = 0; i < properties.getLength(); i++) {
            Node item = properties.item(i);
            String propertyName = item.getAttributes().getNamedItem("name").getTextContent();
            if (password.equals(propertyName)) {
                return item.getFirstChild().getTextContent();
            }
        }
        throw new IllegalStateException();
    }

    public static String getFasitPassword() {
        return getVariable(FASIT_PASSWORD_VARIABLE_NAME);
    }

    public static String getMiljo() {
        return getVariable(MILJO_VARIABLE_NAME);
    }

    private static String getFasitUser() {
        return getVariable(FASIT_USERNAME_VARIABLE_NAME);
    }

    private static class FasitAuthenication extends BasicAuthentication {

        public FasitAuthenication() {
            super(null, null, getFasitUser(), getFasitPassword());
        }

        @Override
        public boolean matches(String type, URI uri, String realm) {
            return true;
        }

    }

    private static class ServiceBruker {
        public String username;
        public String password;
    }

}
