package no.nav.fo;

import no.nav.dialogarena.config.DevelopmentSecurity;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.fo.veilarbsituasjon.config.DatabaseConfig;
import no.nav.fo.veilarbsituasjon.config.JndiLocalContextConfig;
import no.nav.fo.veilarbsituasjon.config.PepConfig;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import javax.naming.NamingException;
import java.io.IOException;

import static no.nav.fo.veilarbsituasjon.config.DatabaseConfig.DATA_SOURCE_JDNI_NAME;

public abstract class IntegrasjonsTest {

    public static final String APPLICATION_NAME = "veilarbsituasjon";

    protected static AnnotationConfigApplicationContext annotationConfigApplicationContext;
    private static TransactionStatus transaction;
    private static PlatformTransactionManager platformTransactionManager;

    @BeforeAll
    @BeforeClass
    public static void setupFelles() throws IOException {
        DevelopmentSecurity.setupIntegrationTestSecurity(FasitUtils.getServiceUser("srvveilarbsituasjon", APPLICATION_NAME));
        JndiLocalContextConfig.setupInMemoryDatabase();
        annotationConfigApplicationContext = new AnnotationConfigApplicationContext(
                JndiBean.class,
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

    @Component
    public static class JndiBean {

        private final SimpleNamingContextBuilder builder = new SimpleNamingContextBuilder();

        public JndiBean() throws Exception {
            builder.bind(DATA_SOURCE_JDNI_NAME, JndiLocalContextConfig.setupInMemoryDatabase());
            builder.activate();
        }

    }

    @BeforeEach
    @Before
    public final void fiksJndiOgLdapKonflikt() throws NamingException {
        getBean(JndiBean.class).builder.deactivate();
    }

}
