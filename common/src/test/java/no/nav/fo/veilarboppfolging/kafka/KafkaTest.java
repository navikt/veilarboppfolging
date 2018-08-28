package no.nav.fo.veilarboppfolging.kafka;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.listener.config.ContainerProperties;
import org.springframework.kafka.test.rule.KafkaEmbedded;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.util.Map;

import static org.springframework.kafka.test.utils.KafkaTestUtils.producerProps;

public abstract  class KafkaTest {

    private static String RECEIVER_TOPIC = "receiver.t";

    private KafkaMessageListenerContainer<String, String> container;
    public KafkaTemplate<String, String> template;

    @ClassRule
    public static KafkaEmbedded embeddedKafka = new KafkaEmbedded(1, true, RECEIVER_TOPIC);

    @Before
    public void setUp() throws Exception {
        Map<String, Object> consumerProperties = KafkaTestUtils.consumerProps("template", "false", embeddedKafka);
        DefaultKafkaConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProperties);
        ContainerProperties containerProperties = new ContainerProperties(RECEIVER_TOPIC);

        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        container.setupMessageListener((MessageListener<String, String>) record -> {
            System.out.println("KafkaMessage: " + record.toString());
        });
        container.start();
        ContainerTestUtils.waitForAssignment(container, embeddedKafka.getPartitionsPerTopic());

        template = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps(embeddedKafka)));
        template.setDefaultTopic(RECEIVER_TOPIC);
    }

    public KafkaTest addListener(MessageListener<String, String> listener) {
        this.container.setupMessageListener(listener);
        return this;
    }

    @After
    public void tearDown() {
        container.stop();
    }

}
