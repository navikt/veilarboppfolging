package no.nav.veilarboppfolging.config;

import no.nav.veilarboppfolging.controller.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        ArenaOppfolgingController.class,
        InternalController.class,
        KvpController.class,
        MalController.class,
        OppfolgingControllerTest.class,
        OppfolgingController.class,
        OppfolgingNiva3Controller.class,
        SystemOppfolgingController.class,
        UnderOppfolgingController.class,
        VeilederController.class,
        VeilederTilordningController.class,
        YtelseController.class
})
public class ControllerTestConfig {}
