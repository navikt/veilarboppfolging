package no.nav.fo.veilarboppfolging.kafka;

import no.nav.fo.veilarboppfolging.db.AvsluttOppfolgingEndringRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.LoggingProducerListener;

import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;
import static no.nav.sbl.util.EnvironmentUtils.requireEnvironmentName;

@Configuration
public class ProducerConfig {

    private static final String KAFKA_PRODUCER_TOPIC = "aapen-fo-endringPaaAvsluttOppfolging-v1" + "-" + getRequiredProperty("APP_ENVIRONMENT_NAME");

    static ProducerFactory<String, String> producerFactory() {
        return new DefaultKafkaProducerFactory<>(KafkaPropsConfig.kafkaProducerProperties());
    }

    @Bean
    public AvsluttOppfolgingProducer avsluttOppfolgingProducer(AvsluttOppfolgingEndringRepository avsluttOppfolgingEndringRepository) {
        KafkaTemplate<String, String> kafkaTemplate =  new KafkaTemplate<>(producerFactory());
        LoggingProducerListener<String, String> producerListener = new LoggingProducerListener<>();
        producerListener.setIncludeContents(false);
        kafkaTemplate.setProducerListener(producerListener);
        return new AvsluttOppfolgingProducer(kafkaTemplate, avsluttOppfolgingEndringRepository, KAFKA_PRODUCER_TOPIC);
    }


}
