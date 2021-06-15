package no.nav.veilarboppfolging.controller;

import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.response.Veileder;
import no.nav.veilarboppfolging.repository.VeilederTilordningerRepository;
import no.nav.veilarboppfolging.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/person")
public class VeilederController {

    private final VeilederTilordningerRepository veilederTilordningerRepository;

    private final AuthService authService;

    public VeilederController(VeilederTilordningerRepository veilederTilordningerRepository, AuthService authService) {
        this.veilederTilordningerRepository = veilederTilordningerRepository;
        this.authService = authService;
    }

    @GetMapping("/{fnr}/veileder")
    public Veileder getVeileder(@PathVariable("fnr") Fnr fnr) {
        authService.skalVereInternBruker();
        authService.sjekkLesetilgangMedFnr(fnr);

        String veilederIdent = veilederTilordningerRepository.hentTilordningForAktoer(authService.getAktorIdOrThrow(fnr));
        return new Veileder(veilederIdent);
    }

}
