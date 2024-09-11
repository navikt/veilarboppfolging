package no.nav.veilarboppfolging.controller.v2;

import lombok.RequiredArgsConstructor;
import no.nav.veilarboppfolging.controller.response.YtelserResponse;
import no.nav.veilarboppfolging.controller.v2.request.YtelserRequest;
import no.nav.veilarboppfolging.service.ArenaYtelserService;
import no.nav.veilarboppfolging.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/person")
public class YtelseV2Controller {

    private final AuthService authService;

    private final ArenaYtelserService arenaYtelserService;

    @PostMapping("/hent-ytelser")
    public YtelserResponse hentYtelser(@RequestBody YtelserRequest ytelserRequest) {
        authService.skalVereInternBruker();
        authService.sjekkLesetilgangMedFnr(ytelserRequest.fnr());

        return arenaYtelserService.hentYtelser(ytelserRequest.fnr());
    }
}
