package no.nav.veilarboppfolging.controller.v2;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.response.AvslutningStatus;
import no.nav.veilarboppfolging.controller.response.OppfolgingPeriodeDTO;
import no.nav.veilarboppfolging.controller.response.OppfolgingPeriodeMinimalDTO;
import no.nav.veilarboppfolging.controller.v2.request.AvsluttOppfolgingV2Request;
import no.nav.veilarboppfolging.controller.v2.response.UnderOppfolgingV2Response;
import no.nav.veilarboppfolging.repository.entity.KvpPeriodeEntity;
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity;
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
    private final static List<String> ALLOWLIST = List.of("veilarbvedtaksstotte");
    private final OppfolgingService oppfolgingService;

    private final AuthService authService;

    @GetMapping(params = "fnr")
    public UnderOppfolgingV2Response underOppfolging(@RequestParam(value = "fnr") Fnr fnr) {
        boolean harTilgangSomAADSystembruker = authService.erSystemBrukerFraAzureAd();
        if (!harTilgangSomAADSystembruker) {
            authService.sjekkLesetilgangMedFnr(fnr);
        }
        return new UnderOppfolgingV2Response(oppfolgingService.erUnderOppfolging(fnr));
    }

    @GetMapping(params = "aktorId")
    public UnderOppfolgingV2Response underOppfolging(@RequestParam(value = "aktorId") AktorId aktorId) {
        Fnr fnr = authService.getFnrOrThrow(aktorId);
        return underOppfolging(fnr);
    }

    @GetMapping
    public UnderOppfolgingV2Response underOppfolging() {
        if (!authService.erEksternBruker()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Som internbruker/systembruker er aktorId eller fnr påkrevd");
        }

        Fnr fnr = Fnr.of(authService.getInnloggetBrukerIdent());

        return new UnderOppfolgingV2Response(oppfolgingService.erUnderOppfolging(fnr));
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
    public OppfolgingPeriodeMinimalDTO hentOppfolgingsPeriode(@PathVariable("uuid") String uuid) {
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
        if (authService.erSystemBrukerFraAzureAd()) {
            authService.sjekkAtApplikasjonErIAllowList(ALLOWLIST);
        } else {
            authService.sjekkLesetilgangMedFnr(fnr);
        }

        return oppfolgingService.hentGjeldendeOppfolgingsperiode(fnr)
                .map(DtoMappers::tilOppfolgingPeriodeMinimalDTO)
                .map(op -> new ResponseEntity<>(op, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NO_CONTENT));
    }

    @GetMapping(value = "/perioder", params = "fnr")
    public List<OppfolgingPeriodeDTO> hentOppfolgingsperioder(@RequestParam("fnr") Fnr fnr) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);
        return hentOppfolgingsperioder(aktorId);
    }

    @GetMapping(value = "/perioder", params = "aktorId")
    public List<OppfolgingPeriodeDTO> hentOppfolgingsperioder(@RequestParam("aktorId") AktorId aktorId) {
        if (authService.erSystemBrukerFraAzureAd()) {
            authService.sjekkAtApplikasjonErIAllowList(ALLOWLIST);
        } else {
            authService.sjekkLesetilgangMedAktorId(aktorId);
        }
        return oppfolgingService.hentOppfolgingsperioder(aktorId)
                .stream()
                .map(this::filtrerKvpPerioder)
                .map(this::mapTilDto)
                .collect(Collectors.toList());
    }

    private OppfolgingPeriodeDTO mapTilDto(OppfolgingsperiodeEntity oppfolgingsperiode) {
        return tilOppfolgingPeriodeDTO(oppfolgingsperiode, !authService.erEksternBruker());
    }

    private OppfolgingsperiodeEntity filtrerKvpPerioder(OppfolgingsperiodeEntity periode) {
        if (!authService.erInternBruker() || periode.getKvpPerioder() == null || periode.getKvpPerioder().isEmpty()) {
            return periode;
        }

        List<KvpPeriodeEntity> kvpPeriodeEntities = periode
                .getKvpPerioder()
                .stream()
                .filter(it -> authService.harTilgangTilEnhet(it.getEnhet()))
                .collect(Collectors.toList());

        return periode.toBuilder().kvpPerioder(kvpPeriodeEntities).build();
    }


}
