package no.nav.veilarboppfolging.controller.v2;

import lombok.RequiredArgsConstructor;
import no.nav.common.auth.context.UserRole;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.client.dkif.DkifKontaktinfo;
import no.nav.veilarboppfolging.controller.request.VeilederBegrunnelseDTO;
import no.nav.veilarboppfolging.controller.v2.response.ManuellStatusV2Response;
import no.nav.veilarboppfolging.controller.v2.response.ManuellV2Response;
import no.nav.veilarboppfolging.repository.enums.KodeverkBruker;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.ManuellStatusService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v2/manuell")
@RequiredArgsConstructor
public class ManuellStatusV2Controller {

    private final static List<UserRole> INTERN_OR_SYSTEM = List.of(UserRole.INTERN, UserRole.SYSTEM);

    private final AuthService authService;

    private final ManuellStatusService manuellStatusService;

    @GetMapping
    public ManuellV2Response hentErUnderManuellOppfolging(@RequestParam("fnr") Fnr fnr) {
        authService.skalVereInternBruker();
        authService.sjekkLesetilgangMedFnr(fnr);

        boolean erManuell = manuellStatusService.erManuell(fnr);

        return new ManuellV2Response(erManuell);
    }

    @GetMapping("/status")
    public ManuellStatusV2Response hentManuellStatus(@RequestParam("fnr") Fnr fnr) {
        authService.skalVereEnAv(INTERN_OR_SYSTEM);
        authService.sjekkLesetilgangMedFnr(fnr);

        DkifKontaktinfo kontaktinfo = manuellStatusService.hentDkifKontaktinfo(fnr);
        boolean erManuell = manuellStatusService.erManuell(fnr);

        return new ManuellStatusV2Response(
                erManuell,
                new ManuellStatusV2Response.KrrStatus(
                        kontaktinfo.isKanVarsles(), kontaktinfo.isReservert()
                )
        );
    }

    /**
     * Brukes av veilarbpersonflatefs for å manuelt trigge synkronisering av manuell status med reservasjon fra DKIF(KRR).
     * @param fnr fnr/dnr til bruker som synkroniseringen skal gjøres på.
     */
    @PostMapping("/synkroniser-med-dkif")
    public void synkroniserManuellStatusMedDkif(@RequestParam("fnr") Fnr fnr) {
        authService.skalVereInternBruker();
        manuellStatusService.synkroniserManuellStatusMedDkif(fnr);
    }

    @PostMapping("/sett-manuell")
    public void settTilManuell(@RequestBody VeilederBegrunnelseDTO dto, @RequestParam("fnr") Fnr fnr) {
        authService.skalVereInternBruker();

        manuellStatusService.oppdaterManuellStatus(
                fnr, true, dto.begrunnelse,
                KodeverkBruker.NAV, authService.getInnloggetBrukerIdent()
        );
    }

    @PostMapping("/sett-digital")
    public void settTilDigital(
            @RequestBody(required = false) VeilederBegrunnelseDTO dto,
            @RequestParam(value = "fnr", required = false) Fnr fnr
    ) {
        Fnr fodselsnummer = authService.hentIdentForEksternEllerIntern(fnr);

        if (authService.erEksternBruker()) {
            manuellStatusService.settDigitalBruker(fodselsnummer);
            return;
        }

        // Påkrevd for intern bruker
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        manuellStatusService.oppdaterManuellStatus(
                fodselsnummer, false, dto.begrunnelse,
                KodeverkBruker.NAV, authService.getInnloggetBrukerIdent()
        );
    }


}