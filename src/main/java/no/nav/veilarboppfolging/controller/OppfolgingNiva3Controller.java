package no.nav.veilarboppfolging.controller;

import lombok.RequiredArgsConstructor;
import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.response.UnderOppfolgingNiva3DTO;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.OppfolgingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static java.lang.String.valueOf;

/**
 * Denne ressursen er dedikert til å håndtere forespørsler på vegne av brukere som har innloggingstoken med nivå 3.
 * Ressursen ble opprettet for å dekke behov fra Ditt Nav.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/niva3")
public class OppfolgingNiva3Controller {

    private final OppfolgingService oppfolgingService;

    private final MetricsClient metricsClient;

    private final AuthService authService;

    @GetMapping("/underoppfolging")
    public UnderOppfolgingNiva3DTO underOppfolgingNiva3() {
        Fnr fnr = Fnr.of(authService.getInnloggetBrukerIdent());

        UnderOppfolgingNiva3DTO underOppfolgingNiva3DTO = new UnderOppfolgingNiva3DTO()
                .setUnderOppfolging(oppfolgingService.underOppfolgingNiva3(fnr));

        Event event = new Event("request.niva3.underoppfolging");
        event.addTagToReport("underoppfolging", valueOf(underOppfolgingNiva3DTO.isUnderOppfolging()));
        metricsClient.report(event);

        return underOppfolgingNiva3DTO;
    }

}
