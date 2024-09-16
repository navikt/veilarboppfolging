package no.nav.veilarboppfolging.controller.v3;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.BadRequestException;
import no.nav.veilarboppfolging.client.digdir_krr.KRRData;
import no.nav.veilarboppfolging.controller.v2.response.ManuellStatusV2Response;
import no.nav.veilarboppfolging.controller.v2.response.ManuellV2Response;
import no.nav.veilarboppfolging.client.digdir_krr.DigdirKontaktinfo;
import no.nav.veilarboppfolging.controller.v3.request.ManuellStatusRequest;
import no.nav.veilarboppfolging.controller.v3.request.VeilederBegrunnelseRequest;
import no.nav.veilarboppfolging.repository.enums.KodeverkBruker;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.ManuellStatusService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v3")
@RequiredArgsConstructor
public class ManuellStatusV3Controller {

	private final static List<String> ALLOWLIST = List.of("veilarbdialog", "veilarbaktivitet");

	private final AuthService authService;

	private final ManuellStatusService manuellStatusService;

	@PostMapping("/hent-manuell")
	public ManuellV2Response hentErUnderManuellOppfolging(@RequestBody ManuellStatusRequest manuellStatusRequest) {
		authService.sjekkLesetilgangMedFnr(manuellStatusRequest.fnr());

		boolean erManuell = manuellStatusService.erManuell(manuellStatusRequest.fnr());

		return new ManuellV2Response(erManuell);
	}

	@PostMapping("/manuell/hent-status")
	public ManuellStatusV2Response hentManuellStatus(@RequestBody ManuellStatusRequest manuellStatusRequest) {
		if (authService.erEksternBruker()) {
			authService.sjekkAtApplikasjonErIAllowList(ALLOWLIST);
			authService.harEksternBrukerTilgang(manuellStatusRequest.fnr());
		} else if (authService.erSystemBrukerFraAzureAd()) {
			authService.sjekkAtApplikasjonErIAllowList(ALLOWLIST);
		} else  {
			authService.sjekkLesetilgangMedFnr(manuellStatusRequest.fnr());
		}

		KRRData kontaktinfo = manuellStatusService.hentDigdirKontaktinfo(manuellStatusRequest.fnr());
		boolean erManuell = manuellStatusService.erManuell(manuellStatusRequest.fnr());

		return new ManuellStatusV2Response(
				erManuell,
				new ManuellStatusV2Response.KrrStatus(
						kontaktinfo.isKanVarsles(), kontaktinfo.isReservert()
				)
		);
	}

	/**
	 * Brukes av veilarbpersonflatefs for å manuelt trigge synkronisering av manuell status med reservasjon fra DIGDIR(KRR).
	 * @param manuellStatusRequest som har felt fnr. fnr/dnr til bruker som synkroniseringen skal gjøres på.
	 */

	@PostMapping("/manuell/synkroniser-med-dkif")
	public void synkroniserManuellStatusMedDigdir(@RequestBody ManuellStatusRequest manuellStatusRequest) {
		authService.skalVereInternBruker();
		manuellStatusService.synkroniserManuellStatusMedDigdir(manuellStatusRequest.fnr());
	}

	@PostMapping("/manuell/sett-manuell")
	public void settTilManuell(@RequestBody VeilederBegrunnelseRequest veilederBegrunnelseRequest) {
		authService.skalVereInternBruker();

		manuellStatusService.oppdaterManuellStatus(
				veilederBegrunnelseRequest.fnr(), true, veilederBegrunnelseRequest.begrunnelse(),
				KodeverkBruker.NAV, authService.getInnloggetVeilederIdent()
		);
	}

	@PostMapping("/manuell/sett-digital")
	public void settTilDigital(
			@RequestBody(required = false) VeilederBegrunnelseRequest veilederBegrunnelseRequest
	) {
		Fnr maybeFodselsnummer = veilederBegrunnelseRequest == null ? null : veilederBegrunnelseRequest.fnr();
		Fnr fodselsnummer = authService.hentIdentForEksternEllerIntern(maybeFodselsnummer);

		if (authService.erEksternBruker()) {
			manuellStatusService.settDigitalBruker(fodselsnummer);
			return;
		}

		// Påkrevd for intern bruker
		if (veilederBegrunnelseRequest == null) {
			throw new BadRequestException("veilederBegrunnelseRequest kan ikke være null");
		}

		manuellStatusService.oppdaterManuellStatus(
				fodselsnummer, false, veilederBegrunnelseRequest.begrunnelse(),
				KodeverkBruker.NAV, authService.getInnloggetBrukerIdent()
		);
	}


}
