package no.nav.veilarboppfolging.controller;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.BadRequestException;
import no.nav.veilarboppfolging.NotFoundException;
import no.nav.veilarboppfolging.controller.request.StartKvpDTO;
import no.nav.veilarboppfolging.controller.request.StoppKvpDTO;
import no.nav.veilarboppfolging.controller.request.VeilederBegrunnelseDTO;
import no.nav.veilarboppfolging.controller.response.*;
import no.nav.veilarboppfolging.repository.enums.KodeverkBruker;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.KvpService;
import no.nav.veilarboppfolging.service.ManuellStatusService;
import no.nav.veilarboppfolging.service.OppfolgingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static no.nav.veilarboppfolging.utils.DtoMappers.*;

@RestController
@RequestMapping("/api/oppfolging")
public class OppfolgingController {
    private final OppfolgingService oppfolgingService;
    private final KvpService kvpService;
    private final AuthService authService;
    private final ManuellStatusService manuellStatusService;

    @Autowired
    public OppfolgingController(OppfolgingService oppfolgingService, KvpService kvpService, AuthService authService, ManuellStatusService manuellStatusService) {
        this.oppfolgingService = oppfolgingService;
        this.kvpService = kvpService;
        this.authService = authService;
        this.manuellStatusService = manuellStatusService;
    }

    @GetMapping("/me")
    public Bruker hentBrukerInfo() {
        return new Bruker(
                authService.getInnloggetBrukerIdent(),
                authService.erInternBruker(),
                authService.erEksternBruker()
        );
    }

    // TODO: Ikke returner OppfolgingStatus
    @PostMapping("/settManuell")
    public OppfolgingStatus settTilManuell(@RequestBody VeilederBegrunnelseDTO dto, @RequestParam("fnr") Fnr fnr) {
        authService.skalVereInternBruker();

        manuellStatusService.oppdaterManuellStatus(
                fnr, true, dto.getBegrunnelse(),
                KodeverkBruker.NAV, authService.getInnloggetVeilederIdent()
        );

        return tilDto(oppfolgingService.hentOppfolgingsStatus(fnr), authService.erInternBruker());
    }

    // TODO: Ikke returner OppfolgingStatus
    @PostMapping("/settDigital")
    public OppfolgingStatus settTilDigital(
            @RequestBody(required = false) VeilederBegrunnelseDTO dto,
            @RequestParam(value = "fnr", required = false) Fnr fnr) {
        Fnr fodselsnummer = authService.hentIdentForEksternEllerIntern(fnr);

        if (authService.erEksternBruker()) {
            manuellStatusService.settDigitalBruker(fodselsnummer);
            return tilDto(oppfolgingService.hentOppfolgingsStatus(fodselsnummer), authService.erInternBruker());
        }

        // Påkrevd for intern bruker
        if (dto == null) {
            throw new BadRequestException("VeilederBegrunnelseDTO er påkrevd");
        }

        manuellStatusService.oppdaterManuellStatus(
                fodselsnummer, false, dto.getBegrunnelse(),
                KodeverkBruker.NAV, hentBrukerInfo().getId()
        );

        return tilDto(oppfolgingService.hentOppfolgingsStatus(fodselsnummer), authService.erInternBruker());
    }

    @PostMapping("/startKvp")
    public ResponseEntity startKvp(@RequestBody StartKvpDTO startKvp, @RequestParam("fnr") Fnr fnr) {
        authService.skalVereInternBruker();
        kvpService.startKvp(fnr, startKvp.getBegrunnelse());
        return ResponseEntity.status(204).build();
    }

    @PostMapping("/stoppKvp")
    public ResponseEntity stoppKvp(@RequestBody StoppKvpDTO stoppKvp, @RequestParam("fnr") Fnr fnr) {
        authService.skalVereInternBruker();
        kvpService.stopKvp(fnr, stoppKvp.getBegrunnelse());
        return ResponseEntity.status(204).build();
    }

    @GetMapping("/veilederTilgang")
    public VeilederTilgang hentVeilederTilgang(@RequestParam("fnr") Fnr fnr) {
        authService.skalVereInternBruker();
        return oppfolgingService.hentVeilederTilgang(fnr);
    }

    @GetMapping("/oppfolgingsperiode/{uuid}")
    public OppfolgingPeriodeMinimalDTO hentOppfolgingsPeriode(@PathVariable String uuid) {
        var maybePeriode = oppfolgingService.hentOppfolgingsperiode(uuid);

        if (maybePeriode.isEmpty()) {
            throw new NotFoundException("Kunne ikke finne oppfolgingsperiode");
        }

        var periode = maybePeriode.get();

        authService.sjekkLesetilgangMedAktorId(AktorId.of(periode.getAktorId()));

        return tilOppfolgingPeriodeMinimalDTO(periode);

    }

    /**
     * Returnerer informasjon om bruker har oppfølgingsperioder
     * på flere forskjellige aktørIder
     *
     * @param fnr fødselsnummer på brukeren
     * @return true dersom bruker både har flere aktørIder og har oppfølgingsperioder på flere av disse
     */
    @GetMapping("/harFlereAktorIderMedOppfolging")
    public boolean harFlereAktorIderMedOppfolging(@RequestParam(value = "fnr", required = false) Fnr fnr) {
        Fnr fodselsnummer = authService.hentIdentForEksternEllerIntern(fnr);
        return oppfolgingService.hentHarFlereAktorIderMedOppfolging(fodselsnummer);
    }

}
