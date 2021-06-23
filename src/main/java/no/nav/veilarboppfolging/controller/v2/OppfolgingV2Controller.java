package no.nav.veilarboppfolging.controller.v2;

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

@RestController
@RequestMapping("/api/v2/oppfolging")
@RequiredArgsConstructor
public class OppfolgingV2Controller {

    private final OppfolgingService oppfolgingService;

    private final MetricsClient metricsClient;

    private final AuthService authService;

    @GetMapping("/niva3")
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
