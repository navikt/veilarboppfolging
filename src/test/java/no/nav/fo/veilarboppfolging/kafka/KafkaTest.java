package no.nav.fo.veilarboppfolging.kafka;

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

import java.io.IOException;
import java.util.Map;

import static no.nav.fo.veilarboppfolging.config.ApplicationConfig.KAFKA_BROKERS_PROPERTY;
import static no.nav.sbl.util.EnvironmentUtils.Type.PUBLIC;
import static no.nav.sbl.util.EnvironmentUtils.setProperty;
import static org.springframework.kafka.test.utils.KafkaTestUtils.producerProps;

public abstract class KafkaTest {

    private static String RECEIVER_TOPIC = "receiver.t";

    private static AnnotationConfigApplicationContext annotationConfigApplicationContext;

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

        setProperty(KAFKA_BROKERS_PROPERTY, embeddedKafka.getBrokersAsString(), PUBLIC);
    }

    public KafkaTest addListener(MessageListener<String, String> listener) {
        container.setupMessageListener(listener);
        return this;
    }

    @BeforeClass
    public static void setupFelles() throws IOException {
        annotationConfigApplicationContext = new AnnotationConfigApplicationContext(KafkaTestConfig.class);
        annotationConfigApplicationContext.start();
    }

    @Before
    public void injectAvhengigheter() {
        annotationConfigApplicationContext.getAutowireCapableBeanFactory().autowireBean(this);
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

}
