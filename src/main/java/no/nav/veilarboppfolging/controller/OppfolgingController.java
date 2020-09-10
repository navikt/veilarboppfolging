package no.nav.veilarboppfolging.controller;

import no.nav.veilarboppfolging.controller.domain.*;
import no.nav.veilarboppfolging.domain.InnstillingsHistorikk;
import no.nav.veilarboppfolging.domain.KodeverkBruker;
import no.nav.veilarboppfolging.domain.VeilederTilgang;
import no.nav.veilarboppfolging.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static no.nav.veilarboppfolging.utils.DtoMappers.tilDto;

@RequestMapping("/api/oppfolging")
@RestController
public class OppfolgingController {
    
    private final OppfolgingService oppfolgingService;
    
    private final KvpService kvpService;
    
    private final HistorikkService historikkService;
    
    private final AuthService authService;

    private final EskaleringService eskaleringService;

    private final ManuellStatusService manuellStatusService;

    @Autowired
    public OppfolgingController(
            OppfolgingService oppfolgingService,
            KvpService kvpService,
            HistorikkService historikkService,
            AuthService authService,
            EskaleringService eskaleringService,
            ManuellStatusService manuellStatusService
    ) {
        this.oppfolgingService = oppfolgingService;
        this.kvpService = kvpService;
        this.historikkService = historikkService;
        this.authService = authService;
        this.eskaleringService = eskaleringService;
        this.manuellStatusService = manuellStatusService;
    }

    @GetMapping("/me")
    public Bruker hentBrukerInfo() {
        return new Bruker()
                .setId(authService.getInnloggetBrukerIdent())
                .setErVeileder(authService.erInternBruker())
                .setErBruker(authService.erEksternBruker());
    }

    @GetMapping
    public OppfolgingStatus hentOppfolgingsStatus(@RequestParam(value = "fnr", required = false) String fnr) {
        String fodselsnummer = authService.hentIdentForEksternEllerIntern(fnr);
        return tilDto(oppfolgingService.hentOppfolgingsStatus(fodselsnummer), authService.erInternBruker());
    }

    @PostMapping("/startOppfolging")
    public OppfolgingStatus startOppfolging(@RequestParam("fnr") String fnr) {
        authService.skalVereInternBruker();
        return tilDto(oppfolgingService.startOppfolging(fnr), authService.erInternBruker());
    }

    @GetMapping("/avslutningStatus")
    public OppfolgingStatus hentAvslutningStatus(@RequestParam("fnr") String fnr) {
        authService.skalVereInternBruker();
        return tilDto(oppfolgingService.hentAvslutningStatus(fnr), authService.erInternBruker());
    }

    @PostMapping("/avsluttOppfolging")
    public OppfolgingStatus avsluttOppfolging(@RequestBody VeilederBegrunnelseDTO dto, @RequestParam("fnr") String fnr) {
        authService.skalVereInternBruker();
        return tilDto(oppfolgingService.avsluttOppfolging(
                fnr,
                dto.veilederId,
                dto.begrunnelse
        ), authService.erInternBruker());
    }

    // TODO: Ikke returner OppfolgingStatus
    @PostMapping("/settManuell")
    public OppfolgingStatus settTilManuell(@RequestBody VeilederBegrunnelseDTO dto, @RequestParam("fnr") String fnr) {
        authService.skalVereInternBruker();

        manuellStatusService.oppdaterManuellStatus(
                fnr, true, dto.begrunnelse,
                KodeverkBruker.NAV, authService.getInnloggetBrukerIdent()
        );

        return tilDto(oppfolgingService.hentOppfolgingsStatus(fnr),  authService.erInternBruker());
    }

    // TODO: Ikke returner OppfolgingStatus
    @PostMapping("/settDigital")
    public OppfolgingStatus settTilDigital(@RequestBody(required = false) VeilederBegrunnelseDTO dto, @RequestParam(value = "fnr", required = false) String fnr) {
        String fodselsnummer = authService.hentIdentForEksternEllerIntern(fnr);

        if (authService.erEksternBruker()) {
            manuellStatusService.settDigitalBruker(fodselsnummer);
            return tilDto(oppfolgingService.hentOppfolgingsStatus(fodselsnummer), authService.erInternBruker());
        }

        // Påkrevd for intern bruker
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        manuellStatusService.oppdaterManuellStatus(
                fodselsnummer, false, dto.begrunnelse,
                KodeverkBruker.NAV, hentBrukerInfo().getId()
        );

        return tilDto(oppfolgingService.hentOppfolgingsStatus(fodselsnummer), authService.erInternBruker());
    }

    @GetMapping("/innstillingsHistorikk")
    public List<InnstillingsHistorikk> hentInnstillingsHistorikk(@RequestParam("fnr") String fnr) {
        authService.skalVereInternBruker();
        return historikkService.hentInstillingsHistorikk(fnr);
    }

    @PostMapping("/startEskalering")
    public ResponseEntity startEskalering(@RequestBody StartEskaleringDTO startEskalering, @RequestParam("fnr") String fnr) {
        authService.skalVereInternBruker();
        eskaleringService.startEskalering(
                fnr,
                authService.getInnloggetVeilederIdent(),
                startEskalering.getBegrunnelse(),
                startEskalering.getDialogId()
        );

        return ResponseEntity.status(204).build();
    }

    @PostMapping("/stoppEskalering")
    public ResponseEntity stoppEskalering(@RequestBody StoppEskaleringDTO stoppEskalering, @RequestParam("fnr") String fnr) {
        authService.skalVereInternBruker();
        eskaleringService.stoppEskalering(fnr, authService.getInnloggetVeilederIdent(), stoppEskalering.getBegrunnelse());
        return ResponseEntity.status(204).build();
    }

    @PostMapping("/startKvp")
    public ResponseEntity startKvp(@RequestBody StartKvpDTO startKvp, @RequestParam("fnr") String fnr) {
        authService.skalVereInternBruker();
        kvpService.startKvp(fnr, startKvp.getBegrunnelse());
        return ResponseEntity.status(204).build();
    }

    @PostMapping("/stoppKvp")
    public ResponseEntity stoppKvp(@RequestBody StoppKvpDTO stoppKvp, @RequestParam("fnr") String fnr) {
        authService.skalVereInternBruker();
        kvpService.stopKvp(fnr, stoppKvp.getBegrunnelse());
        return ResponseEntity.status(204).build();
    }

    @GetMapping("/veilederTilgang")
    public VeilederTilgang hentVeilederTilgang(@RequestParam("fnr") String fnr) {
        authService.skalVereInternBruker();
        return oppfolgingService.hentVeilederTilgang(fnr);
    }

}
