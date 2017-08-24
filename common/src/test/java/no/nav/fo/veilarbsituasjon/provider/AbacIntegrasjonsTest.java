package no.nav.fo.veilarbsituasjon.provider;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeAll;

import no.nav.apiapp.security.PepClient;
import no.nav.apiapp.security.PepClientTester;
import no.nav.dialogarena.config.DevelopmentSecurity;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.veilarbsituasjon.config.PepConfig;

public class AbacIntegrasjonsTest extends IntegrasjonsTest implements PepClientTester {

    private static final String APPLICATION_NAME = "veilarbsituasjon";

    @BeforeAll
    public static void setup() {
        DevelopmentSecurity.setupIntegrationTestSecurity(FasitUtils.getServiceUser("srvveilarbsituasjon", APPLICATION_NAME, "t6"));
        DevelopmentSecurity.configureLdap(FasitUtils.getLdapConfig("ldap", APPLICATION_NAME, "t6"));        
        annotationConfigApplicationContext.register(PepConfig.class);
    }
    
    @Inject
    private PepClient pepClient;

    @Override
    public PepClient getPepClient() {
        return pepClient;
    }

}