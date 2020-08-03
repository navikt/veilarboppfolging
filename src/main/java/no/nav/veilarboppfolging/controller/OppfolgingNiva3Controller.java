package no.nav.veilarboppfolging.controller;

import static java.lang.String.valueOf;

import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import no.nav.veilarboppfolging.controller.domain.UnderOppfolgingNiva3DTO;
import no.nav.veilarboppfolging.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;

import no.nav.veilarboppfolging.service.OppfolgingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Denne ressursen er dedikert til å håndtere forespørsler på vegne av brukere som har innloggingstoken med nivå 3.
 * Ressursen ble opprettet for å dekke behov fra Ditt Nav.
 */
@RestController
@RequestMapping("/api/niva3")
public class OppfolgingNiva3Controller {

    private final OppfolgingService oppfolgingService;

    private final MetricsClient metricsClient;

    private final AuthService authService;

    @Autowired
    public OppfolgingNiva3Controller(OppfolgingService oppfolgingService, MetricsClient metricsClient, AuthService authService) {
        this.oppfolgingService = oppfolgingService;
        this.metricsClient = metricsClient;
        this.authService = authService;
    }

    @GetMapping("/underoppfolging")
    public UnderOppfolgingNiva3DTO underOppfolgingNiva3() {
        String fnr = authService.getInnloggetBrukerIdent();

        UnderOppfolgingNiva3DTO underOppfolgingNiva3DTO = new UnderOppfolgingNiva3DTO()
                .setUnderOppfolging(oppfolgingService.underOppfolgingNiva3(fnr));

        Event event = new Event("request.niva3.underoppfolging");
        event.addTagToReport("underoppfolging", valueOf(underOppfolgingNiva3DTO.isUnderOppfolging()));
        metricsClient.report(event);

        return underOppfolgingNiva3DTO;
    }

}
