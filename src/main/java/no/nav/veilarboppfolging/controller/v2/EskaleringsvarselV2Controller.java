package no.nav.veilarboppfolging.controller.v2;

import lombok.RequiredArgsConstructor;
import no.nav.veilarboppfolging.controller.v2.request.StartEskaleringV2Request;
import no.nav.veilarboppfolging.controller.v2.request.StoppEskaleringV2Request;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.EskaleringService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/eskaleringsvarsel")
@RequiredArgsConstructor
public class EskaleringsvarselV2Controller {

    private final AuthService authService;

    private final EskaleringService eskaleringService;

    @PostMapping("/start")
    public ResponseEntity<?> startEskalering(@RequestBody StartEskaleringV2Request request) {
        authService.skalVereInternBruker();

        eskaleringService.startEskalering(
                request.getFnr(),
                authService.getInnloggetVeilederIdent(),
                request.getBegrunnelse(),
                request.getDialogId()
        );

        return ResponseEntity.status(204).build();
    }

    @PostMapping("/stopp")
    public ResponseEntity<?> stoppEskalering(@RequestBody StoppEskaleringV2Request request) {
        authService.skalVereInternBruker();

        eskaleringService.stoppEskalering(request.getFnr(), authService.getInnloggetVeilederIdent(), request.getBegrunnelse());

        return ResponseEntity.status(204).build();
    }

}
