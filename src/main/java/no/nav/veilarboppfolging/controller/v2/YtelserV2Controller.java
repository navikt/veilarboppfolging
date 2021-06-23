package no.nav.veilarboppfolging.controller.v2;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.v2.response.YtelserV2Response;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.YtelserOgAktiviteterService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// TODO: Kunne ligget under /person?

@RestController
@RequestMapping("/api/v2/ytelser")
@RequiredArgsConstructor
public class YtelserV2Controller {

    private final YtelserOgAktiviteterService ytelserOgAktiviteterService;

    private final AuthService authService;

    @GetMapping
    public YtelserV2Response hentYtelser(@RequestParam("fnr") Fnr fnr) {
        authService.skalVereInternBruker();
        authService.sjekkLesetilgangMedFnr(fnr);

        var ytelseskontrakt =  ytelserOgAktiviteterService.hentYtelseskontrakt(fnr);

        return new YtelserV2Response(ytelseskontrakt.getVedtaksliste(), ytelseskontrakt.getYtelser());
    }

}
