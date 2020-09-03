package no.nav.veilarboppfolging.controller;

import no.nav.veilarboppfolging.controller.domain.TilordneVeilederResponse;
import no.nav.veilarboppfolging.controller.domain.VeilederTilordning;
import no.nav.veilarboppfolging.service.VeilederTilordningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class VeilederTilordningController {

    private final VeilederTilordningService veilederTilordningService;

    @Autowired
    public VeilederTilordningController(VeilederTilordningService veilederTilordningService) {
      this.veilederTilordningService = veilederTilordningService;
    }

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
