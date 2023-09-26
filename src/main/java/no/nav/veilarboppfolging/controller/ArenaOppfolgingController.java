package no.nav.veilarboppfolging.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.response.OppfolgingEnhetMedVeilederResponse;
import no.nav.veilarboppfolging.service.ArenaOppfolgingService;
import no.nav.veilarboppfolging.service.AuthService;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/person")
public class ArenaOppfolgingController {

    private final AuthService authService;

    private final ArenaOppfolgingService arenaOppfolgingService;

    /*
     API used by veilarbmaofs. Contains only the necessary information
     */
    @PostMapping("/oppfolgingsstatus")
    public OppfolgingEnhetMedVeilederResponse getOppfolginsstatus(@RequestBody String fnr) {
        authService.sjekkLesetilgangMedFnr(Fnr.of(fnr));
        return arenaOppfolgingService.getOppfolginsstatus(Fnr.of(fnr));
    }

    @GetMapping("/oppfolgingsenhet")
    public OppfolgingEnhetMedVeilederResponse.Oppfolgingsenhet getOppfolgingsenhet(@RequestParam("aktorId") AktorId aktorId) {
        authService.sjekkLesetilgangMedAktorId(aktorId);
        Fnr fnr = authService.getFnrOrThrow(aktorId);
        return arenaOppfolgingService.getOppfolginsstatus(fnr).getOppfolgingsenhet();
    }
}
