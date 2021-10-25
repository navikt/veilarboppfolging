package no.nav.veilarboppfolging.controller;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;
import no.nav.veilarboppfolging.controller.response.Veileder;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.VeilederTilordningService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/person")
public class VeilederController {

    private final VeilederTilordningService veilederTilordningService;

    private final AuthService authService;

    @GetMapping("/{fnr}/veileder")
    public Veileder getVeileder(@PathVariable("fnr") Fnr fnr) {
        authService.skalVereInternEllerSystemBruker();
        authService.sjekkLesetilgangMedFnr(fnr);

        var veilederIdent = veilederTilordningService.hentTilordnetVeilederIdent(fnr)
                .map(NavIdent::get)
                .orElse(null);

        return new Veileder(veilederIdent);
    }

}
