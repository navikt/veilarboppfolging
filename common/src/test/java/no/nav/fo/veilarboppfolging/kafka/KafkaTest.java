package no.nav.fo.veilarboppfolging.kafka;

import no.nav.dialogarena.config.DevelopmentSecurity;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.veilarboppfolging.config.*;
import org.junit.*;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.listener.config.ContainerProperties;
import org.springframework.kafka.test.rule.KafkaEmbedded;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import javax.naming.NamingException;
import java.io.IOException;
import java.util.Map;

import static no.nav.fo.veilarboppfolging.config.ApplicationConfig.APPLICATION_NAME;
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

        System.setProperty(ConsumerConfig.KAFKA_BROKERS_URL_PROPERTY_NAME, embeddedKafka.getBrokersAsString());
    }

    public KafkaTest addListener(MessageListener<String, String> listener) {
        container.setupMessageListener(listener);
        return this;
    }

    @BeforeClass
    public static void setupFelles() throws IOException {
        DevelopmentSecurity.setupIntegrationTestSecurity(FasitUtils.getServiceUser("srvveilarboppfolging", APPLICATION_NAME));
        JndiLocalContextConfig.setupInMemoryDatabase();
        annotationConfigApplicationContext = new AnnotationConfigApplicationContext(
                IntegrasjonsTest.JndiBean.class,
                DatabaseConfig.class,
                PepConfig.class,
                KafkaTestConfig.class
        );
        annotationConfigApplicationContext.start();
        platformTransactionManager = getBean(PlatformTransactionManager.class);
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

    @Before
    public final void fiksJndiOgLdapKonflikt() throws NamingException {
        getBean(IntegrasjonsTest.JndiBean.class).builder.deactivate();
    }

}
