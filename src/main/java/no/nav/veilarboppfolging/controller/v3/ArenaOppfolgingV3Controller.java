package no.nav.veilarboppfolging.controller.v3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarboppfolging.controller.response.OppfolgingEnhetMedVeilederResponse;
import no.nav.veilarboppfolging.domain.PersonRequest;
import no.nav.veilarboppfolging.service.ArenaOppfolgingService;
import no.nav.veilarboppfolging.service.AuthService;
import org.springframework.web.bind.annotation.*;

import static no.nav.veilarboppfolging.utils.SecureLog.secureLog;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/person/v3")
public class ArenaOppfolgingV3Controller {

    private final AuthService authService;

    private final ArenaOppfolgingService arenaOppfolgingService;

    /*
     API used by veilarbmaofs. Contains only the necessary information
     */
    @PostMapping("/oppfolgingsstatus")
    public OppfolgingEnhetMedVeilederResponse getOppfolgingsstatusV3(@RequestBody PersonRequest personRequest)
    {
        secureLog.info("v3 Arena postmapping innsendt ident: {}", personRequest);
        authService.sjekkLesetilgangMedFnr(personRequest.getFnr());
        return arenaOppfolgingService.getOppfolginsstatus(personRequest);
    }
}
