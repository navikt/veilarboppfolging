package no.nav.veilarboppfolging.controller.v3;

import lombok.RequiredArgsConstructor;
import no.nav.veilarboppfolging.controller.response.HistorikkHendelse;
import no.nav.veilarboppfolging.controller.v3.request.HistorikkRequest;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.HistorikkService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v3")
@RequiredArgsConstructor
public class HistorikkV3Controller {

	private final HistorikkService historikkService;

	private final AuthService authService;

	@PostMapping("/hent-instillingshistorikk")
	public List<HistorikkHendelse> hentInnstillingsHistorikk(@RequestBody HistorikkRequest historikkRequest) {
		// TODO: Vurder å refaktorer DTOen, brukes kun av veilarbvisittkortfs
		authService.skalVereInternBruker();
		return historikkService.hentInstillingsHistorikk(historikkRequest.fnr());
	}

}
