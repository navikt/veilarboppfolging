package no.nav.veilarboppfolging.controller.v2;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.request.VeilederTilordning;
import no.nav.veilarboppfolging.controller.response.TilordneVeilederResponse;
import no.nav.veilarboppfolging.controller.v2.response.HentVeilederV2Response;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.VeilederTilordningService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v2/veileder")
@RequiredArgsConstructor
public class VeilederV2Controller {

    private final VeilederTilordningService veilederTilordningService;

    private final AuthService authService;

    @GetMapping(params = "fnr")
    public ResponseEntity<HentVeilederV2Response> hentVeileder(@RequestParam("fnr") Fnr fnr) {
        authService.skalVereInternEllerSystemBruker();

        if (authService.erInternBruker()) {
            authService.sjekkLesetilgangMedFnr(fnr);
        }

        var maybeVeilederIdent = veilederTilordningService.hentTilordnetVeilederIdent(fnr);

        if (maybeVeilederIdent.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        return ResponseEntity.ok(new HentVeilederV2Response(maybeVeilederIdent.get()));
    }

    @GetMapping(params = "aktorId")
    public ResponseEntity<HentVeilederV2Response> hentVeileder(@RequestParam("aktorId") AktorId aktorId) {
        Fnr fnr = authService.getFnrOrThrow(aktorId);
        return hentVeileder(fnr);
    }

    @PostMapping("/tilordne")
    public TilordneVeilederResponse tilordneVeiledere(@RequestBody List<VeilederTilordning> tilordninger) {
        return veilederTilordningService.tilordneVeiledere(tilordninger);
    }

    @PostMapping("/lest-aktivitetsplan")
    public ResponseEntity<?> lestAktivitetsplan(@RequestParam("fnr") Fnr fnr) {
        veilederTilordningService.lestAktivitetsplan(fnr);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

}
