package no.nav.veilarboppfolging.controller.v2;

import lombok.RequiredArgsConstructor;
import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.request.VeilederBegrunnelseDTO;
import no.nav.veilarboppfolging.controller.response.*;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.OppfolgingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.valueOf;
import static no.nav.veilarboppfolging.utils.DtoMappers.*;

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

    @GetMapping
    public UnderOppfolgingDTO underOppfolging(@RequestParam(value = "fnr", required = false) Fnr fnr) {
        // TODO: Hvis dette endepunktet kun blir brukt av interne brukere så kan vi gjøre fnr query param required
        Fnr fodselsnummer = authService.hentIdentForEksternEllerIntern(fnr);
        return oppfolgingService.oppfolgingData(fodselsnummer);
    }

    @GetMapping
    public OppfolgingStatus hentOppfolgingsStatus(@RequestParam(value = "fnr", required = false) Fnr fnr) {
        Fnr fodselsnummer = authService.hentIdentForEksternEllerIntern(fnr);
        return tilDto(oppfolgingService.hentOppfolgingsStatus(fodselsnummer), authService.erInternBruker());
    }

    @PostMapping("/start")
    public OppfolgingStatus startOppfolging(@RequestParam("fnr") Fnr fnr) {
        authService.skalVereInternBruker();
        return tilDto(oppfolgingService.startOppfolging(fnr), authService.erInternBruker());
    }

    @PostMapping("/avslutt")
    public AvslutningStatus avsluttOppfolging(@RequestBody VeilederBegrunnelseDTO dto, @RequestParam("fnr") Fnr fnr) {
        authService.skalVereInternBruker();

        return tilDto(oppfolgingService.avsluttOppfolging(
                fnr,
                dto.veilederId,
                dto.begrunnelse
        ));
    }

    @GetMapping("/avslutning-status")
    public AvslutningStatus hentAvslutningStatus(@RequestParam("fnr") Fnr fnr) {
        authService.skalVereInternBruker();
        return tilDto(oppfolgingService.hentAvslutningStatus(fnr));
    }

    @GetMapping("/periode/{uuid}")
    public OppfolgingPeriodeMinimalDTO hentOppfolgingsPeriode(@PathVariable String uuid){
        var maybePeriode = oppfolgingService.hentOppfolgingsperiode(uuid);

        if (maybePeriode.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        var periode = maybePeriode.get();

        authService.sjekkLesetilgangMedAktorId(AktorId.of(periode.getAktorId()));

        return tilOppfolgingPeriodeMinimalDTO(periode);

    }

    @GetMapping("/perioder")
    public List<OppfolgingPeriodeDTO> hentOppfolgingsperioder(@RequestParam("fnr") Fnr fnr) {
        authService.skalVereSystemBruker();

        return oppfolgingService.hentOppfolgingsperioder(fnr)
                .stream()
                .map(op -> tilOppfolgingPeriodeDTO(op, true))
                .collect(Collectors.toList());
    }

}
