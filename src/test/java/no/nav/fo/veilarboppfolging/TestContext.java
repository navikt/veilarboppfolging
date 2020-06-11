package no.nav.fo.veilarboppfolging;

import no.nav.fasit.DbCredentials;
import no.nav.fasit.FasitUtils;
import no.nav.fasit.ServiceUser;
import no.nav.fasit.ServiceUserCertificate;
import no.nav.fasit.dto.RestService;
import no.nav.sbl.dialogarena.common.abac.pep.CredentialConstants;
import no.nav.sbl.util.EnvironmentUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import static java.lang.System.setProperty;
import static no.nav.brukerdialog.security.Constants.*;
import static no.nav.brukerdialog.security.oidc.provider.AzureADB2CConfig.EXTERNAL_USERS_AZUREAD_B2C_DISCOVERY_URL;
import static no.nav.fasit.FasitUtils.*;
import static no.nav.fasit.FasitUtils.Zone.FSS;
import static no.nav.fo.veilarboppfolging.config.ApplicationConfig.*;
import static no.nav.fo.veilarboppfolging.config.DatabaseConfig.*;
import static no.nav.sbl.dialogarena.common.abac.pep.service.AbacServiceConfig.ABAC_ENDPOINT_URL_PROPERTY_NAME;
import static no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants.*;
import static no.nav.sbl.featuretoggle.unleash.UnleashServiceConfig.UNLEASH_API_URL_PROPERTY_NAME;
import static no.nav.sbl.util.EnvironmentUtils.APP_ENVIRONMENT_NAME_PROPERTY_NAME;
import static no.nav.sbl.util.EnvironmentUtils.Type.PUBLIC;
import static no.nav.sbl.util.EnvironmentUtils.Type.SECRET;

public class TestContext {

    private static final String SERVICE_USER_ALIAS = "srvveilarboppfolging";
    private static final String SECURITY_TOKEN_SERVICE_ALIAS = "securityTokenService";
    private static final String ABAC_PDP_ENDPOINT_ALIAS = "abac.pdp.endpoint";
    private static final String AKTOER_V2_ALIAS = "Aktoer_v2";
    private static final String VEILARBLOGIN_REDIRECT_URL_ALIAS = "veilarblogin.redirect-url";
    private static final String AZURE_AD_B2C_DISCOVERY_ALIAS = "aad_b2c_discovery";
    private static final String AAD_B2C_CLIENTID_ALIAS = "aad_b2c_clientid";
    private static final String ARBEIDSRETTET_DIALOG_ALIAS = "arbeidsrettet_dialog";
    private static final String VEILARBAKTIVITETAPI_ALIAS = "veilArbAktivitetAPI";
    private static final String VIRKSOMHET_DIGITALKONTAKINFORMASJON_V1_ALIAS = "virksomhet:DigitalKontakinformasjon_v1";
    private static final String VIRKSOMHET_YTELSESKONTRAKT_V3_ALIAS = "virksomhet:Ytelseskontrakt_v3";
    private static final String VIRKSOMHET_OPPFOLGING_V1_ALIAS = "virksomhet:Oppfolging_v1";
    private static final String VIRKSOMHET_ORGANISASJONENHET_V2_ALIAS = "virksomhet:OrganisasjonEnhet_v2";
    private static final String VARSELOPPGAVE_V1_ALIAS = "Varseloppgave_v1";
    private static final String VIRKSOMHET_BEHANDLEARBEIDSSOEKER_V1_ALIAS = "virksomhet:BehandleArbeidssoeker_v1";
    private static final String KAFKA_BROKERS_ALIAS = "kafka-brokers";
    private static final String VEILARBARENAAPI_ALIAS = "veilarbarenaAPI";
    private static final String UNLEASH_API = "unleash-api";

