package no.nav.veilarboppfolging.config;

import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.veilarboppfolging.kafka.AvsluttOppfolgingProducer;
import no.nav.veilarboppfolging.kafka.KafkaTopics;
import no.nav.veilarboppfolging.kafka.OppfolgingStatusKafkaProducer;
import no.nav.veilarboppfolging.repository.OppfolgingFeedRepository;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.LoggingProducerListener;
import org.springframework.kafka.test.EmbeddedKafkaBroker;

import java.util.HashMap;

import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;

@EnableKafka
@Configuration
@Import({
        AvsluttOppfolgingProducer.class
})
public class KafkaTestConfig {

    private final KafkaTopics kafkaTopics;

    @Autowired
    public KafkaTestConfig(KafkaTopics kafkaTopics) {
        this.kafkaTopics = kafkaTopics;
    }

    @Bean
    public OppfolgingStatusKafkaProducer oppfolgingStatusKafkaProducer(
            EmbeddedKafkaBroker embeddedKafkaBroker,
            OppfolgingFeedRepository oppfolgingFeedRepository,
            AktorregisterClient aktorregisterClient
    ) {
        KafkaProducer kafkaProducer = new KafkaProducer<String, String>(producerConfig(embeddedKafkaBroker.getBrokersAsString()));
        return new OppfolgingStatusKafkaProducer(kafkaProducer, oppfolgingFeedRepository, aktorregisterClient, kafkaTopics.getEndringPaaOppfolgingStatus());
    }

    @Bean
    public EmbeddedKafkaBroker embeddedKafkaBroker() {
        EmbeddedKafkaBroker embeddedKafkaBroker = new EmbeddedKafkaBroker(1, true, kafkaTopics.getAllTopics());
        return embeddedKafkaBroker;
    }

    @Bean
    public KafkaListenerContainerFactory kafkaListenerContainerFactory(EmbeddedKafkaBroker embeddedKafkaBroker) {
        ConcurrentKafkaListenerContainerFactory factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory(embeddedKafkaBroker.getBrokersAsString()));
        return factory;
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(EmbeddedKafkaBroker embeddedKafkaBroker) {
        KafkaTemplate<String, String> template = new KafkaTemplate<>(producerFactory(embeddedKafkaBroker.getBrokersAsString()));
        LoggingProducerListener<String, String> producerListener = new LoggingProducerListener<>();
        producerListener.setIncludeContents(false);
        template.setProducerListener(producerListener);
        return template;
    }

    private static DefaultKafkaConsumerFactory consumerFactory(String kafkaBrokersUrl) {
        HashMap<String, Object> props = new HashMap<>();
        props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaBrokersUrl);
        props.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(GROUP_ID_CONFIG, "veilarboppfolging-consumer");
        props.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(MAX_POLL_INTERVAL_MS_CONFIG, 1000);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    private static ProducerFactory<String, String> producerFactory(String kafkaBrokersUrl) {
        return new DefaultKafkaProducerFactory<>(producerConfig(kafkaBrokersUrl));
    }

    private static HashMap<String, Object> producerConfig(String kafkaBrokersUrl) {
        HashMap<String, Object> props = new HashMap<>();
        props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaBrokersUrl);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "veilarboppfolging-producer");
        props.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return props;
    }
}
