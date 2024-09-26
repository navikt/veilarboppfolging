package no.nav.veilarboppfolging.config;

import no.nav.veilarboppfolging.kafka.ArbeidssøkerperiodeConsumerService;
import no.nav.veilarboppfolging.service.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        AktiverBrukerService.class,
        ArenaOppfolgingService.class,
        AuthService.class,
        HistorikkService.class,
        IservService.class,
        KvpService.class,
        MaalService.class,
        ManuellStatusService.class,
        MetricsService.class,
        OppfolgingsenhetEndringService.class,
        OppfolgingService.class,
        VeilederTilordningService.class,
        KafkaProducerService.class,
        KafkaConsumerService.class,
        ArbeidssøkerperiodeConsumerService.class,
        OppfolgingEndringService.class,
        SakService.class,
        ArenaYtelserService.class
})
public class ServiceTestConfig {}
