package no.nav.veilarboppfolging.config;

import no.nav.veilarboppfolging.services.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        AktiverBrukerService.class,
        AuthService.class,
        HistorikkService.class,
        IservService.class,
        KvpService.class,
        MalService.class,
        ManuellStatusService.class,
        MetricsService.class,
        OppfolgingsenhetEndringService.class,
        OppfolgingService.class

})
public class ServiceTestConfig {}
