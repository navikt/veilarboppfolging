package no.nav.fo.veilarboppfolging;

import no.nav.dialogarena.config.fasit.DbCredentials;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.ServiceUser;
import no.nav.dialogarena.config.fasit.ServiceUserCertificate;
import no.nav.sbl.dialogarena.common.abac.pep.CredentialConstants;
import no.nav.sbl.util.EnvironmentUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import static java.lang.System.setProperty;
import static no.nav.brukerdialog.security.Constants.*;
import static no.nav.dialogarena.config.fasit.FasitUtils.*;
import static no.nav.dialogarena.config.fasit.FasitUtils.Zone.FSS;
import static no.nav.fo.veilarboppfolging.config.ApplicationConfig.*;
import static no.nav.fo.veilarboppfolging.config.DatabaseConfig.*;
import static no.nav.sbl.dialogarena.common.abac.pep.service.AbacServiceConfig.ABAC_ENDPOINT_URL_PROPERTY_NAME;
import static no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants.*;
import static no.nav.sbl.featuretoggle.unleash.UnleashServiceConfig.UNLEASH_API_URL_PROPERTY_NAME;
import static no.nav.sbl.util.EnvironmentUtils.Type.PUBLIC;
import static no.nav.sbl.util.EnvironmentUtils.Type.SECRET;

public class TestContext {

    private static final String SERVICE_USER_ALIAS = "srvveilarboppfolging";
    private static final String SECURITY_TOKEN_SERVICE_ALIAS = "securityTokenService";
    private static final String ABAC_PDP_ENDPOINT_ALIAS = "abac.pdp.endpoint";
    private static final String AKTOER_V2_ALIAS = "Aktoer_v2";
    private static final String VEILARBLOGIN_REDIRECT_URL_ALIAS = "veilarblogin.redirect-url";
    private static final String VEILARBAZUREADPROXY_DISCOVERY_ALIAS = "veilarbazureadproxy_discovery";
    private static final String AAD_B2C_CLIENTID_ALIAS = "aad_b2c_clientid";
    private static final String AKTIVITETSPLAN_ALIAS = "aktivitetsplan";
    private static final String VEILARBAKTIVITETAPI_ALIAS = "veilArbAktivitetAPI";
    private static final String VIRKSOMHET_DIGITALKONTAKINFORMASJON_V1_ALIAS = "virksomhet:DigitalKontakinformasjon_v1";
    private static final String VIRKSOMHET_YTELSESKONTRAKT_V3_ALIAS = "virksomhet:Ytelseskontrakt_v3";
    private static final String VIRKSOMHET_OPPFOLGING_V1_ALIAS = "virksomhet:Oppfolging_v1";
    private static final String VIRKSOMHET_OPPFOELGINGSSTATUS_V2_ALIAS = "virksomhet:Oppfoelgingsstatus_v2";
    private static final String VIRKSOMHET_ORGANISASJONENHET_V2_ALIAS = "virksomhet:OrganisasjonEnhet_v2";
    private static final String VARSELOPPGAVE_V1_ALIAS = "Varseloppgave_v1";
    private static final String VIRKSOMHET_BEHANDLEARBEIDSSOEKER_V1_ALIAS = "virksomhet:BehandleArbeidssoeker_v1";
    private static final String KAFKA_BROKERS_ALIAS = "kafka-brokers";
    private static final String VEILARBARENAAPI_ALIAS = "veilarbarenaAPI";

