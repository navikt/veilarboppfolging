package no.nav.veilarboppfolging.controller;

import no.nav.veilarboppfolging.domain.*;
import no.nav.veilarboppfolging.controller.domain.Bruker;
import no.nav.veilarboppfolging.controller.domain.OppfolgingStatus;
import no.nav.veilarboppfolging.controller.domain.StartEskaleringDTO;
import no.nav.veilarboppfolging.controller.domain.StartKvpDTO;
import no.nav.veilarboppfolging.controller.domain.StoppEskaleringDTO;
import no.nav.veilarboppfolging.controller.domain.StoppKvpDTO;
import no.nav.veilarboppfolging.controller.domain.VeilederBegrunnelseDTO;
import no.nav.veilarboppfolging.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static no.nav.veilarboppfolging.utils.DtoMappers.tilDto;

@RequestMapping("/api/oppfolging")
@RestController
public class OppfolgingController {
    
    private final OppfolgingService oppfolgingService;
    
    private final KvpService kvpService;
    
    private final HistorikkService historikkService;
    
    private final MalService malService;

    private final AktiverBrukerService aktiverBrukerService;
    
    private final AuthService authService;

    @Autowired
    public OppfolgingController(
            OppfolgingService oppfolgingService,
            KvpService kvpService,
            HistorikkService historikkService,
            MalService malService,
            AktiverBrukerService aktiverBrukerService,
            AuthService authService
    ) {
        this.oppfolgingService = oppfolgingService;
        this.kvpService = kvpService;
        this.historikkService = historikkService;
        this.malService = malService;
        this.aktiverBrukerService = aktiverBrukerService;
        this.authService = authService;
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

    @PostMapping("/settManuell")
    public OppfolgingStatus settTilManuell(@RequestBody VeilederBegrunnelseDTO dto, @RequestParam("fnr") String fnr) {
        authService.skalVereInternBruker();
        return tilDto(oppfolgingService.oppdaterManuellStatus(fnr,
                true,
                dto.begrunnelse,
                KodeverkBruker.NAV,
                hentBrukerInfo().getId()),
                authService.erInternBruker()
        );
    }

    @PostMapping("/settDigital")
    public OppfolgingStatus settTilDigital(@RequestBody VeilederBegrunnelseDTO dto, @RequestParam(value = "fnr", required = false) String fnr) {
        String fodselsnummer = authService.hentIdentForEksternEllerIntern(fnr);

        if (authService.erEksternBruker()) {
            return tilDto(oppfolgingService.settDigitalBruker(fodselsnummer), authService.erInternBruker());
        }

        return tilDto(oppfolgingService.oppdaterManuellStatus(fodselsnummer,
                false,
                dto.begrunnelse,
                KodeverkBruker.NAV,
                hentBrukerInfo().getId()),
                authService.erInternBruker()
        );
    }

    @GetMapping("/innstillingsHistorikk")
    public List<InnstillingsHistorikk> hentInnstillingsHistorikk(@RequestParam("fnr") String fnr) {
        authService.skalVereInternBruker();
        return historikkService.hentInstillingsHistorikk(fnr);
    }

    @PostMapping("/startEskalering")
    public void startEskalering(@RequestBody StartEskaleringDTO startEskalering, @RequestParam("fnr") String fnr) {
        authService.skalVereInternBruker();
        oppfolgingService.startEskalering(
                fnr,
                startEskalering.getBegrunnelse(),
                startEskalering.getDialogId()
        );
    }

    @PostMapping("/stoppEskalering")
    public void stoppEskalering(@RequestBody StoppEskaleringDTO stoppEskalering, @RequestParam("fnr") String fnr) {
        authService.skalVereInternBruker();
        oppfolgingService.stoppEskalering(fnr, stoppEskalering.getBegrunnelse());
    }

    @PostMapping("/startKvp")
    public void startKvp(@RequestBody StartKvpDTO startKvp, @RequestParam("fnr") String fnr) {
        authService.skalVereInternBruker();
        kvpService.startKvp(fnr, startKvp.getBegrunnelse());
    }

    @PostMapping("/stoppKvp")
    public void stoppKvp(@RequestBody StoppKvpDTO stoppKvp, @RequestParam("fnr") String fnr) {
        authService.skalVereInternBruker();
        kvpService.stopKvp(fnr, stoppKvp.getBegrunnelse());
    }

    @GetMapping("/veilederTilgang")
    public VeilederTilgang hentVeilederTilgang(@RequestParam("fnr") String fnr) {
        authService.skalVereInternBruker();
        return oppfolgingService.hentVeilederTilgang(fnr);
    }

}
