package no.nav.fo.veilarboppfolging.kafka;

import lombok.val;
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

import java.util.HashMap;

import static no.nav.fo.veilarboppfolging.config.ApplicationConfig.APP_ENVIRONMENT_NAME;
import static no.nav.fo.veilarboppfolging.kafka.KafkaPropsConfig.kafkaProducerProperties;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;
import static no.nav.sbl.util.EnvironmentUtils.requireEnvironmentName;
import static org.apache.kafka.clients.producer.ProducerConfig.*;

@Configuration
public class ProducerConfig {

    static final String KAFKA_PRODUCER_TOPIC_AVSLUTT_OPPFOLGING = "aapen-fo-endringPaaAvsluttOppfolging-v1" + "-" + getRequiredProperty(APP_ENVIRONMENT_NAME);
    static final String KAFKA_PRODUCER_TOPIC_OPPFOLGING = "aapen-fo-oppfolgingOppdatert-v1-" + requireEnvironmentName();

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
        return new AvsluttOppfolgingProducer(kafkaTemplate, avsluttOppfolgingEndringRepository, KAFKA_PRODUCER_TOPIC_AVSLUTT_OPPFOLGING);
    }

    @Bean
    public OppfolgingKafkaProducer oppfolgingStatusProducer(OppfolgingFeedRepository repository, OppfolgingKafkaFeiletMeldingRepository feiletMeldingRepository,  AktorService aktorService) {
        HashMap<String, Object> config = kafkaProducerProperties();

        config.put(ACKS_CONFIG, "1");                  // Leader will now wait for ACKs from all followers
        config.put(BATCH_SIZE_CONFIG, 1024*300);     // 300KiB batch size
        config.put(MAX_BLOCK_MS_CONFIG, 60*10_000);  // 10s timeout

        val kafkaProducer = new KafkaProducer<String, String>(config);
        return new OppfolgingKafkaProducer(kafkaProducer, repository, feiletMeldingRepository, aktorService, KAFKA_PRODUCER_TOPIC_OPPFOLGING);
    }
}
