package no.nav.fo.veilarboppfolging.kafka;

import no.nav.fo.veilarboppfolging.config.DatabaseConfig;
import no.nav.fo.veilarboppfolging.config.UnleashTestConfig;
import org.junit.*;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.listener.config.ContainerProperties;
import org.springframework.kafka.test.rule.KafkaEmbedded;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Map;

import static no.nav.fo.veilarboppfolging.config.DatabaseConfig.*;
import static no.nav.fo.veilarboppfolging.db.testdriver.TestDriver.createInMemoryDatabaseUrl;
import static no.nav.sbl.dialogarena.test.SystemProperties.setTemporaryProperty;
import static no.nav.sbl.util.EnvironmentUtils.Type.PUBLIC;
import static no.nav.sbl.util.EnvironmentUtils.setProperty;
import static org.springframework.kafka.test.utils.KafkaTestUtils.producerProps;

public abstract class KafkaTest {

    private static String RECEIVER_TOPIC = "receiver.t";

    private static AnnotationConfigApplicationContext annotationConfigApplicationContext;
    private static TransactionStatus transaction;
    private static PlatformTransactionManager platformTransactionManager;

    protected static KafkaTemplate<String, String> kafkaTemplate;

    private static KafkaMessageListenerContainer<String, String> container;

    @ClassRule
    public static KafkaEmbedded embeddedKafka = new KafkaEmbedded(1, true, RECEIVER_TOPIC);

    @BeforeClass
    public static void configureKafkaBroker() throws Exception {
        Map<String, Object> consumerProperties = KafkaTestUtils.consumerProps("template", "false", embeddedKafka);
        DefaultKafkaConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProperties);
        ContainerProperties containerProperties = new ContainerProperties(RECEIVER_TOPIC);

        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        container.setupMessageListener((MessageListener<String, String>) record -> {
            System.out.println("KafkaMessage: " + record.toString());
        });
        container.start();
        ContainerTestUtils.waitForAssignment(container, embeddedKafka.getPartitionsPerTopic());

        kafkaTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps(embeddedKafka)));
        kafkaTemplate.setDefaultTopic(RECEIVER_TOPIC);

        setProperty(ConsumerConfig.KAFKA_BROKERS_URL_PROPERTY_NAME, embeddedKafka.getBrokersAsString(), PUBLIC);
    }

    public KafkaTest addListener(MessageListener<String, String> listener) {
        container.setupMessageListener(listener);
        return this;
    }

    @BeforeClass
    public static void setupFelles() throws IOException {
        setTemporaryProperty(VEILARBOPPFOLGINGDB_URL_PROPERTY, createInMemoryDatabaseUrl(), () -> {
            setTemporaryProperty(VEILARBOPPFOLGINGDB_USERNAME_PROPERTY, "sa", () -> {
                setTemporaryProperty(VEILARBOPPFOLGINGDB_PASSWORD_PROPERTY, "pw", () -> {
                    annotationConfigApplicationContext = new AnnotationConfigApplicationContext(
                            DatabaseConfig.class,
                            KafkaTestConfig.class,
                            UnleashTestConfig.class
                    );
                    annotationConfigApplicationContext.start();
                    platformTransactionManager = getBean(PlatformTransactionManager.class);
                    migrateDatabase(getBean(DataSource.class));
                });
            });
        });
    }

    @Before
    public void injectAvhengigheter() {
        annotationConfigApplicationContext.getAutowireCapableBeanFactory().autowireBean(this);
    }

    @Before
    public void beginTransaction() {
        transaction = platformTransactionManager.getTransaction(new TransactionTemplate());
    }

    @After
    public void rollbackTransaction() {
        platformTransactionManager.rollback(transaction);
        transaction = null;
    }

    @AfterClass
    public static void stopSpringContext() {
        if (annotationConfigApplicationContext != null) {
            annotationConfigApplicationContext.stop();
        }
    }

    @After
    public void tearDown() {
        if (container != null) {
            container.stop();
        }
    }

    protected static <T> T getBean(Class<T> requiredType) {
        return annotationConfigApplicationContext.getBean(requiredType);
    }

}
