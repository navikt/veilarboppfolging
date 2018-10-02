package no.nav.fo.veilarboppfolging.kafka;

import no.nav.fo.veilarboppfolging.mappers.ArenaBruker;
import no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

import java.util.HashMap;

import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;
import static org.apache.kafka.clients.consumer.ConsumerConfig.*;

@Configuration
@EnableKafka
public class ConsumerConfig {

    public static final String KAFKA_BROKERS_URL_PROPERTY_NAME = "kafka-brokers.url";
    private static final String USERNAME = getRequiredProperty(StsSecurityConstants.SYSTEMUSER_USERNAME);
    private static final String PASSWORD = getRequiredProperty(StsSecurityConstants.SYSTEMUSER_PASSWORD);

    static String getBrokerUrls() {
        return getRequiredProperty(KAFKA_BROKERS_URL_PROPERTY_NAME);
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, ArenaBruker>> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ArenaBruker> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setErrorHandler(new KafkaErrorHandler());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, ArenaBruker> consumerFactory() {
        HashMap<String, Object> props = new HashMap<>();
        props.put(BOOTSTRAP_SERVERS_CONFIG, getBrokerUrls());
        props.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(GROUP_ID_CONFIG, "veilarboppfolging-consumer");
        props.put(AUTO_OFFSET_RESET_CONFIG, "earliest");

        props.put(MAX_POLL_INTERVAL_MS_CONFIG,5000);

        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        props.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        props.put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" + USERNAME + "\" password=\"" + PASSWORD + "\";");

        DefaultKafkaConsumerFactory<String, ArenaBruker> stringArenaBrukerDefaultKafkaConsumerFactory = new DefaultKafkaConsumerFactory<>(props);

        return stringArenaBrukerDefaultKafkaConsumerFactory;
    }

}
