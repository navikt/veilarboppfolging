package no.nav.fo.veilarboppfolging.kafka;

import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.services.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

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
    public Iserv28Service aktorService(JdbcTemplate jdbcTemplate) {
        OppfolgingService oppfolgingService = mock(OppfolgingService.class);
        AktorService aktorService = mock(AktorService.class);
        LockingTaskExecutor taskExecutor = mock(LockingTaskExecutor.class);
        SystemUserSubjectProvider systemUserSubjectProvider = mock(SystemUserSubjectProvider.class);
        return new Iserv28Service(jdbcTemplate, oppfolgingService, aktorService, taskExecutor, systemUserSubjectProvider);
    }

}