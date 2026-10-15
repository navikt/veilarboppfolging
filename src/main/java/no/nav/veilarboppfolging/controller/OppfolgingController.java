package no.nav.veilarboppfolging.controller;

import no.nav.veilarboppfolging.controller.response.*;
import no.nav.veilarboppfolging.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/oppfolging")
public class OppfolgingController {
    private final AuthService authService;

    @Autowired
    public OppfolgingController(AuthService authService) {
        this.authService = authService;
    }

    /* Bare brukt av arbeidsrettet-dialog */
    @GetMapping("/me")
    public Bruker hentBrukerInfo() {
        return new Bruker(
                authService.getInnloggetBrukerIdent(),
                authService.erInternBruker(),
                authService.erEksternBruker()
        );
    }

}
