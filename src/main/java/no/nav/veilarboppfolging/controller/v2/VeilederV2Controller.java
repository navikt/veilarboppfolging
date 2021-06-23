package no.nav.veilarboppfolging.controller.v2;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.request.VeilederTilordning;
import no.nav.veilarboppfolging.controller.response.TilordneVeilederResponse;
import no.nav.veilarboppfolging.controller.response.Veileder;
import no.nav.veilarboppfolging.repository.VeilederTilordningerRepository;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.VeilederTilordningService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/veileder")
@RequiredArgsConstructor
public class VeilederV2Controller {

    private final VeilederTilordningService veilederTilordningService;

    // TODO: Skal hente gjennom service
    private final VeilederTilordningerRepository veilederTilordningerRepository;

    private final AuthService authService;

    @GetMapping
    public Veileder hentVeileder(@RequestParam("fnr") Fnr fnr) {
        authService.skalVereInternBruker();
        authService.sjekkLesetilgangMedFnr(fnr);

        String veilederIdent = veilederTilordningerRepository.hentTilordningForAktoer(authService.getAktorIdOrThrow(fnr));
        return new Veileder(veilederIdent);
    }

    @PostMapping("/tilordne")
    public TilordneVeilederResponse tilordneVeiledere(@RequestBody List<VeilederTilordning> tilordninger) {
        return veilederTilordningService.tilordneVeiledere(tilordninger);
    }

    @PostMapping("/lest-aktivitetsplan")
    public ResponseEntity<?> lestAktivitetsplan(@RequestParam("fnr") Fnr fnr) {
        veilederTilordningService.lestAktivitetsplan(fnr);
        return ResponseEntity.status(204).build();
    }

}
