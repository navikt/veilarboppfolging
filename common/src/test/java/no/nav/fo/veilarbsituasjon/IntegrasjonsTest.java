package no.nav.fo.veilarbsituasjon;

import no.nav.dialogarena.config.DevelopmentSecurity;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.fo.veilarbsituasjon.config.ApplicationConfig;
import no.nav.fo.veilarbsituasjon.config.DatabaseConfig;
import no.nav.fo.veilarbsituasjon.config.JndiLocalContextConfig;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import no.nav.fo.veilarbsituasjon.config.PepConfig;
import no.nav.sbl.dialogarena.common.abac.pep.context.AbacContext;
import org.junit.Before;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;

public abstract class IntegrasjonsTest {

    public static final String APPLICATION_NAME = "veilarbsituasjon";

    protected static AnnotationConfigApplicationContext annotationConfigApplicationContext;
    private static TransactionStatus transaction;
    private static PlatformTransactionManager platformTransactionManager;

    @BeforeAll
    @BeforeClass
    public static void setup() throws IOException {
        DevelopmentSecurity.setupIntegrationTestSecurity(FasitUtils.getServiceUser("srvveilarbsituasjon", APPLICATION_NAME, "t6"));
        DevelopmentSecurity.configureLdap(FasitUtils.getLdapConfig("ldap", APPLICATION_NAME, "t6"));
        JndiLocalContextConfig.setupInMemoryDatabase();
        annotationConfigApplicationContext = new AnnotationConfigApplicationContext(
                DatabaseConfig.class,
                PepConfig.class
        );
        annotationConfigApplicationContext.start();
        platformTransactionManager = getBean(PlatformTransactionManager.class);
    }

    @BeforeEach
    @Before
    public void injectAvhengigheter() {
        annotationConfigApplicationContext.getAutowireCapableBeanFactory().autowireBean(this);
    }

    @BeforeEach
    @Before
    public void beginTransaction() {
        transaction = platformTransactionManager.getTransaction(new TransactionTemplate());
    }

    @AfterEach
    @After
    public void rollbackTransaction() {
        platformTransactionManager.rollback(transaction);
        transaction = null;
    }

    @AfterAll
    @AfterClass
    public static void tearDown() {
        if (annotationConfigApplicationContext != null) {
            annotationConfigApplicationContext.stop();
        }
    }

    protected static <T> T getBean(Class<T> requiredType) {
        return annotationConfigApplicationContext.getBean(requiredType);
    }

}
