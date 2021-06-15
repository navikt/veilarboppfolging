package no.nav.veilarboppfolging.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.response.OppfolgingEnhetMedVeilederResponse;
import no.nav.veilarboppfolging.service.ArenaOppfolgingService;
import no.nav.veilarboppfolging.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/person")
public class ArenaOppfolgingController {

    private final AuthService authService;

    private final ArenaOppfolgingService arenaOppfolgingService;

    @Autowired
    public ArenaOppfolgingController (AuthService authService, ArenaOppfolgingService arenaOppfolgingService) {
        this.authService = authService;
        this.arenaOppfolgingService = arenaOppfolgingService;
    }

    /*
     API used by veilarbmaofs. Contains only the necessary information
     */
    @GetMapping("/{fnr}/oppfolgingsstatus")
    public OppfolgingEnhetMedVeilederResponse getOppfolginsstatus(@PathVariable("fnr") Fnr fnr) {
        authService.sjekkLesetilgangMedFnr(fnr);
        return arenaOppfolgingService.getOppfolginsstatus(fnr);
    }

}
