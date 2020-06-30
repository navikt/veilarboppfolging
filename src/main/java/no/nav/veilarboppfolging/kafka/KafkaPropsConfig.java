package no.nav.veilarboppfolging.kafka;

import no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.HashMap;

import static no.nav.veilarboppfolging.config.ApplicationConfig.KAFKA_BROKERS_URL_PROPERTY;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;
import static org.apache.kafka.clients.CommonClientConfigs.REQUEST_TIMEOUT_MS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.*;
import static org.apache.kafka.clients.producer.ProducerConfig.CLIENT_ID_CONFIG;

public class KafkaPropsConfig {
    public static final String KAFKA_BROKERS = getRequiredProperty(KAFKA_BROKERS_URL_PROPERTY);
    private static final String USERNAME = getRequiredProperty(StsSecurityConstants.SYSTEMUSER_USERNAME);
    private static final String PASSWORD = getRequiredProperty(StsSecurityConstants.SYSTEMUSER_PASSWORD);

    static HashMap<String, Object> kafkaProducerProperties () {
        HashMap<String, Object> props = new HashMap<>();
        props.put(BOOTSTRAP_SERVERS_CONFIG, KAFKA_BROKERS);
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        props.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        props.put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" + USERNAME + "\" password=\"" + PASSWORD + "\";");
        props.put(ACKS_CONFIG, "all");
        props.put(REQUEST_TIMEOUT_MS_CONFIG, 3000);
        props.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(CLIENT_ID_CONFIG, "veilarboppfolging-producer");
        return props;
    }

    static HashMap<String, Object> kafkaConsumerProperties () {
        HashMap<String, Object> props = new HashMap<>();
        props.put(BOOTSTRAP_SERVERS_CONFIG, KAFKA_BROKERS);
        props.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(GROUP_ID_CONFIG, "veilarboppfolging-consumer");
        props.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(MAX_POLL_INTERVAL_MS_CONFIG, 150_000);
        props.put(MAX_POLL_RECORDS_CONFIG, 1000);
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        props.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        props.put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" + USERNAME + "\" password=\"" + PASSWORD + "\";");
        return props;
    }
}
