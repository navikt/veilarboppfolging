package no.nav.fo.veilarboppfolging.kafka;

import no.nav.fo.veilarboppfolging.mappers.ArenaBruker;
import no.nav.fo.veilarboppfolging.services.Iserv28Service;
import no.nav.fo.veilarboppfolging.utils.FunksjonelleMetrikker;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import no.nav.sbl.util.EnvironmentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static no.nav.json.JsonUtils.fromJson;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Component
public class Consumer {

    public static final String ENDRING_PAA_BRUKER_KAFKA_TOPIC_PROPERTY_NAME = "ENDRING_PAA_OPPFOLGINGSBRUKER_TOPIC";

    private static final Logger LOG = LoggerFactory.getLogger(Consumer.class);

    private final Iserv28Service iserv28Service;
    private final UnleashService unleashService;

    @Inject
    public Consumer(Iserv28Service iserv28Service, UnleashService unleashService) {
        this.iserv28Service = iserv28Service;
        this.unleashService = unleashService;
        getRequiredProperty(ENDRING_PAA_BRUKER_KAFKA_TOPIC_PROPERTY_NAME);
    }

    @KafkaListener(topics = "${" + ENDRING_PAA_BRUKER_KAFKA_TOPIC_PROPERTY_NAME + "}")
    public void consume(String arenaBruker) {
        if (unleashService.isEnabled("konsumer-endring-oppfolgingsbruker")) {
            try {
                final ArenaBruker deserialisertBruker = deserialisereBruker(arenaBruker);
                iserv28Service.filterereIservBrukere(deserialisertBruker);
                FunksjonelleMetrikker.antallMeldingerKonsumertAvKafka();
                LOG.info("endret bruker= '{}'", deserialisertBruker);
            } catch (Throwable t) {
                LOG.error(t.getMessage(), t);
            }
        } else {
            LOG.info("konsumering av endringer er disablet");
        }
    }

    public ArenaBruker deserialisereBruker(String arenaBruker) {
        return fromJson(arenaBruker, ArenaBruker.class);
    }
}
