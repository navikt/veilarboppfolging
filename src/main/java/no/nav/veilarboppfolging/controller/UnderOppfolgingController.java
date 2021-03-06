package no.nav.veilarboppfolging.controller;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.response.UnderOppfolgingDTO;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.OppfolgingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Denne ressursen skal kun brukes til å hente data som veilarboppfolging har selv, uavhengig av integrasjoner. Hvis
 * behovet ikke dekkes av veilarboppfolging på egen hånd, bruk eksisterende OppfolgingRessurs
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/underoppfolging")
public class UnderOppfolgingController {

    private final OppfolgingService oppfolgingService;

    private final AuthService authService;

    @GetMapping
    public UnderOppfolgingDTO underOppfolging(@RequestParam(value = "fnr", required = false) Fnr fnr) {
        // TODO: Hvis dette endepunktet kun blir brukt av interne brukere så kan vi gjøre fnr query param required
        Fnr fodselsnummer = authService.hentIdentForEksternEllerIntern(fnr);
        return oppfolgingService.oppfolgingData(fodselsnummer);
    }

}
