package no.nav.veilarboppfolging.kafka;

import lombok.val;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.veilarboppfolging.db.AvsluttOppfolgingEndringRepository;
import no.nav.veilarboppfolging.db.OppfolgingFeedRepository;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.LoggingProducerListener;

import java.util.HashMap;

import static no.nav.veilarboppfolging.config.ApplicationConfig.APP_ENVIRONMENT_NAME;
import static no.nav.veilarboppfolging.kafka.KafkaPropsConfig.kafkaProducerProperties;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;
import static no.nav.sbl.util.EnvironmentUtils.requireEnvironmentName;
import static org.apache.kafka.clients.producer.ProducerConfig.*;

@Configuration
public class ProducerConfig {

    public static final String TOPIC_AVSLUTT_OPPFOLGING = "aapen-fo-endringPaaAvsluttOppfolging-v1" + "-" + getRequiredProperty(APP_ENVIRONMENT_NAME);
    public static final String TOPIC_OPPFOLGING_STATUS = "aapen-fo-endringPaaOppfolgingStatus-v1-" + requireEnvironmentName();

    static ProducerFactory<String, String> producerFactory() {
        return new DefaultKafkaProducerFactory<>(kafkaProducerProperties());
    }

    public static KafkaProducer<String, String> createKafkaProducer() {
        return new KafkaProducer<>(kafkaProducerProperties());
    }

    @Bean
    public AvsluttOppfolgingProducer avsluttOppfolgingProducer(AvsluttOppfolgingEndringRepository avsluttOppfolgingEndringRepository) {
        KafkaTemplate<String, String> kafkaTemplate =  new KafkaTemplate<>(producerFactory());
        LoggingProducerListener<String, String> producerListener = new LoggingProducerListener<>();
        producerListener.setIncludeContents(false);
        kafkaTemplate.setProducerListener(producerListener);
        return new AvsluttOppfolgingProducer(kafkaTemplate, avsluttOppfolgingEndringRepository, TOPIC_AVSLUTT_OPPFOLGING);
    }

    @Bean
    public OppfolgingStatusKafkaProducer oppfolgingStatusProducer(OppfolgingFeedRepository repository, AktorService aktorService) {
        HashMap<String, Object> config = kafkaProducerProperties();

        config.put(ACKS_CONFIG, "0");                  // Fire-and-forget, we do not care about acks when hydrating
        config.put(BATCH_SIZE_CONFIG, 25*1024*1000);   // 25MiB Buffer
        config.put(MAX_BLOCK_MS_CONFIG, 60*20*1000);   // 20s timeout when buffer is full or requesting metadata for topic
        config.put(REQUEST_TIMEOUT_MS_CONFIG, 1000);   // 1s timeout for waiting on reply from server

        val kafkaProducer = new KafkaProducer<String, String>(config);
        return new OppfolgingStatusKafkaProducer(kafkaProducer, repository, aktorService, TOPIC_OPPFOLGING_STATUS);
    }
}
