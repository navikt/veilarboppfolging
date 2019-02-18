package no.nav.fo.veilarboppfolging.kafka;

import no.nav.fo.veilarboppfolging.mappers.ArenaBruker;
import no.nav.fo.veilarboppfolging.services.Iserv28Service;
import no.nav.fo.veilarboppfolging.utils.FunksjonelleMetrikker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static no.nav.json.JsonUtils.fromJson;
import static no.nav.sbl.util.EnvironmentUtils.Type.PUBLIC;
import static no.nav.sbl.util.EnvironmentUtils.setProperty;

@Component
public class Consumer {

    public static final String ENDRING_PAA_BRUKER_KAFKA_TOPIC_PROPERTY_NAME = "ENDRING_PAA_OPPFOLGINGSBRUKER_TOPIC";

    private static final Logger LOG = LoggerFactory.getLogger(Consumer.class);

    private final Iserv28Service iserv28Service;

    @Inject
    public Consumer(Iserv28Service iserv28Service, ConsumerParameters consumerParameters) {
        this.iserv28Service = iserv28Service;
        setProperty(ENDRING_PAA_BRUKER_KAFKA_TOPIC_PROPERTY_NAME, consumerParameters.topic, PUBLIC);
    }

    @KafkaListener(topics = "${" + ENDRING_PAA_BRUKER_KAFKA_TOPIC_PROPERTY_NAME + "}")
    public void consume(String arenaBruker) {
        try {
            final ArenaBruker deserialisertBruker = deserialisereBruker(arenaBruker);
            iserv28Service.filterereIservBrukere(deserialisertBruker);
            FunksjonelleMetrikker.antallMeldingerKonsumertAvKafka();
            LOG.info("Konsumert bruker med akoerid = '{}' har status i veilarbarena: {}", deserialisertBruker.aktoerid, deserialisertBruker);
        } catch (Throwable t) {
            LOG.error(t.getMessage(), t);
        }
    }

    public ArenaBruker deserialisereBruker(String arenaBruker) {
        return fromJson(arenaBruker, ArenaBruker.class);
    }

    public static class ConsumerParameters {
        public final String topic;

        public ConsumerParameters(String topic) {
            this.topic = topic;
        }
    }
}
