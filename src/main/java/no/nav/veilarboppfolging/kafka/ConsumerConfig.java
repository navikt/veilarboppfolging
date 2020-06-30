package no.nav.veilarboppfolging.kafka;

import no.nav.veilarboppfolging.mappers.VeilarbArenaOppfolging;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

import static no.nav.veilarboppfolging.config.ApplicationConfig.APP_ENVIRONMENT_NAME;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Configuration
@Import({ KafkaHelsesjekk.class })
@EnableKafka
public class ConsumerConfig {


    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, VeilarbArenaOppfolging>> kafkaListenerContainerFactory(KafkaHelsesjekk kafkaHelsesjekk) {
        ConcurrentKafkaListenerContainerFactory<String, VeilarbArenaOppfolging> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setErrorHandler(kafkaHelsesjekk);
        return factory;
    }

    @Bean
    public Consumer.ConsumerParameters consumerParameters() {
        return new Consumer.ConsumerParameters("aapen-fo-endringPaaOppfoelgingsBruker-v1-" + getRequiredProperty(APP_ENVIRONMENT_NAME));
    }

    static ConsumerFactory<String, VeilarbArenaOppfolging> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(KafkaPropsConfig.kafkaConsumerProperties());
    }

}