    public static void setup() throws IOException {
        ServiceUser serviceUser = getServiceUser(SERVICE_USER_ALIAS, APPLICATION_NAME);
        setProperty(SYSTEMUSER_USERNAME, serviceUser.getUsername());
        setProperty(SYSTEMUSER_PASSWORD, serviceUser.getPassword());

        DbCredentials dbCredentials = getDbCredentials(APPLICATION_NAME);
        setProperty(VEILARBOPPFOLGINGDB_URL_PROPERTY, dbCredentials.getUrl());
        setProperty(VEILARBOPPFOLGINGDB_USERNAME_PROPERTY, dbCredentials.getUsername());
        setProperty(VEILARBOPPFOLGINGDB_PASSWORD_PROPERTY, dbCredentials.getPassword());

        setProperty(STS_URL_KEY, getBaseUrl(SECURITY_TOKEN_SERVICE_ALIAS, FSS));
        setProperty(ABAC_ENDPOINT_URL_PROPERTY_NAME, getRestService(ABAC_PDP_ENDPOINT_ALIAS, getDefaultEnvironment()).getUrl());
        setProperty(CredentialConstants.SYSTEMUSER_USERNAME, serviceUser.getUsername());
        setProperty(CredentialConstants.SYSTEMUSER_PASSWORD, serviceUser.getPassword());
        setProperty(AKTOER_V2_URL_PROPERTY, getWebServiceEndpoint(AKTOER_V2_ALIAS).getUrl());
        setProperty(UNLEASH_API_URL_PROPERTY_NAME, "https://unleash.nais.adeo.no/api/"); // getRestService(UNLEASH_API_ALIAS, getDefaultEnvironment()).getUrl());

        setProperty(AKTIVITETSPLAN_URL_PROPERTY, getBaseUrl(AKTIVITETSPLAN_ALIAS));
        setProperty(VEILARBAKTIVITETAPI_URL_PROPERTY, getRestService(VEILARBAKTIVITETAPI_ALIAS, getDefaultEnvironment()).getUrl());
        setProperty(VIRKSOMHET_DIGITALKONTAKINFORMASJON_V1_PROPERTY, getWebServiceEndpoint(VIRKSOMHET_DIGITALKONTAKINFORMASJON_V1_ALIAS).getUrl());
        setProperty(VIRKSOMHET_YTELSESKONTRAKT_V3_PROPERTY, getWebServiceEndpoint(VIRKSOMHET_YTELSESKONTRAKT_V3_ALIAS).getUrl());
        setProperty(VIRKSOMHET_OPPFOLGING_V1_PROPERTY, getWebServiceEndpoint(VIRKSOMHET_OPPFOLGING_V1_ALIAS).getUrl());
        setProperty(VIRKSOMHET_OPPFOELGINGSSTATUS_V2_PROPERTY, getWebServiceEndpoint(VIRKSOMHET_OPPFOELGINGSSTATUS_V2_ALIAS).getUrl());
        setProperty(VIRKSOMHET_ORGANISASJONENHET_V2_PROPERTY, getWebServiceEndpoint(VIRKSOMHET_ORGANISASJONENHET_V2_ALIAS).getUrl());
        setProperty(VARSELOPPGAVE_V1_PROPERTY, getWebServiceEndpoint(VARSELOPPGAVE_V1_ALIAS).getUrl());
        setProperty(VIRKSOMHET_BEHANDLEARBEIDSSOEKER_V1_PROPERTY, getWebServiceEndpoint(VIRKSOMHET_BEHANDLEARBEIDSSOEKER_V1_ALIAS).getUrl());
        setProperty(KAFKA_BROKERS_PROPERTY, getBaseUrl(KAFKA_BROKERS_ALIAS));
        setProperty(VEILARBARENAAPI_URL_PROPERTY, getRestService(VEILARBARENAAPI_ALIAS, getDefaultEnvironment()).getUrl());

        ServiceUser isso_rp_user = getServiceUser("isso-rp-user", APPLICATION_NAME);
        String loginUrl = getRestService(VEILARBLOGIN_REDIRECT_URL_ALIAS, getDefaultEnvironment()).getUrl();
        setProperty(ISSO_HOST_URL_PROPERTY_NAME, getBaseUrl("isso-host"));
        setProperty(ISSO_RP_USER_USERNAME_PROPERTY_NAME, isso_rp_user.getUsername());
        setProperty(ISSO_RP_USER_PASSWORD_PROPERTY_NAME, isso_rp_user.getPassword());
        setProperty(ISSO_JWKS_URL_PROPERTY_NAME, getBaseUrl("isso-jwks"));
        setProperty(ISSO_ISSUER_URL_PROPERTY_NAME, getBaseUrl("isso-issuer"));
        setProperty(ISSO_ISALIVE_URL_PROPERTY_NAME, getBaseUrl("isso.isalive", Zone.FSS));
        setProperty(VEILARBLOGIN_REDIRECT_URL_URL_PROPERTY, loginUrl);

        ServiceUser aadB2cUser = getServiceUser(AAD_B2C_CLIENTID_ALIAS, APPLICATION_NAME);
        setProperty(VEILARBAZUREADPROXY_DISCOVERY_URL_PROPERTY, getRestService(VEILARBAZUREADPROXY_DISCOVERY_ALIAS, getDefaultEnvironment()).getUrl());
        setProperty(AAD_B2C_CLIENTID_USERNAME_PROPERTY, aadB2cUser.getUsername());
        setProperty(AAD_B2C_CLIENTID_PASSWORD_PROPERTY, aadB2cUser.getPassword());

        ServiceUserCertificate navTrustStore = FasitUtils.getServiceUserCertificate("nav_truststore", FasitUtils.getDefaultEnvironmentClass());
        File navTrustStoreFile = File.createTempFile("nav_truststore", ".jks");
        FileUtils.writeByteArrayToFile(navTrustStoreFile, navTrustStore.getKeystore());

        EnvironmentUtils.setProperty("javax.net.ssl.trustStore", navTrustStoreFile.getAbsolutePath(), PUBLIC);
        EnvironmentUtils.setProperty("javax.net.ssl.trustStorePassword", navTrustStore.getKeystorepassword(), SECRET);
    }
}
