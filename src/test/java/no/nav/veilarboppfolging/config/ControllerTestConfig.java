package no.nav.veilarboppfolging.config;

import no.nav.veilarboppfolging.controller.*;
import no.nav.veilarboppfolging.controller.v2.ArenaOppfolgingV2Controller;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        ArenaOppfolgingController.class,
        ArenaOppfolgingV2Controller.class,
        InternalController.class,
        KvpController.class,
        MaalController.class,
        OppfolgingController.class,
        OppfolgingNiva3Controller.class,
        SystemOppfolgingController.class,
        UnderOppfolgingController.class,
        VeilederController.class,
        VeilederTilordningController.class,
        YtelseController.class
})
public class ControllerTestConfig {}
