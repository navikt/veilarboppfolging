package no.nav.veilarboppfolging.kafka;

import no.nav.common.utils.Credentials;
import no.nav.veilarboppfolging.config.EnvironmentProperties;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.LoggingProducerListener;

import java.util.HashMap;

import static no.nav.veilarboppfolging.utils.KafkaUtils.requireKafkaTopicPrefix;

@EnableKafka
@Configuration
public class KafkaConfig {

    private final KafkaConsumerHealthCheck kafkaConsumerHealthCheck;

    private final Credentials serviceUserCredentials;

    private final String brokersUrl;

    @Autowired
    public KafkaConfig(EnvironmentProperties properties, KafkaConsumerHealthCheck kafkaConsumerHealthCheck, Credentials serviceUserCredentials) {
        brokersUrl = properties.getKafkaBrokersUrl();
        this.kafkaConsumerHealthCheck = kafkaConsumerHealthCheck;
        this.serviceUserCredentials = serviceUserCredentials;
    }

    @Bean
    public KafkaTopics kafkaTopics() {
        return KafkaTopics.create(requireKafkaTopicPrefix());
    }

    @Bean
    public KafkaListenerContainerFactory kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(kafkaConsumerProperties(brokersUrl, serviceUserCredentials)));
        factory.setErrorHandler(kafkaConsumerHealthCheck);
        return factory;
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        ProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(kafkaProducerProperties(brokersUrl, serviceUserCredentials));
        KafkaTemplate<String, String> template = new KafkaTemplate<>(producerFactory);
        LoggingProducerListener<String, String> producerListener = new LoggingProducerListener<>();
        producerListener.setIncludeContents(false);
        template.setProducerListener(producerListener);
        return template;
    }

    private static HashMap<String, Object> kafkaBaseProperties(String kafkaBrokersUrl, Credentials serviceUserCredentials) {
        HashMap<String, Object> props = new HashMap<>();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokersUrl);
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        props.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        props.put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" + serviceUserCredentials.username + "\" password=\"" + serviceUserCredentials.password + "\";");
        return props;
    }

    private static HashMap<String, Object> kafkaProducerProperties (String kafkaBrokersUrl, Credentials serviceUserCredentials) {
        HashMap<String, Object> props = kafkaBaseProperties(kafkaBrokersUrl, serviceUserCredentials);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 3000);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "veilarboppfolging-producer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return props;
    }

    private static HashMap<String, Object> kafkaConsumerProperties(String kafkaBrokersUrl, Credentials serviceUserCredentials) {
        HashMap<String, Object> props = kafkaBaseProperties(kafkaBrokersUrl, serviceUserCredentials);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "veilarboppfolging-consumer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 150_000);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1000);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return props;
    }

}
