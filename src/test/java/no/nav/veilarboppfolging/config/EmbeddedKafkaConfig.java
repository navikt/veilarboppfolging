package no.nav.veilarboppfolging.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.test.EmbeddedKafkaBroker;

@Configuration
public class EmbeddedKafkaConfig {
    @Bean
    public EmbeddedKafkaBroker kafkaContainer() {
        return new EmbeddedKafkaBroker(1, true, 1);
    }
}
