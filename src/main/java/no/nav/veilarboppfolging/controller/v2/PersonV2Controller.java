package no.nav.veilarboppfolging.controller.v2;

import lombok.RequiredArgsConstructor;
import no.nav.veilarboppfolging.controller.response.Bruker;
import no.nav.veilarboppfolging.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/person")
@RequiredArgsConstructor
public class PersonV2Controller {

    private final AuthService authService;

    @GetMapping("/me")
    public Bruker hentBrukerInfo() {
        return new Bruker()
                .setId(authService.getInnloggetBrukerIdent())
                .setErVeileder(authService.erInternBruker())
                .setErBruker(authService.erEksternBruker());
    }


}
