package no.nav.fo.veilarboppfolging.kafka;


import no.nav.fo.veilarboppfolging.services.Iserv28Service;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.mockito.Mockito.mock;

@Import({
        ConsumerConfig.class,
        Consumer.class
})
public class KafkaTestConfig {

    public static final String KAFKA_TEST_TOPIC = "test-topic";

    @Bean
    public Consumer.ConsumerParameters consumerParameters() {
        return new Consumer.ConsumerParameters(KAFKA_TEST_TOPIC);
    }

    @Bean
    public ConsumerConfig.SASL sasl(){
        return ConsumerConfig.SASL.DISABLED;
    }

    @Bean
    public Iserv28Service iserv28Service() {
        return mock(Iserv28Service.class);
    }
}
