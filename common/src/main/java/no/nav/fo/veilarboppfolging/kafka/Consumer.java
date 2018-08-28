package no.nav.fo.veilarboppfolging.kafka;

import no.nav.fo.veilarboppfolging.mappers.ArenaBruker;
import no.nav.fo.veilarboppfolging.services.Iserv28Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.concurrent.CountDownLatch;

import static no.nav.json.JsonUtils.fromJson;

@Component
public class Consumer {

    private static final Logger LOG = LoggerFactory.getLogger(Consumer.class);

    @Inject
    private Iserv28Service iserv28Service;

    private CountDownLatch latch = new CountDownLatch(1);

    public CountDownLatch getLatch() {
        return latch;
    }

    @KafkaListener(topics = "aapen-fo-endringPaaOppfoelgingsBruker-v1")
    public void consume(String arenaBruker) {
        latch.countDown();
        final ArenaBruker deserialisertBruker = fromJson(arenaBruker, ArenaBruker.class);
        iserv28Service.filterereIservBrukere(deserialisertBruker);
        LOG.info("endret bruker= '{}'", deserialisertBruker);
    }
}
