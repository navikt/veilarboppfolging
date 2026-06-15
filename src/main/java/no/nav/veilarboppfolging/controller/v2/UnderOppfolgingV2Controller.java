package no.nav.veilarboppfolging.controller.v2;

import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.response.UnderOppfolgingDTO;
import no.nav.veilarboppfolging.controller.v2.request.UnderOppfolgingRequest;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.OppfolgingService;
import org.springframework.web.bind.annotation.*;

/**
 * Denne ressursen skal kun brukes til å hente data som veilarboppfolging har selv, uavhengig av integrasjoner. Hvis
 * behovet ikke dekkes av veilarboppfolging på egen hånd, bruk eksisterende OppfolgingRessurs
 */
@RestController

@RequestMapping("/api/v2")
public class UnderOppfolgingV2Controller {

    private final OppfolgingService oppfolgingService;
    private final AuthService authService;

    public UnderOppfolgingV2Controller(OppfolgingService oppfolgingService, AuthService authService) {
        this.oppfolgingService = oppfolgingService;
        this.authService = authService;
    }

    @PostMapping("/hent-underOppfolging")
    public UnderOppfolgingDTO underOppfolging(@RequestBody(required = false) UnderOppfolgingRequest underOppfolgingRequest) {
        Fnr maybeFodselsnummer = underOppfolgingRequest == null ? null : underOppfolgingRequest.getFnr();
        Fnr fodselsnummer = authService.hentIdentForEksternEllerIntern(maybeFodselsnummer);
        return oppfolgingService.oppfolgingData(fodselsnummer);
    }
}
