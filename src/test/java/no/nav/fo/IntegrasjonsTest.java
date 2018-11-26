package no.nav.fo;

import no.nav.dialogarena.config.DevelopmentSecurity;
import no.nav.fo.veilarboppfolging.config.DatabaseConfig;
import no.nav.fo.veilarboppfolging.config.PepConfig;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

import static no.nav.fo.veilarboppfolging.config.ApplicationConfig.APPLICATION_NAME;
import static no.nav.fo.veilarboppfolging.config.DatabaseConfig.*;
import static no.nav.fo.veilarboppfolging.db.testdriver.TestDriver.createInMemoryDatabaseUrl;
import static no.nav.sbl.dialogarena.test.SystemProperties.setTemporaryProperty;

public abstract class IntegrasjonsTest {

    protected static AnnotationConfigApplicationContext annotationConfigApplicationContext;
    private static TransactionStatus transaction;
    private static PlatformTransactionManager platformTransactionManager;

    @BeforeAll
    @BeforeClass
    public static void setupFelles() {
        DevelopmentSecurity.setupIntegrationTestSecurity(new DevelopmentSecurity.IntegrationTestConfig(APPLICATION_NAME));
        setTemporaryProperty(VEILARBOPPFOLGINGDB_URL_PROPERTY, createInMemoryDatabaseUrl(), () -> {
            setTemporaryProperty(VEILARBOPPFOLGINGDB_USERNAME_PROPERTY, "sa", () -> {
                setTemporaryProperty(VEILARBOPPFOLGINGDB_PASSWORD_PROPERTY, "pw", () -> {
                    annotationConfigApplicationContext = new AnnotationConfigApplicationContext(
                            DatabaseConfig.class,
                            PepConfig.class
                    );
                    annotationConfigApplicationContext.start();
                    platformTransactionManager = getBean(PlatformTransactionManager.class);
                    migrateDatabase(getBean(DataSource.class));
                });
            });
        });
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
    public static void stopSpringContext() {
        if (annotationConfigApplicationContext != null) {
            annotationConfigApplicationContext.stop();
        }
    }

    protected static <T> T getBean(Class<T> requiredType) {
        return annotationConfigApplicationContext.getBean(requiredType);
    }

}
