package no.nav.veilarboppfolging.controller;

import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.client.behandle_arbeidssoker.ArenaFeilException;
import no.nav.veilarboppfolging.controller.request.AktiverArbeidssokerData;
import no.nav.veilarboppfolging.controller.request.ReaktiverBrukerRequest;
import no.nav.veilarboppfolging.controller.request.SykmeldtBrukerType;
import no.nav.veilarboppfolging.controller.response.ArenaFeilDTO;
import no.nav.veilarboppfolging.service.AktiverBrukerService;
import no.nav.veilarboppfolging.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.FORBIDDEN;

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

        try {
            aktiverBrukerService.aktiverBruker(aktiverArbeidssokerData);
        } catch (ArenaFeilException exception) {
            // veilarbregistrering må ha body i response som inneholder årsak til feil fra Arena
            return ResponseEntity
                    .status(FORBIDDEN)
                    .body(new ArenaFeilDTO().setType(exception.type));
        }

        return ResponseEntity.status(204).build();
    }

    @PostMapping("/reaktiverbruker")
    public ResponseEntity reaktiverBruker(@RequestBody ReaktiverBrukerRequest request) {
        authService.skalVereSystemBruker();
        authService.sjekkLesetilgangMedFnr(request.getFnr());

        try {
            aktiverBrukerService.reaktiverBruker(request.getFnr());
        } catch (ArenaFeilException exception) {
            // veilarbregistrering må ha body i response som inneholder årsak til feil fra Arena
             return ResponseEntity
                     .status(FORBIDDEN)
                     .body(new ArenaFeilDTO().setType(exception.type));
        }

        return ResponseEntity.status(204).build();
    }

    @PostMapping("/aktiverSykmeldt")
    public ResponseEntity aktiverSykmeldt(@RequestBody SykmeldtBrukerType sykmeldtBrukerType, @RequestParam Fnr fnr) {
        authService.skalVereSystemBruker();
        authService.sjekkLesetilgangMedFnr(fnr);
        aktiverBrukerService.aktiverSykmeldt(fnr, sykmeldtBrukerType);
        return ResponseEntity.status(204).build();
    }

}
