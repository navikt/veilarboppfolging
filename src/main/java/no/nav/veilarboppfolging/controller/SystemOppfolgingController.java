package no.nav.veilarboppfolging.controller;

import no.nav.veilarboppfolging.domain.AktiverArbeidssokerData;
import no.nav.veilarboppfolging.domain.Fnr;
import no.nav.veilarboppfolging.domain.SykmeldtBrukerType;
import no.nav.veilarboppfolging.service.AktiverBrukerService;
import no.nav.veilarboppfolging.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/oppfolging")
public class SystemOppfolgingController {

    private final AuthService authService;

    private final AktiverBrukerService aktiverBrukerService;

    // Veilarbregistrering forventer 204, som forsåvidt er riktig status for disse endepunktene

    @Autowired
    public SystemOppfolgingController (AuthService authService, AktiverBrukerService aktiverBrukerService) {
        this.authService = authService;
        this.aktiverBrukerService = aktiverBrukerService;
    }

    @PostMapping("/aktiverbruker")
    public ResponseEntity aktiverBruker(@RequestBody AktiverArbeidssokerData aktiverArbeidssokerData) {
        authService.skalVereSystemBruker();
        authService.sjekkLesetilgangMedFnr(aktiverArbeidssokerData.getFnr().getFnr());
        aktiverBrukerService.aktiverBruker(aktiverArbeidssokerData);
        return ResponseEntity.status(204).build();
    }

    @PostMapping("/reaktiverbruker")
    public ResponseEntity reaktiverBruker(@RequestBody Fnr fnr) {
        authService.skalVereSystemBruker();
        authService.sjekkLesetilgangMedFnr(fnr.getFnr());
        aktiverBrukerService.reaktiverBruker(fnr);
        return ResponseEntity.status(204).build();
    }

    @PostMapping("/aktiverSykmeldt")
    public ResponseEntity aktiverSykmeldt(@RequestBody SykmeldtBrukerType sykmeldtBrukerType, @RequestParam String fnr) {
        authService.skalVereSystemBruker();
        aktiverBrukerService.aktiverSykmeldt(fnr, sykmeldtBrukerType);
        return ResponseEntity.status(204).build();
    }

}
