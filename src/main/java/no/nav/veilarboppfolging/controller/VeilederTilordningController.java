package no.nav.veilarboppfolging.controller;

import lombok.RequiredArgsConstructor;
import no.nav.veilarboppfolging.controller.request.VeilederTilordning;
import no.nav.veilarboppfolging.controller.response.TilordneVeilederResponse;
import no.nav.veilarboppfolging.service.VeilederTilordningService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class VeilederTilordningController {

    private final VeilederTilordningService veilederTilordningService;

    @PostMapping("/tilordneveileder")
    public TilordneVeilederResponse tilordneVeiledere(@RequestBody List<VeilederTilordning> tilordninger) {
        return veilederTilordningService.tilordneVeiledere(tilordninger);
    }

    @PostMapping("{fnr}/lestaktivitetsplan")
    public ResponseEntity lestAktivitetsplan(@PathVariable("fnr") String fnr) {
        veilederTilordningService.lestAktivitetsplan(fnr);
        return ResponseEntity.status(204).build();
    }

}
