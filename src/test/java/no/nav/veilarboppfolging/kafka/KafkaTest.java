package no.nav.veilarboppfolging.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;

import java.util.concurrent.BlockingQueue;

public abstract class KafkaTest {

    protected static String RECEIVER_TOPIC = "receiver.t";

    private static AnnotationConfigApplicationContext annotationConfigApplicationContext;

    protected static KafkaTemplate<String, String> kafkaTemplate;

    private static KafkaMessageListenerContainer<String, String> container;
    protected static BlockingQueue<ConsumerRecord<String, String>> records;

//    @ClassRule
//    public static KafkaEmbedded embeddedKafka = new KafkaEmbedded(1, true, RECEIVER_TOPIC);

    @BeforeClass
    public static void configureKafkaBroker() {
//        Map<String, Object> consumerProperties = KafkaTestUtils.consumerProps("template", "false", embeddedKafka);
//        DefaultKafkaConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProperties);
//        ContainerProperties containerProperties = new ContainerProperties(RECEIVER_TOPIC);
//
//        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
//        records = new LinkedBlockingQueue<>();
//        container.setupMessageListener((MessageListener<String, String>) record -> {
//            records.add(record);
//        });
//        container.start();
//        ContainerTestUtils.waitForAssignment(container, embeddedKafka.getPartitionsPerTopic());
//
//        Map<String, Object> kafkaProps = producerProps(embeddedKafka);
//        kafkaProps.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
//
//        kafkaTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(kafkaProps));
//        kafkaTemplate.setDefaultTopic(RECEIVER_TOPIC);
//
//        setProperty(KAFKA_BROKERS_PROPERTY, embeddedKafka.getBrokersAsString(), PUBLIC);
    }

    @BeforeClass
    public static void setupFelles() {
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
