package no.nav.veilarboppfolging.controller.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.response.OppfolgingEnhetMedVeilederResponse;
import no.nav.veilarboppfolging.controller.v2.request.ArenaOppfolgingRequest;
import no.nav.veilarboppfolging.service.ArenaOppfolgingService;
import no.nav.veilarboppfolging.service.AuthService;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/person")
public class ArenaOppfolgingV2Controller {

    private final AuthService authService;

    private final ArenaOppfolgingService arenaOppfolgingService;

    /*
     API used by veilarbdetaljerfs. Contains only the necessary information
     */
    @PostMapping ("/hent-oppfolgingsstatus")
    public OppfolgingEnhetMedVeilederResponse getOppfolgingsstatus(@RequestBody ArenaOppfolgingRequest arenaOppfolgingRequest) {
        authService.sjekkLesetilgangMedFnr(arenaOppfolgingRequest.fnr());
        return arenaOppfolgingService.getOppfolginsstatus(arenaOppfolgingRequest.fnr());
    }
}
