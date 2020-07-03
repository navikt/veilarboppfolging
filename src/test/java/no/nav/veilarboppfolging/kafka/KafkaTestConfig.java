package no.nav.veilarboppfolging.kafka;

import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolging;
import no.nav.veilarboppfolging.services.IservService;
import no.nav.veilarboppfolging.services.OppfolgingsenhetEndringService;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;

import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

import java.util.HashMap;

import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG;
import static org.mockito.Mockito.mock;

@EnableKafka
public class KafkaTestConfig {

    public static final String KAFKA_TEST_TOPIC = "test-topic";

//    @Bean
//    public Consumer.ConsumerParameters consumerParameters() {
//        return new Consumer.ConsumerParameters(KAFKA_TEST_TOPIC);
//    }

    @Bean
    public IservService iserv28Service() {
        return mock(IservService.class);
    }

    @Bean
    public OppfolgingsenhetEndringService oppfolgingsenhetEndringService() {
        return mock(OppfolgingsenhetEndringService.class);
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, VeilarbArenaOppfolging>> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, VeilarbArenaOppfolging> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

    static DefaultKafkaConsumerFactory consumerFactory () {
        HashMap<String, Object> props = new HashMap<>();
//        props.put(BOOTSTRAP_SERVERS_CONFIG, getRequiredProperty(KAFKA_BROKERS_PROPERTY));
        props.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(GROUP_ID_CONFIG, "veilarboppfolging-consumer");
        props.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(MAX_POLL_INTERVAL_MS_CONFIG, 10);
        return new DefaultKafkaConsumerFactory<>(props);
    }
}
