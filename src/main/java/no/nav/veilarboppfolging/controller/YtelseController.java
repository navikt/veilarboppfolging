package no.nav.veilarboppfolging.controller;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.response.YtelserResponse;
import no.nav.veilarboppfolging.service.ArenaYtelserService;
import no.nav.veilarboppfolging.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/person")
public class YtelseController {

    private final ArenaYtelserService arenaYtelserService;

    private final AuthService authService;

    @GetMapping("/{fnr}/ytelser")
    public YtelserResponse hentYtelser(@PathVariable("fnr") Fnr fnr) {
        authService.skalVereInternBruker();
        authService.sjekkLesetilgangMedFnr(fnr);

        return arenaYtelserService.hentYtelser(fnr);
    }
}
