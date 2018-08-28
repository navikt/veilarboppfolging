package no.nav.fo.veilarboppfolging.kafka;

import no.nav.fo.veilarboppfolging.mappers.ArenaBruker;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.*;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

import java.util.HashMap;
import java.util.Map;

import static org.apache.kafka.clients.consumer.ConsumerConfig.*;

@Configuration
@EnableAspectJAutoProxy
public class ConsumerConfig
{
    public static final String KAFKA_TOPIC = System.getProperty("endring.bruker.topic");
    private static final String KAFKA_BROKERS = System.getProperty("kafka-brokers.url");

    @Bean
    public static Map<String, Object> consumerConfigs() {
        HashMap<String, Object> props = new HashMap<>();
        props.put(BOOTSTRAP_SERVERS_CONFIG, KAFKA_BROKERS);
        props.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(GROUP_ID_CONFIG, "veilarboppfolging-consumer");
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, ArenaBruker>> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ArenaBruker> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, ArenaBruker> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerConfigs());
    }

    @Bean
    public Consumer consumer() {
        return new Consumer();
    }
}
