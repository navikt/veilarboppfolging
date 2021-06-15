package no.nav.veilarboppfolging.controller;

import no.nav.veilarboppfolging.controller.response.UnderOppfolgingDTO;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.OppfolgingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Denne ressursen skal kun brukes til å hente data som veilarboppfolging har selv, uavhengig av integrasjoner. Hvis
 * behovet ikke dekkes av veilarboppfolging på egen hånd, bruk eksisterende OppfolgingRessurs
 */
@RestController
@RequestMapping("/api/underoppfolging")
public class UnderOppfolgingController {

    private final OppfolgingService oppfolgingService;

    private final AuthService authService;

    @Autowired
    public UnderOppfolgingController(OppfolgingService oppfolgingService, AuthService authService) {
        this.oppfolgingService = oppfolgingService;
        this.authService = authService;
    }

    @GetMapping
    public UnderOppfolgingDTO underOppfolging(@RequestParam(value = "fnr", required = false) String fnr) {
        // TODO: Hvis dette endepunktet kun blir brukt av interne brukere så kan vi gjøre fnr query param required
        String fodselsnummer = authService.hentIdentForEksternEllerIntern(fnr);
        return oppfolgingService.oppfolgingData(fodselsnummer);
    }

}
