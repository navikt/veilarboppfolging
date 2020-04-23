package no.nav.fo.veilarboppfolging.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static no.nav.fo.veilarboppfolging.kafka.KafkaPropsConfig.KAFKA_BROKERS;
import static no.nav.fo.veilarboppfolging.kafka.ProducerConfig.KAFKA_PRODUCER_TOPIC_OPPFOLGING;


@Slf4j
@Component
public class OppfolgingKafkaTopicHelsesjekk implements Helsesjekk {

    KafkaProducer<String, String> kafkaProducer;

    public OppfolgingKafkaTopicHelsesjekk(KafkaProducer<String, String> kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    @Override
    public void helsesjekk() {
        kafkaProducer.partitionsFor(KAFKA_PRODUCER_TOPIC_OPPFOLGING);
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return new HelsesjekkMetadata(KAFKA_PRODUCER_TOPIC_OPPFOLGING, KAFKA_BROKERS, "Kafka-topic for endring på oppfølging for en bruker", false);
    }

}
