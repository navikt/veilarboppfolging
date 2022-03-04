package no.nav.veilarboppfolging.controller.v2;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.response.AvslutningStatus;
import no.nav.veilarboppfolging.controller.response.OppfolgingPeriodeDTO;
import no.nav.veilarboppfolging.controller.response.OppfolgingPeriodeMinimalDTO;
import no.nav.veilarboppfolging.controller.v2.request.AvsluttOppfolgingV2Request;
import no.nav.veilarboppfolging.controller.v2.response.UnderOppfolgingV2Response;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.OppfolgingService;
import no.nav.veilarboppfolging.utils.DtoMappers;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

import static no.nav.veilarboppfolging.utils.DtoMappers.*;

@RestController
@RequestMapping("/api/v2/oppfolging")
@RequiredArgsConstructor
public class OppfolgingV2Controller {

    private final OppfolgingService oppfolgingService;

    private final AuthService authService;

    @GetMapping
    public UnderOppfolgingV2Response underOppfolging(@RequestParam(value = "fnr", required = false) Fnr fnr) {
        Fnr fodselsnummer = authService.hentIdentFraQueryParamEllerToken(fnr);
        return new UnderOppfolgingV2Response(oppfolgingService.erUnderOppfolging(fodselsnummer));
    }

    @PostMapping("/start")
    public ResponseEntity<?> startOppfolging(@RequestParam("fnr") Fnr fnr) {
        authService.skalVereInternBruker();
        oppfolgingService.startOppfolging(fnr);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/avslutt")
    public ResponseEntity<?> avsluttOppfolging(@RequestBody AvsluttOppfolgingV2Request request) {
        authService.skalVereInternBruker();
        oppfolgingService.avsluttOppfolging(request.getFnr(), request.getVeilederId().get(), request.getBegrunnelse());

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/avslutning-status")
    public AvslutningStatus hentAvslutningStatus(@RequestParam("fnr") Fnr fnr) {
        authService.skalVereInternBruker();
        return tilDto(oppfolgingService.hentAvslutningStatus(fnr));
    }

    @GetMapping("/periode/{uuid}")
    public OppfolgingPeriodeMinimalDTO hentOppfolgingsPeriode(@PathVariable("uuid") String uuid){
        var maybePeriode = oppfolgingService.hentOppfolgingsperiode(uuid);

        if (maybePeriode.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        var periode = maybePeriode.get();

        authService.sjekkLesetilgangMedAktorId(AktorId.of(periode.getAktorId()));

        return tilOppfolgingPeriodeMinimalDTO(periode);
    }

    @GetMapping("/periode/gjeldende")
    public ResponseEntity<OppfolgingPeriodeMinimalDTO> hentGjeldendePeriode(@RequestParam("fnr") Fnr fnr) {
        authService.skalVereSystemBruker();

        return oppfolgingService.hentGjeldendeOppfolgingsperiode(fnr)
                .map(DtoMappers::tilOppfolgingPeriodeMinimalDTO)
                .map(op -> new ResponseEntity<>(op, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NO_CONTENT));
    }

    @GetMapping(value = "/perioder", params = "fnr")
    public List<OppfolgingPeriodeDTO> hentOppfolgingsperioder(@RequestParam("fnr") Fnr fnr) {
        authService.skalVereSystemBruker();

        return oppfolgingService.hentOppfolgingsperioder(fnr)
                .stream()
                .map(op -> tilOppfolgingPeriodeDTO(op, true))
                .collect(Collectors.toList());
    }

    @GetMapping(value = "/perioder", params = "aktorId")
    public List<OppfolgingPeriodeDTO> hentOppfolgingsperioder(@RequestParam("aktorId") AktorId aktorId) {
        authService.skalVereSystemBruker();

        return oppfolgingService.hentOppfolgingsperioder(aktorId)
                .stream()
                .map(op -> tilOppfolgingPeriodeDTO(op, true))
                .collect(Collectors.toList());
    }


}
