package no.nav.veilarboppfolging.controller.v2;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.BadRequestException;
import no.nav.veilarboppfolging.client.digdir_krr.KRRData;
import no.nav.veilarboppfolging.controller.request.VeilederBegrunnelseDTO;
import no.nav.veilarboppfolging.controller.v2.response.ManuellStatusV2Response;
import no.nav.veilarboppfolging.controller.v2.response.ManuellV2Response;
import no.nav.veilarboppfolging.repository.enums.KodeverkBruker;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.ManuellStatusService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/manuell")
@RequiredArgsConstructor
public class ManuellStatusV2Controller {

    private final static List<String> ALLOWLIST = List.of("veilarbdialog", "veilarbaktivitet");

    private final AuthService authService;

    private final ManuellStatusService manuellStatusService;

    @GetMapping
    @Deprecated(forRemoval = true)
    public ManuellV2Response hentErUnderManuellOppfolging(@RequestParam("fnr") Fnr fnr) {
        authService.sjekkLesetilgangMedFnr(fnr);

        boolean erManuell = manuellStatusService.erManuell(fnr);

        return new ManuellV2Response(erManuell);
    }

    @GetMapping("/status")
	@Deprecated(forRemoval = true)
    public ManuellStatusV2Response hentManuellStatus(@RequestParam("fnr") Fnr fnr) {
        if (authService.erEksternBruker()) {
            authService.sjekkAtApplikasjonErIAllowList(ALLOWLIST);
            authService.sjekkLesetilgangMedFnr(fnr);
        } else if (authService.erSystemBrukerFraAzureAd()) {
            authService.sjekkAtApplikasjonErIAllowList(ALLOWLIST);
        } else  {
            authService.sjekkLesetilgangMedFnr(fnr);
        }

        KRRData kontaktinfo = manuellStatusService.hentDigdirKontaktinfo(fnr);
        boolean erManuell = manuellStatusService.erManuell(fnr);

        return new ManuellStatusV2Response(
                erManuell,
                new ManuellStatusV2Response.KrrStatus(
                        kontaktinfo.isKanVarsles(), kontaktinfo.isReservert()
                )
        );
    }

    /**
     * Brukes av veilarbpersonflatefs for å manuelt trigge synkronisering av manuell status med reservasjon fra DIGDIR(KRR).
     * @param fnr fnr/dnr til bruker som synkroniseringen skal gjøres på.
     */

    @PostMapping("/synkroniser-med-dkif")
	@Deprecated(forRemoval = true)
    public void synkroniserManuellStatusMedDigdir(@RequestParam("fnr") Fnr fnr) {
        authService.skalVereInternBruker();
        manuellStatusService.synkroniserManuellStatusMedDigdir(fnr);
    }

    @PostMapping("/sett-manuell")
	@Deprecated(forRemoval = true)
    public void settTilManuell(@RequestBody VeilederBegrunnelseDTO dto, @RequestParam("fnr") Fnr fnr) {
        authService.skalVereInternBruker();

        manuellStatusService.oppdaterManuellStatus(
                fnr, true, dto.begrunnelse,
                KodeverkBruker.NAV, authService.getInnloggetVeilederIdent()
        );
    }

    @PostMapping("/sett-digital")
	@Deprecated(forRemoval = true)
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
            throw new BadRequestException("VeilederBegrunnelseDTO er påkrevd når man skal sette bruker til digital");
        }

        manuellStatusService.oppdaterManuellStatus(
                fodselsnummer, false, dto.begrunnelse,
                KodeverkBruker.NAV, authService.getInnloggetBrukerIdent()
        );
    }


}