    public static void setup() throws IOException {
        ServiceUser serviceUser = getServiceUser(SERVICE_USER_ALIAS, APPLICATION_NAME);
        setProperty(SYSTEMUSER_USERNAME, serviceUser.getUsername());
        setProperty(SYSTEMUSER_PASSWORD, serviceUser.getPassword());

        DbCredentials dbCredentials = getDbCredentials(APPLICATION_NAME);
        setProperty(VEILARBOPPFOLGINGDB_URL_PROPERTY, dbCredentials.getUrl());
        setProperty(VEILARBOPPFOLGINGDB_USERNAME_PROPERTY, dbCredentials.getUsername());
        setProperty(VEILARBOPPFOLGINGDB_PASSWORD_PROPERTY, dbCredentials.getPassword());

        setProperty(STS_URL_KEY, getBaseUrl(SECURITY_TOKEN_SERVICE_ALIAS, FSS));
        RestService abacEndpoint = FasitUtils.getRestServices(ABAC_PDP_ENDPOINT_ALIAS).stream()
                .filter(rs -> getDefaultEnvironment().equals(rs.getEnvironment()) && rs.getApplication() == null)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Fant ikke " + ABAC_PDP_ENDPOINT_ALIAS + " i Fasit"));
        setProperty(ABAC_ENDPOINT_URL_PROPERTY_NAME, abacEndpoint.getUrl());
        setProperty(CredentialConstants.SYSTEMUSER_USERNAME, serviceUser.getUsername());
        setProperty(CredentialConstants.SYSTEMUSER_PASSWORD, serviceUser.getPassword());
        setProperty(AKTOER_V2_URL_PROPERTY, getWebServiceEndpoint(AKTOER_V2_ALIAS).getUrl());
        setProperty(UNLEASH_API_URL_PROPERTY_NAME, getRestService(UNLEASH_API).getUrl());

        setProperty(ARBEIDSRETTET_DIALOG_PROPERTY, getBaseUrl(ARBEIDSRETTET_DIALOG_ALIAS));
        setProperty(VEILARBAKTIVITETAPI_URL_PROPERTY, getRestService(VEILARBAKTIVITETAPI_ALIAS).getUrl());
        setProperty(VIRKSOMHET_DIGITALKONTAKINFORMASJON_V1_PROPERTY, getWebServiceEndpoint(VIRKSOMHET_DIGITALKONTAKINFORMASJON_V1_ALIAS, "q2").getUrl());
        setProperty(VIRKSOMHET_YTELSESKONTRAKT_V3_PROPERTY, getWebServiceEndpoint(VIRKSOMHET_YTELSESKONTRAKT_V3_ALIAS).getUrl());
        setProperty(VIRKSOMHET_OPPFOLGING_V1_PROPERTY, getWebServiceEndpoint(VIRKSOMHET_OPPFOLGING_V1_ALIAS).getUrl());
        setProperty(VIRKSOMHET_ORGANISASJONENHET_V2_PROPERTY, getWebServiceEndpoint(VIRKSOMHET_ORGANISASJONENHET_V2_ALIAS).getUrl());
        setProperty(VARSELOPPGAVE_V1_PROPERTY, getWebServiceEndpoint(VARSELOPPGAVE_V1_ALIAS).getUrl());
        setProperty(VIRKSOMHET_BEHANDLEARBEIDSSOEKER_V1_PROPERTY, getWebServiceEndpoint(VIRKSOMHET_BEHANDLEARBEIDSSOEKER_V1_ALIAS).getUrl());
        setProperty(KAFKA_BROKERS_PROPERTY, getBaseUrl(KAFKA_BROKERS_ALIAS));
        setProperty(VEILARBARENAAPI_URL_PROPERTY, getRestService(VEILARBARENAAPI_ALIAS).getUrl());

        ServiceUser isso_rp_user = getServiceUser("isso-rp-user", APPLICATION_NAME);
        String loginUrl = getRestService(VEILARBLOGIN_REDIRECT_URL_ALIAS).getUrl();
        setProperty(ISSO_HOST_URL_PROPERTY_NAME, getBaseUrl("isso-host"));
        setProperty(ISSO_RP_USER_USERNAME_PROPERTY_NAME, isso_rp_user.getUsername());
        setProperty(ISSO_RP_USER_PASSWORD_PROPERTY_NAME, isso_rp_user.getPassword());
        setProperty(ISSO_JWKS_URL_PROPERTY_NAME, getBaseUrl("isso-jwks"));
        setProperty(ISSO_ISSUER_URL_PROPERTY_NAME, getBaseUrl("isso-issuer"));
        setProperty(ISSO_ISALIVE_URL_PROPERTY_NAME, getBaseUrl("isso.isalive", Zone.FSS));
        setProperty(VEILARBLOGIN_REDIRECT_URL_URL_PROPERTY, loginUrl);

        ServiceUser aadB2cUser = getServiceUser(AAD_B2C_CLIENTID_ALIAS, APPLICATION_NAME);
        setProperty(EXTERNAL_USERS_AZUREAD_B2C_DISCOVERY_URL, getBaseUrl(AZURE_AD_B2C_DISCOVERY_ALIAS));
        setProperty(AAD_B2C_CLIENTID_USERNAME_PROPERTY, aadB2cUser.getUsername());
        setProperty(AAD_B2C_CLIENTID_PASSWORD_PROPERTY, aadB2cUser.getPassword());

        setProperty("VEILARBPORTEFOLJEAPI_URL", "https://veilarbportefolje-" + FasitUtils.getDefaultEnvironment() + ".nais.preprod.local/veilarbportefolje/api");
        setProperty(APP_ENVIRONMENT_NAME_PROPERTY_NAME, getDefaultEnvironment());

        String stsRestServiceAlias = "security-token-service-openid-configuration";
        RestService stsRestService = getRestServices(stsRestServiceAlias)
                .stream()
                .filter(x -> x.getEnvironmentClass().equals(getEnvironmentClass(getDefaultEnvironment())) && x.getEnvironment() == null)
                .findFirst().orElseThrow(() -> new IllegalStateException("Fant ikke " + stsRestServiceAlias + " i Fasit"));
        EnvironmentUtils.setProperty(STS_OIDC_CONFIGURATION_URL_PROPERTY, stsRestService.getUrl(), PUBLIC);

        ServiceUserCertificate navTrustStore = FasitUtils.getServiceUserCertificate("nav_truststore_pto", FasitUtils.getDefaultEnvironmentClass());
        File navTrustStoreFile = File.createTempFile("nav_truststore", ".jks");
        FileUtils.writeByteArrayToFile(navTrustStoreFile, navTrustStore.getKeystore());

        EnvironmentUtils.setProperty("javax.net.ssl.trustStore", navTrustStoreFile.getAbsolutePath(), PUBLIC);
        EnvironmentUtils.setProperty("javax.net.ssl.trustStorePassword", navTrustStore.getKeystorepassword(), SECRET);
    }
}
