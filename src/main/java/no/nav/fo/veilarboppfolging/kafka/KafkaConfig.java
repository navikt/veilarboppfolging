package no.nav.fo.veilarboppfolging.kafka;

import no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;
import static no.nav.sbl.util.EnvironmentUtils.requireEnvironmentName;
import static org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;

@Configuration
public class KafkaConfig {

    private static final String KAFKA_BROKERS_URL_PROPERTY = "KAFKA_BROKERS_URL";
    private static final String KAFKA_BROKERS = getRequiredProperty(KAFKA_BROKERS_URL_PROPERTY);
    private static final String USERNAME = getRequiredProperty(StsSecurityConstants.SYSTEMUSER_USERNAME);
    private static final String PASSWORD = getRequiredProperty(StsSecurityConstants.SYSTEMUSER_PASSWORD);


    @Bean
    public Consumer.ConsumerParameters consumerParameters() {
        return new Consumer.ConsumerParameters("aapen-fo-endringPaaOppfoelgingsBruker-v1-" + requireEnvironmentName());
    }

    @Bean
    public ConsumerConfig.SASL sasl() {
        return ConsumerConfig.SASL.ENABLED;
    }

    private static HashMap<String, Object> kafkaProperties () {
        HashMap<String, Object> props = new HashMap<>();
        props.put(BOOTSTRAP_SERVERS_CONFIG, KAFKA_BROKERS);
        props.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        props.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        props.put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" + USERNAME + "\" password=\"" + PASSWORD + "\";");
        return props;
    }

    static HashMap<String, Object> producerProperties () {
        HashMap<String, Object>  props = kafkaProperties();
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "veilarboppfolging-producer");
        return props;
    }

}
