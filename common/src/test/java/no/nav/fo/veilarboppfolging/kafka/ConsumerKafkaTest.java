package no.nav.fo.veilarboppfolging.kafka;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ConsumerConfig.class)
public class ConsumerKafkaTest extends KafkaTest {

    @Autowired
    private Consumer consumer;

    @Test
    public void testConsume() throws InterruptedException {
        String greeting = "Hello Spring Kafka Receiver!";
        template.sendDefault(greeting);

        consumer.getLatch().await(10000, TimeUnit.MILLISECONDS);
        // check that the message was received
        assertThat(consumer.getLatch().getCount()).isEqualTo(0);
    }
}
