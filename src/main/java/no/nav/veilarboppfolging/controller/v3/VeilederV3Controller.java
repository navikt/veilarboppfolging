package no.nav.veilarboppfolging.controller.v3;

import lombok.RequiredArgsConstructor;
import no.nav.veilarboppfolging.controller.v2.response.HentVeilederV2Response;
import no.nav.veilarboppfolging.controller.v3.request.VeilederRequest;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.VeilederTilordningService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v3")
@RequiredArgsConstructor
public class VeilederV3Controller {

    private final VeilederTilordningService veilederTilordningService;

    private final AuthService authService;

    @PostMapping("/hent-veileder")
    public ResponseEntity<HentVeilederV2Response> hentVeileder(@RequestBody VeilederRequest veilederRequest) {
        authService.skalVereInternEllerSystemBruker();

        if (authService.erInternBruker()) {
            authService.sjekkLesetilgangMedFnr(veilederRequest.fnr());
        }

        var maybeVeilederIdent = veilederTilordningService.hentTilordnetVeilederIdent(veilederRequest.fnr());

        if (maybeVeilederIdent.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        return ResponseEntity.ok(new HentVeilederV2Response(maybeVeilederIdent.get()));
    }

    @PostMapping("/veileder/lest-aktivitetsplan")
    public ResponseEntity<?> lestAktivitetsplan(@RequestBody VeilederRequest veilederRequest) {
        veilederTilordningService.lestAktivitetsplan(veilederRequest.fnr());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
