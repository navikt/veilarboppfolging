package no.nav.fo.veilarboppfolging.kafka;

import no.nav.fo.veilarboppfolging.mappers.ArenaBruker;
import no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

import java.util.HashMap;

import static no.nav.fo.veilarboppfolging.config.ApplicationConfig.KAFKA_BROKERS_PROPERTY;
import static no.nav.fo.veilarboppfolging.kafka.ConsumerConfig.SASL.DISABLED;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;
import static no.nav.sbl.util.EnvironmentUtils.requireEnvironmentName;
import static org.apache.kafka.clients.consumer.ConsumerConfig.*;

@Configuration
@Import({ KafkaHelsesjekk.class })
@EnableKafka
public class ConsumerConfig {


    private final SASL sasl;

    public ConsumerConfig(SASL sasl) {
        this.sasl = sasl;
    }

    public enum SASL {
        ENABLED,
        DISABLED
    }

    static String getBrokerUrls() {
        return getRequiredProperty(KAFKA_BROKERS_PROPERTY);
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, ArenaBruker>> kafkaListenerContainerFactory(KafkaHelsesjekk kafkaHelsesjekk) {
        ConcurrentKafkaListenerContainerFactory<String, ArenaBruker> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setErrorHandler(kafkaHelsesjekk);
        return factory;
    }

    @Bean
    public Consumer.ConsumerParameters consumerParameters() {
        return new Consumer.ConsumerParameters("aapen-fo-endringPaaOppfoelgingsBruker-v1-" + requireEnvironmentName());
    }

    @Bean
    public ConsumerConfig.SASL sasl() {
        return ConsumerConfig.SASL.ENABLED;
    }

    private ConsumerFactory<String, ArenaBruker> consumerFactory() {
        HashMap<String, Object> props = new HashMap<>();
        props.put(BOOTSTRAP_SERVERS_CONFIG, getBrokerUrls());
        props.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(GROUP_ID_CONFIG, "veilarboppfolging-consumer");
        props.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(MAX_POLL_INTERVAL_MS_CONFIG, 5000);

        if (sasl != DISABLED) {
            String username = getRequiredProperty(StsSecurityConstants.SYSTEMUSER_USERNAME);
            String password = getRequiredProperty(StsSecurityConstants.SYSTEMUSER_PASSWORD);
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
            props.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
            props.put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" + username + "\" password=\"" + password + "\";");
        }

        return new DefaultKafkaConsumerFactory<>(props);
    }

}
