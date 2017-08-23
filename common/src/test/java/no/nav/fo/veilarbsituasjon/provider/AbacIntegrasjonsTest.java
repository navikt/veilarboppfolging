package no.nav.fo.veilarbsituasjon.provider;

import no.nav.apiapp.security.PepClient;
import no.nav.apiapp.security.PepClientTester;
import no.nav.dialogarena.config.DevelopmentSecurity;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.fo.IntegrasjonsTest;

import java.io.IOException;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeAll;

public class AbacIntegrasjonsTest extends IntegrasjonsTest implements PepClientTester {

    @BeforeAll
    public static void setup() throws IOException {
        DevelopmentSecurity.setupIntegrationTestSecurity(FasitUtils.getServiceUser("srvveilarbsituasjon", APPLICATION_NAME, "t6"));
        DevelopmentSecurity.configureLdap(FasitUtils.getLdapConfig("ldap", APPLICATION_NAME, "t6"));
        annotationConfigApplicationContext.register(PepClient.class);
    }

    @Inject
    private PepClient pepClient;

    @Override
    public PepClient getPepClient() {
        return pepClient;
    }

}