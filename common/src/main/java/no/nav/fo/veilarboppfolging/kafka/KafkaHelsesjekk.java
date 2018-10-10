package no.nav.fo.veilarboppfolging.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import org.springframework.context.ApplicationListener;
import org.springframework.kafka.event.NonResponsiveConsumerEvent;
import org.springframework.stereotype.Component;

import java.util.Date;

import static no.nav.fo.veilarboppfolging.kafka.ConsumerConfig.getBrokerUrls;

@Component
@Slf4j
public class KafkaHelsesjekk implements Helsesjekk, ApplicationListener<NonResponsiveConsumerEvent> {

    private long lastNonResponsiveEvent;

    @Override
    public void helsesjekk() throws Throwable {
        // kafka rapporterer om ikke-responsiv klient hvert 30. sekund.
        // feiler selftest når det er mindre enn 60 sekunder siden forrige NonResponsiveConsumerEvent
        if ((lastNonResponsiveEvent + 60_000L) > System.currentTimeMillis()) {
            throw new IllegalArgumentException("Kafka consumer var ikke-responsiv " + new Date(lastNonResponsiveEvent));
        }
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return new HelsesjekkMetadata("kafka", getBrokerUrls(), "kafka", false);
    }

    @Override
    public void onApplicationEvent(NonResponsiveConsumerEvent nonResponsiveConsumerEvent) {
        log.warn(nonResponsiveConsumerEvent.toString());
        lastNonResponsiveEvent = System.currentTimeMillis();
    }
}
