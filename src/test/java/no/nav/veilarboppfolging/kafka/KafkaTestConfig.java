package no.nav.veilarboppfolging.kafka;

import no.nav.veilarboppfolging.utils.mappers.VeilarbArenaOppfolging;
import no.nav.veilarboppfolging.services.Iserv28Service;
import no.nav.veilarboppfolging.services.OppfolgingsenhetEndringService;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;

import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

import java.util.HashMap;

import static no.nav.veilarboppfolging.config.ApplicationConfig.KAFKA_BROKERS_PROPERTY;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;
import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG;
import static org.mockito.Mockito.mock;

@Import({Consumer.class})
@EnableKafka
public class KafkaTestConfig {

    public static final String KAFKA_TEST_TOPIC = "test-topic";

    @Bean
    public Consumer.ConsumerParameters consumerParameters() {
        return new Consumer.ConsumerParameters(KAFKA_TEST_TOPIC);
    }

    @Bean
    public Iserv28Service iserv28Service() {
        return mock(Iserv28Service.class);
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
        props.put(BOOTSTRAP_SERVERS_CONFIG, getRequiredProperty(KAFKA_BROKERS_PROPERTY));
        props.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(GROUP_ID_CONFIG, "veilarboppfolging-consumer");
        props.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(MAX_POLL_INTERVAL_MS_CONFIG, 10);
        return new DefaultKafkaConsumerFactory<>(props);
    }
}
