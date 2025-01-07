package no.nav.veilarboppfolging.config;

import no.nav.veilarboppfolging.controller.*;
import no.nav.veilarboppfolging.controller.v2.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        ArenaOppfolgingController.class,
        ArenaOppfolgingV2Controller.class,
        InternalController.class,
        KvpV2Controller.class,
        MaalController.class,
        OppfolgingController.class,
        OppfolgingV2Controller.class,
        OppfolgingNiva3Controller.class,
        UnderOppfolgingController.class,
        UnderOppfolgingV2Controller.class,
        VeilederController.class,
        VeilederV2Controller.class,
        VeilederTilordningController.class,
        YtelseController.class,
        SakController.class
})
public class ControllerTestConfig {}
