package no.nav.veilarboppfolging.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckResult;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.ContainerAwareErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class KafkaHelsesjekk implements HealthCheck, ContainerAwareErrorHandler {

    private long lastThrownExceptionTime;

    private Exception lastThrownException;

    @Override
    public void handle(Exception thrownException, List<ConsumerRecord<?, ?>> records, Consumer<?, ?> consumer, MessageListenerContainer container) {
        log.error("Feil i listener:", thrownException);
        lastThrownExceptionTime = System.currentTimeMillis();
        lastThrownException = thrownException;
    }

    @Override
    public HealthCheckResult checkHealth() {
        // hvis en exception har blitt kastet innen det siste minuttet antar vi det er noe feil med spring-klienten
        if ((lastThrownExceptionTime + 60_000L) > System.currentTimeMillis()) {
            return HealthCheckResult.unhealthy("Kafka consumer feilet " + new Date(lastThrownExceptionTime), lastThrownException);
        }

        return HealthCheckResult.healthy();
    }
}
