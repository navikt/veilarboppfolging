package no.nav.fo.veilarboppfolging.kafka;

import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.services.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.Mockito.mock;

@Configuration
@Import({
        ConsumerConfig.class,
        Consumer.class
})
public class KafkaTestConfig {

    @Bean
    public Iserv28Service aktorService(JdbcTemplate jdbcTemplate) {
        OppfolgingService oppfolgingService = mock(OppfolgingService.class);
        AktorService aktorService = mock(AktorService.class);
        return new Iserv28Service(jdbcTemplate, oppfolgingService, aktorService);
    }

}
