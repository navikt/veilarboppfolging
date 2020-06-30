package no.nav.veilarboppfolging.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.ContainerAwareErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

import static no.nav.veilarboppfolging.kafka.KafkaPropsConfig.KAFKA_BROKERS;


@Component
@Slf4j
public class KafkaHelsesjekk implements Helsesjekk, ContainerAwareErrorHandler {

    private long lastThrownExceptionTime;
    private Exception e;

    @Override
    public void helsesjekk() throws Throwable {
        // hvis en exception har blitt kastet innen det siste minuttet antar vi det er noe feil med spring-klienten
        if ((lastThrownExceptionTime + 60_000L) > System.currentTimeMillis()) {
            throw new IllegalArgumentException("Kafka consumer feilet " + new Date(lastThrownExceptionTime), e);
        }
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return new HelsesjekkMetadata("kafka", KAFKA_BROKERS, "kafka", false);
    }

    @Override
    public void handle(Exception thrownException, List<ConsumerRecord<?, ?>> records, Consumer<?, ?> consumer, MessageListenerContainer container) {
        log.error("Feil i listener:", thrownException);
        lastThrownExceptionTime = System.currentTimeMillis();
        e = thrownException;
    }

}
