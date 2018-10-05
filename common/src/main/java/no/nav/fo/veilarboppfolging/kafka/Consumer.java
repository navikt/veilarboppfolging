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

@Component
public class Consumer {

    public static final String ENDRING_PAA_BRUKER_KAFKA_TOPIC = "aapen-fo-endringPaaOppfoelgingsBruker-v1";

    private static final Logger LOG = LoggerFactory.getLogger(Consumer.class);

    private final Iserv28Service iserv28Service;

    @Inject
    public Consumer(Iserv28Service iserv28Service) {
        this.iserv28Service = iserv28Service;
    }

    @KafkaListener(topics = ENDRING_PAA_BRUKER_KAFKA_TOPIC)
    public void consume(String arenaBruker) {
        try {
            final ArenaBruker deserialisertBruker = deserialisereBruker(arenaBruker);
            iserv28Service.filterereIservBrukere(deserialisertBruker);
            FunksjonelleMetrikker.antallMeldingerKonsumertAvKafka();
            LOG.info("endret bruker= '{}'", deserialisertBruker);
        } catch (Throwable t) {
            LOG.error(t.getMessage(), t);
        }
    }

    public ArenaBruker deserialisereBruker(String arenaBruker) {
        return fromJson(arenaBruker, ArenaBruker.class);
    }
}
