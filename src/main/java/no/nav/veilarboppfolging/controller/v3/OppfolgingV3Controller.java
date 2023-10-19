package no.nav.veilarboppfolging.controller.v3;

import lombok.RequiredArgsConstructor;
import no.nav.veilarboppfolging.controller.response.OppfolgingStatus;
import no.nav.veilarboppfolging.domain.PersonRequest;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.OppfolgingService;
import org.springframework.web.bind.annotation.*;
import static no.nav.veilarboppfolging.utils.DtoMappers.tilDto;
import static no.nav.veilarboppfolging.utils.SecureLog.secureLog;

@RestController
@RequestMapping("/api/v3")
@RequiredArgsConstructor
public class OppfolgingV3Controller {
    private final OppfolgingService oppfolgingService;
    private final AuthService authService;

    @PostMapping ("/oppfolging")
    public OppfolgingStatus hentOppfolgingData(@RequestBody PersonRequest personRequest) {
        secureLog.info("v3 postmapping innsendt ident: {}", personRequest);
        return tilDto(oppfolgingService.hentOppfolgingsStatus(personRequest), authService.erInternBruker());
    }
}
