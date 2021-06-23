package no.nav.veilarboppfolging.controller.v2;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.client.behandle_arbeidssoker.ArenaFeilException;
import no.nav.veilarboppfolging.controller.response.ArenaFeilDTO;
import no.nav.veilarboppfolging.service.AktiverBrukerService;
import no.nav.veilarboppfolging.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping("/api/v2/system-oppfolging") // TODO burde trolig endres
@RequiredArgsConstructor
public class SystemOppfolgingV2Controller {

    private final AuthService authService;

    private final AktiverBrukerService aktiverBrukerService;

    // Veilarbregistrering forventer 204, som forsåvidt er riktig status for disse endepunktene

    @PostMapping("/aktiver-bruker")
    public ResponseEntity<?> aktiverBruker(@RequestParam("fnr") Fnr fnr) {
        authService.skalVereSystemBruker();
        authService.sjekkLesetilgangMedFnr(fnr);

        try {
            aktiverBrukerService.aktiverBruker(fnr, null);
        } catch (ArenaFeilException exception) {
            // veilarbregistrering må ha body i response som inneholder årsak til feil fra Arena
            return ResponseEntity
                    .status(FORBIDDEN)
                    .body(new ArenaFeilDTO().setType(exception.type));
        }

        return ResponseEntity.status(204).build();
    }

    @PostMapping("/reaktiver-bruker")
    public ResponseEntity<?> reaktiverBruker(@RequestParam("fnr") Fnr fnr) {
        authService.skalVereSystemBruker();
        authService.sjekkLesetilgangMedFnr(fnr);

        try {
            aktiverBrukerService.reaktiverBruker(fnr);
        } catch (ArenaFeilException exception) {
            // veilarbregistrering må ha body i response som inneholder årsak til feil fra Arena
            return ResponseEntity
                    .status(FORBIDDEN)
                    .body(new ArenaFeilDTO().setType(exception.type));
        }

        return ResponseEntity.status(204).build();
    }

    @PostMapping("/aktiver-sykmeldt")
    public ResponseEntity<?> aktiverSykmeldt(@RequestParam("fnr") Fnr fnr) {
        authService.skalVereSystemBruker();
        authService.sjekkLesetilgangMedFnr(fnr);
        aktiverBrukerService.aktiverSykmeldt(fnr, null);

        return ResponseEntity.status(204).build();
    }

}
