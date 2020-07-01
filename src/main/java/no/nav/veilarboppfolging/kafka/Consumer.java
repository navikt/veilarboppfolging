package no.nav.veilarboppfolging.kafka;

import no.nav.veilarboppfolging.domain.VeilarbArenaOppfolgingEndret;
import no.nav.veilarboppfolging.services.IservService;
import no.nav.veilarboppfolging.services.OppfolgingsenhetEndringService;
import no.nav.veilarboppfolging.services.MetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import static no.nav.common.json.JsonUtils.fromJson;


@Component
@Slf4j
public class Consumer {

    public static final String ENDRING_PAA_BRUKER_KAFKA_TOPIC_PROPERTY_NAME = "ENDRING_PAA_OPPFOLGINGSBRUKER_TOPIC";

    private final MetricsService metricsService;
    private final IservService iservService;
    private final OppfolgingsenhetEndringService oppfolgingsenhetEndringService;

    @Autowired
    public Consumer(IservService iservService, ConsumerParameters consumerParameters, MetricsService metricsService, OppfolgingsenhetEndringService oppfolgingsenhetEndringService) {
        this.iservService = iservService;
        this.metricsService = metricsService;
        this.oppfolgingsenhetEndringService = oppfolgingsenhetEndringService;
        setProperty(ENDRING_PAA_BRUKER_KAFKA_TOPIC_PROPERTY_NAME, consumerParameters.topic, PUBLIC);
    }

    @KafkaListener(topics = "${" + ENDRING_PAA_BRUKER_KAFKA_TOPIC_PROPERTY_NAME + "}")
    public void consume(String kafkaMelding) {
        try {
            final VeilarbArenaOppfolgingEndret deserialisertBruker = deserialisereBruker(kafkaMelding);

            iservService.behandleEndretBruker(deserialisertBruker);
            oppfolgingsenhetEndringService.behandleBrukerEndring(deserialisertBruker);
        } catch (Throwable t) {
            log.error("Feilet ved behandling av kafka-melding: {}\n{}", kafkaMelding, t.getMessage(), t);
        } finally {
            metricsService.antallMeldingerKonsumertAvKafka();
        }
    }

    public static VeilarbArenaOppfolgingEndret deserialisereBruker(String arenaBruker) {
        return fromJson(arenaBruker, VeilarbArenaOppfolgingEndret.class);
    }

    public static class ConsumerParameters {
        public final String topic;

        public ConsumerParameters(String topic) {
            this.topic = topic;
        }
    }
}
