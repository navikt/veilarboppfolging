package no.nav.veilarboppfolging.controller.v3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.response.OppfolgingEnhetMedVeilederResponse;
import no.nav.veilarboppfolging.service.ArenaOppfolgingService;
import no.nav.veilarboppfolging.service.AuthService;
import org.json.JSONObject;
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
    public OppfolgingEnhetMedVeilederResponse getOppfolginsstatusV3(@RequestBody String fnr)
    {
        secureLog.info("v3 Arena postmapping innsendt ident: {}", fnr);
        JSONObject request = new JSONObject(fnr);
        Fnr fodselsnummer = authService.hentIdentForEksternEllerIntern(Fnr.of(request.getString("fnr")));
        authService.sjekkLesetilgangMedFnr(fodselsnummer);
        return arenaOppfolgingService.getOppfolginsstatusV3(fodselsnummer);
    }

    @GetMapping("/oppfolgingsenhet")
    public OppfolgingEnhetMedVeilederResponse.Oppfolgingsenhet getOppfolgingsenhet(@RequestParam("aktorId") AktorId aktorId) {
        authService.sjekkLesetilgangMedAktorId(aktorId);
        Fnr fnr = authService.getFnrOrThrow(aktorId);
        return arenaOppfolgingService.getOppfolginsstatusV3(fnr).getOppfolgingsenhet();
    }
}
