package no.nav.fo.veilarboppfolging.kafka;

import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.AvsluttOppfolgingEndringRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingFeedRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingKafkaFeiletMeldingRepository;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.LoggingProducerListener;

import static no.nav.fo.veilarboppfolging.config.ApplicationConfig.APP_ENVIRONMENT_NAME;
import static no.nav.fo.veilarboppfolging.kafka.KafkaPropsConfig.kafkaProducerProperties;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;
import static no.nav.sbl.util.EnvironmentUtils.requireEnvironmentName;

@Configuration
public class ProducerConfig {

    static final String KAFKA_PRODUCER_TOPIC_AVSLUTT_OPPFOLGING = "aapen-fo-endringPaaAvsluttOppfolging-v1" + "-" + getRequiredProperty(APP_ENVIRONMENT_NAME);
    static final String KAFKA_PRODUCER_TOPIC_OPPFOLGING = "aapen-fo-oppfolgingOppdatert-v1-" + requireEnvironmentName();

    static ProducerFactory<String, String> producerFactory() {
        return new DefaultKafkaProducerFactory<>(kafkaProducerProperties());
    }

    static KafkaProducer<String, String> kafkaProducer() {
        return new KafkaProducer<>(kafkaProducerProperties());
    }

    @Bean
    public AvsluttOppfolgingProducer avsluttOppfolgingProducer(AvsluttOppfolgingEndringRepository avsluttOppfolgingEndringRepository) {
        KafkaTemplate<String, String> kafkaTemplate =  new KafkaTemplate<>(producerFactory());
        LoggingProducerListener<String, String> producerListener = new LoggingProducerListener<>();
        producerListener.setIncludeContents(false);
        kafkaTemplate.setProducerListener(producerListener);
        return new AvsluttOppfolgingProducer(kafkaTemplate, avsluttOppfolgingEndringRepository, KAFKA_PRODUCER_TOPIC_AVSLUTT_OPPFOLGING);
    }

    @Bean
    public OppfolgingKafkaProducer oppfolgingStatusProducer(OppfolgingFeedRepository repository, OppfolgingKafkaFeiletMeldingRepository feiletMeldingRepository,  AktorService aktorService) {
        return new OppfolgingKafkaProducer(kafkaProducer(), repository, feiletMeldingRepository, aktorService, KAFKA_PRODUCER_TOPIC_OPPFOLGING);
    }

    @Bean
    public OppfolgingKafkaTopicHelsesjekk oppfolgingKafkaHelsesjekk() {
        return new OppfolgingKafkaTopicHelsesjekk(kafkaProducer());
    }

}
