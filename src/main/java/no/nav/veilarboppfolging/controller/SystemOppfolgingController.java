package no.nav.veilarboppfolging.controller;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.client.behandle_arbeidssoker.ArenaFeilException;
import no.nav.veilarboppfolging.controller.request.AktiverArbeidssokerData;
import no.nav.veilarboppfolging.controller.request.ReaktiverBrukerRequest;
import no.nav.veilarboppfolging.controller.request.SykmeldtBrukerType;
import no.nav.veilarboppfolging.controller.response.ArenaFeilDTO;
import no.nav.veilarboppfolging.service.AktiverBrukerService;
import no.nav.veilarboppfolging.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/oppfolging")
public class SystemOppfolgingController {

    private final AuthService authService;

    private final AktiverBrukerService aktiverBrukerService;

    // Veilarbregistrering forventer 204, som forsåvidt er riktig status for disse endepunktene

    @PostMapping("/aktiverbruker")
    public ResponseEntity aktiverBruker(@RequestBody AktiverArbeidssokerData aktiverArbeidssokerData) {
        authService.skalVereSystemBruker();

        AktiverArbeidssokerData.Fnr requestFnr = ofNullable(aktiverArbeidssokerData.getFnr())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "FNR mangler"));

        Fnr fnr = Fnr.of(requestFnr.getFnr());

        try {
            aktiverBrukerService.aktiverBruker(fnr, aktiverArbeidssokerData.getInnsatsgruppe());
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

        aktiverBrukerService.aktiverSykmeldt(fnr, sykmeldtBrukerType);
        return ResponseEntity.status(204).build();
    }

}
