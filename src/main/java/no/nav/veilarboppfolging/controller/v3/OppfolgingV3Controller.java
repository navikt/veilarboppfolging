package no.nav.veilarboppfolging.controller.v3;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.response.OppfolgingStatus;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.OppfolgingService;
import org.json.JSONObject;
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
    public OppfolgingStatus hentOppfolgingData(@RequestBody  String fnr) {
        secureLog.info("v3 postmapping innsendt ident: {}", fnr);
        JSONObject request = new JSONObject(fnr);
        Fnr fodselsnummer = authService.hentIdentForEksternEllerIntern(Fnr.of(request.getString("fnr")));
        return tilDto(oppfolgingService.hentOppfolgingsStatus(fodselsnummer), authService.erInternBruker());
    }
}
