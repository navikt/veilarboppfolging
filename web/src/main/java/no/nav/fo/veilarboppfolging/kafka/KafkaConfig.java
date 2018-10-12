package no.nav.fo.veilarboppfolging.kafka;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static no.nav.sbl.util.EnvironmentUtils.requireEnvironmentName;

@Configuration
public class KafkaConfig {

    @Bean
    public Consumer.ConsumerParameters consumerParameters() {
        return new Consumer.ConsumerParameters("aapen-fo-endringPaaOppfoelgingsBruker-v1-" + requireEnvironmentName());
    }

    @Bean
    public ConsumerConfig.SASL sasl() {
        return ConsumerConfig.SASL.ENABLED;
    }

}
