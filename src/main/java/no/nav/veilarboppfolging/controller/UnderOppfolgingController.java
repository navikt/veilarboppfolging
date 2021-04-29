package no.nav.veilarboppfolging.controller;

import lombok.RequiredArgsConstructor;
import no.nav.veilarboppfolging.config.EnvironmentProperties;
import no.nav.veilarboppfolging.controller.domain.UnderOppfolgingDTO;
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
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/underoppfolging")
public class UnderOppfolgingController {

    private final OppfolgingService oppfolgingService;

    private final AuthService authService;

    private final EnvironmentProperties properties;

    @GetMapping
    public UnderOppfolgingDTO underOppfolging(@RequestParam(value = "fnr", required = false) String fnr) {
        String fodselsnummer = authService.hentIdentFraTokenX(properties.getTokenXClientId())
                .orElseGet(() -> authService.hentIdentForEksternEllerIntern(fnr));

        return oppfolgingService.oppfolgingData(fodselsnummer);
    }

}
