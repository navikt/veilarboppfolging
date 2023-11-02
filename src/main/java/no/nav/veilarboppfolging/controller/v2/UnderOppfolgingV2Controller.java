package no.nav.veilarboppfolging.controller.v2;

import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@RequestMapping("/api/v2")
public class UnderOppfolgingV2Controller {

    private final OppfolgingService oppfolgingService;

    private final AuthService authService;

    @PostMapping("/hent-underOppfolging")
    public UnderOppfolgingDTO underOppfolging(@RequestBody UnderOppfolgingRequest underOppfolgingRequest) {
        // TODO: Hvis dette endepunktet kun blir brukt av interne brukere så kan vi gjøre fnr query param required
        Fnr fodselsnummer = authService.hentIdentForEksternEllerIntern(underOppfolgingRequest.fnr());
        return oppfolgingService.oppfolgingData(fodselsnummer);
    }

}
