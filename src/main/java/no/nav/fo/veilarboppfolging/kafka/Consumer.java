package no.nav.fo.veilarboppfolging.kafka;

import no.nav.fo.veilarboppfolging.mappers.VeilarbArenaOppfolging;
import no.nav.fo.veilarboppfolging.services.Iserv28Service;
import no.nav.fo.veilarboppfolging.services.OppfolgingsenhetEndringService;
import no.nav.fo.veilarboppfolging.utils.FunksjonelleMetrikker;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

import static no.nav.json.JsonUtils.fromJson;
import static no.nav.sbl.util.EnvironmentUtils.Type.PUBLIC;
import static no.nav.sbl.util.EnvironmentUtils.setProperty;

@Component
@Slf4j
public class Consumer {

    public static final String ENDRING_PAA_BRUKER_KAFKA_TOPIC_PROPERTY_NAME = "ENDRING_PAA_OPPFOLGINGSBRUKER_TOPIC";

    private final Iserv28Service iserv28Service;
    private final OppfolgingsenhetEndringService oppfolgingsenhetEndringService;

    @Inject
    public Consumer(Iserv28Service iserv28Service, ConsumerParameters consumerParameters, OppfolgingsenhetEndringService oppfolgingsenhetEndringService) {
        this.iserv28Service = iserv28Service;
        this.oppfolgingsenhetEndringService = oppfolgingsenhetEndringService;
        setProperty(ENDRING_PAA_BRUKER_KAFKA_TOPIC_PROPERTY_NAME, consumerParameters.topic, PUBLIC);
    }

    @KafkaListener(topics = "${" + ENDRING_PAA_BRUKER_KAFKA_TOPIC_PROPERTY_NAME + "}")
    public void consume(String kafkaMelding) {
        try {
            final VeilarbArenaOppfolging deserialisertBruker = deserialisereBruker(kafkaMelding);

            iserv28Service.behandleEndretBruker(deserialisertBruker);
            oppfolgingsenhetEndringService.behandleBrukerEndring(deserialisertBruker);
        } catch (Throwable t) {
            log.error("Feilet ved behandling av kafka-melding: {}\n{}", kafkaMelding, t.getMessage(), t);
        } finally {
            FunksjonelleMetrikker.antallMeldingerKonsumertAvKafka();
        }
    }

    public static VeilarbArenaOppfolging deserialisereBruker(String arenaBruker) {
        return fromJson(arenaBruker, VeilarbArenaOppfolging.class);
    }

    public static class ConsumerParameters {
        public final String topic;

        public ConsumerParameters(String topic) {
            this.topic = topic;
        }
    }
}
