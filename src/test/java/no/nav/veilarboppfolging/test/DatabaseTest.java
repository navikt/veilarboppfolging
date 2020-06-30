package no.nav.veilarboppfolging.test;

import no.nav.apiapp.security.PepClient;
import no.nav.veilarboppfolging.config.DatabaseConfig;
import no.nav.veilarboppfolging.config.DatabaseRepositoryConfig;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.io.IOException;

import static no.nav.veilarboppfolging.config.DatabaseConfig.*;
import static no.nav.veilarboppfolging.db.testdriver.TestDriver.createInMemoryDatabaseUrl;
import static no.nav.sbl.dialogarena.test.SystemProperties.setTemporaryProperty;
import static org.mockito.Mockito.mock;

public abstract class DatabaseTest {

    protected static AnnotationConfigApplicationContext annotationConfigApplicationContext;
    private static TransactionStatus transaction;
    private static PlatformTransactionManager platformTransactionManager;
    private static PepClient pepClient = mock(PepClient.class);

    @BeforeAll
    @BeforeClass
    public static void setupFelles() throws IOException {
        setTemporaryProperty(VEILARBOPPFOLGINGDB_URL_PROPERTY, createInMemoryDatabaseUrl(), () -> {
            setTemporaryProperty(VEILARBOPPFOLGINGDB_USERNAME_PROPERTY, "sa", () -> {
                setTemporaryProperty(VEILARBOPPFOLGINGDB_PASSWORD_PROPERTY, "pw", () -> {
                    annotationConfigApplicationContext = new AnnotationConfigApplicationContext(
                            DatabaseConfig.class,
                            DatabaseRepositoryConfig.class,
                            PepClientMockConfig.class
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
    public void resetPepClient() {
        Mockito.reset(pepClient);
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

    @Configuration
    public static class PepClientMockConfig {

        @Bean
        public PepClient pepClient(){
            return pepClient;
        }
    }

}
