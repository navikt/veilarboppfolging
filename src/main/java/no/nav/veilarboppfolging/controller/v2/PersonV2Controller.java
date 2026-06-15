package no.nav.veilarboppfolging.controller.v2;

import no.nav.veilarboppfolging.controller.response.Bruker;
import no.nav.veilarboppfolging.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/person")

public class PersonV2Controller {

    private final AuthService authService;

    @Autowired
    public PersonV2Controller(AuthService authService) {
        this.authService = authService;
    }

    // TODO: Sjekk om dette er i bruk
    @GetMapping("/me")
    public Bruker hentBrukerInfo() {
        return new Bruker(authService.getInnloggetBrukerIdent(), authService.erInternBruker(), authService.erEksternBruker());
    }
}
