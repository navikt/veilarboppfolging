package no.nav.fo.veilarboppfolging;

import no.nav.dialogarena.config.DevelopmentSecurity.ISSOSecurityConfig;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.ServiceUserCertificate;
import no.nav.fo.veilarboppfolging.kafka.ConsumerConfig;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import no.nav.sbl.dialogarena.test.SystemProperties;
import org.apache.commons.io.FileUtils;

import java.io.File;

import static no.nav.dialogarena.config.DevelopmentSecurity.setupISSO;
import static no.nav.fo.veilarboppfolging.config.DatabaseConfig.DATA_SOURCE_JDNI_NAME;
import static no.nav.fo.veilarboppfolging.config.JndiLocalContextConfig.configureDataSource;
import static no.nav.sbl.dialogarena.common.jetty.Jetty.usingWar;
import static no.nav.sbl.dialogarena.common.jetty.JettyStarterUtils.*;
import static no.nav.sbl.util.EnvironmentUtils.Type.PUBLIC;
import static no.nav.sbl.util.EnvironmentUtils.Type.SECRET;
import static no.nav.sbl.util.EnvironmentUtils.setProperty;


class StartJetty {

    private static final String APPLICATION_NAME = "veilarboppfolging";

    static final String CONTEXT_NAME = "/veilarboppfolging";
    static final int PORT = 8587;

    public static void main(String[] args) throws Exception {
        setProperty(ConsumerConfig.KAFKA_BROKERS_URL_PROPERTY_NAME, FasitUtils.getBaseUrl("kafka-brokers"), PUBLIC);

        loadTestConfigFromProperties();
        
        Jetty jetty = setupISSO(usingWar()
                        .at(CONTEXT_NAME)
                        .port(PORT)
                        .addDatasource(configureDataSource(APPLICATION_NAME), DATA_SOURCE_JDNI_NAME)
                , new ISSOSecurityConfig(APPLICATION_NAME)).buildJetty();


        // kafka trenger fungerende truststore
        ServiceUserCertificate navTrustStore = FasitUtils.getServiceUserCertificate("nav_truststore", FasitUtils.getDefaultEnvironmentClass());
        File navTrustStoreFile = File.createTempFile("nav_truststore", ".jks");
        FileUtils.writeByteArrayToFile(navTrustStoreFile,navTrustStore.getKeystore());

        setProperty("javax.net.ssl.trustStore", navTrustStoreFile.getAbsolutePath(), PUBLIC);
        setProperty("javax.net.ssl.trustStorePassword", navTrustStore.getKeystorepassword(), SECRET);

        jetty.startAnd(first(waitFor(gotKeypress())).then(jetty.stop));
    }

    private static void loadTestConfigFromProperties() {
        try {
            SystemProperties.setFrom("environment-local.properties");
        } catch (Exception e) {
            SystemProperties.setFrom("environment-test.properties");
        }
    }

}
