package no.nav.veilarboppfolging.controller.v2;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.response.YtelserResponse;
import no.nav.veilarboppfolging.controller.v2.request.YtelserRequest;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.YtelserOgAktiviteterService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/person")
public class YtelseV2Controller {

    private final YtelserOgAktiviteterService ytelserOgAktiviteterService;

    private final AuthService authService;

    @PostMapping("/hent-ytelser")
    public YtelserResponse hentYtelser(@RequestBody YtelserRequest ytelserRequest) {
        authService.skalVereInternBruker();
        authService.sjekkLesetilgangMedFnr(ytelserRequest.fnr());

        var ytelseskontrakt = ytelserOgAktiviteterService.hentYtelseskontrakt(ytelserRequest.fnr());

        return new YtelserResponse()
                .withVedtaksliste(ytelseskontrakt.getVedtaksliste())
                .withYtelser(ytelseskontrakt.getYtelser());
    }
}
