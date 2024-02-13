package no.nav.veilarboppfolging.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.junit.ClassRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.rule.EmbeddedKafkaRule;

import java.util.Map;
import java.util.Set;

@EmbeddedKafka
@Configuration
public class EmbeddedKafkaConfig {
    @ClassRule
    public static EmbeddedKafkaRule embeddedKafka = new EmbeddedKafkaRule(1, true, "my-topic");
    @Bean
    public EmbeddedKafkaBroker kafkaContainer() {
        return new EmbeddedKafkaBroker() {
            @Override
            public EmbeddedKafkaBroker kafkaPorts(int... ports) {
                return null;
            }

            @Override
            public Set<String> getTopics() {
                return null;
            }

            @Override
            public EmbeddedKafkaBroker brokerProperties(Map<String, String> properties) {
                return null;
            }

            @Override
            public EmbeddedKafkaBroker brokerListProperty(String brokerListProperty) {
                return null;
            }

            @Override
            public String getBrokersAsString() {
                return null;
            }

            @Override
            public void addTopics(String... topicsToAdd) {

            }

            @Override
            public void addTopics(NewTopic... topicsToAdd) {

            }

            @Override
            public Map<String, Exception> addTopicsWithResults(NewTopic... topicsToAdd) {
                return null;
            }

            @Override
            public Map<String, Exception> addTopicsWithResults(String... topicsToAdd) {
                return null;
            }

            @Override
            public void consumeFromEmbeddedTopics(Consumer<?, ?> consumer, boolean seekToEnd, String... topicsToConsume) {

            }

            @Override
            public void consumeFromEmbeddedTopics(Consumer<?, ?> consumer, String... topicsToConsume) {

            }

            @Override
            public void consumeFromAnEmbeddedTopic(Consumer<?, ?> consumer, boolean seekToEnd, String topic) {

            }

            @Override
            public void consumeFromAnEmbeddedTopic(Consumer<?, ?> consumer, String topic) {

            }

            @Override
            public void consumeFromAllEmbeddedTopics(Consumer<?, ?> consumer, boolean seekToEnd) {

            }

            @Override
            public void consumeFromAllEmbeddedTopics(Consumer<?, ?> consumer) {

            }

            @Override
            public int getPartitionsPerTopic() {
                return 0;
            }
        };
    }
}
