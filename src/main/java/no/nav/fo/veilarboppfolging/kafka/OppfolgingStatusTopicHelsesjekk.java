package no.nav.fo.veilarboppfolging.kafka;

import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;

import static no.nav.fo.veilarboppfolging.kafka.KafkaPropsConfig.KAFKA_BROKERS;
import static no.nav.fo.veilarboppfolging.kafka.ProducerConfig.TOPIC_OPPFOLGING_STATUS;


public class OppfolgingStatusTopicHelsesjekk implements Helsesjekk {

    KafkaProducer<String, String> kafkaProducer;

    public OppfolgingStatusTopicHelsesjekk(KafkaProducer<String, String> kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    @Override
    public void helsesjekk() {
        kafkaProducer.partitionsFor(TOPIC_OPPFOLGING_STATUS);
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return new HelsesjekkMetadata(TOPIC_OPPFOLGING_STATUS, KAFKA_BROKERS, "Kafka-topic for endring på oppfølgingstatus for en bruker", false);
    }

}
