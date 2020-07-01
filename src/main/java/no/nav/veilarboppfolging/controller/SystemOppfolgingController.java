package no.nav.veilarboppfolging.controller;

import no.nav.veilarboppfolging.domain.AktiverArbeidssokerData;
import no.nav.veilarboppfolging.domain.Fnr;
import no.nav.veilarboppfolging.domain.SykmeldtBrukerType;
import no.nav.veilarboppfolging.services.AktiverBrukerService;
import no.nav.veilarboppfolging.services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/oppfolging")
public class SystemOppfolgingController {

    private final AuthService authService;

    private final AktiverBrukerService aktiverBrukerService;

    @Autowired
    public SystemOppfolgingController (AuthService authService, AktiverBrukerService aktiverBrukerService) {
        this.authService = authService;
        this.aktiverBrukerService = aktiverBrukerService;
    }

    @PostMapping("/aktiverbruker")
    public void aktiverBruker(@RequestBody AktiverArbeidssokerData aktiverArbeidssokerData) {
        authService.skalVereSystemBruker();
        authService.sjekkLesetilgangMedFnr(aktiverArbeidssokerData.getFnr().getFnr());
        aktiverBrukerService.aktiverBruker(aktiverArbeidssokerData);
    }

    @PostMapping("/reaktiverbruker")
    public void reaktiverBruker(@RequestBody Fnr fnr) {
        authService.skalVereSystemBruker();
        authService.sjekkLesetilgangMedFnr(fnr.getFnr());
        aktiverBrukerService.reaktiverBruker(fnr);
    }

    @PostMapping("/aktiverSykmeldt")
    public void aktiverSykmeldt(@RequestBody SykmeldtBrukerType sykmeldtBrukerType, @RequestParam String fnr) {
        authService.skalVereSystemBruker();
        aktiverBrukerService.aktiverSykmeldt(fnr, sykmeldtBrukerType);
    }

}
