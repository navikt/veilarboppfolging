package no.nav.veilarboppfolging.controller.v2;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.v2.request.StartEskaleringV2Request;
import no.nav.veilarboppfolging.controller.v2.request.StoppEskaleringV2Request;
import no.nav.veilarboppfolging.controller.v2.response.EskaleringsvarselV2Response;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.EskaleringService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static no.nav.veilarboppfolging.utils.DtoMappers.mapToResponse;

@RestController
@RequestMapping("/api/v2/eskaleringsvarsel")
@RequiredArgsConstructor
public class EskaleringsvarselV2Controller {

    private final AuthService authService;

    private final EskaleringService eskaleringService;

    @GetMapping
    public ResponseEntity<EskaleringsvarselV2Response> hentGjeldendeEskalering(@RequestParam("fnr") Fnr fnr) {
        authService.skalVereInternBruker();
        authService.sjekkLesetilgangMedFnr(fnr);

        var maybeEskaleringsvarsel = eskaleringService.hentGjeldendeEskaleringsvarsel(fnr);

        if (maybeEskaleringsvarsel.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        var eskaleringsvarsel = maybeEskaleringsvarsel.get();

        return ResponseEntity.ok(mapToResponse(eskaleringsvarsel));
    }

    @PostMapping("/start")
    public ResponseEntity<?> startEskalering(@RequestBody StartEskaleringV2Request request) {
        authService.skalVereInternBruker();

        eskaleringService.startEskalering(
                request.getFnr(),
                authService.getInnloggetVeilederIdent(),
                request.getBegrunnelse(),
                request.getDialogId()
        );

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/stopp")
    public ResponseEntity<?> stoppEskalering(@RequestBody StoppEskaleringV2Request request) {
        authService.skalVereInternBruker();

        eskaleringService.stoppEskalering(request.getFnr(), authService.getInnloggetVeilederIdent(), request.getBegrunnelse());

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
