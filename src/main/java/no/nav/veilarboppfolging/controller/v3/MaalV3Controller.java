package no.nav.veilarboppfolging.controller.v3;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.response.Maal;
import no.nav.veilarboppfolging.controller.v3.request.MaalForPersonRequest;
import no.nav.veilarboppfolging.controller.v3.request.MaalRequest;
import no.nav.veilarboppfolging.repository.entity.MaalEntity;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.MaalService;
import no.nav.veilarboppfolging.utils.DtoMappers;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static no.nav.veilarboppfolging.utils.DtoMappers.tilDto;

@RestController
@RequestMapping("/api/v3")
@RequiredArgsConstructor
public class MaalV3Controller {

	private final MaalService maalService;

	private final AuthService authService;

	@PostMapping("/hent-maal")
	public Maal hentMaal(@RequestBody MaalForPersonRequest maalForPersonRequest) {
		Fnr fodselsnummer = authService.hentIdentForEksternEllerIntern(maalForPersonRequest.fnr());
		return tilDto(maalService.hentMal(fodselsnummer));
	}

	@PostMapping("/maal/hent-alle")
	public List<Maal> hentMaalListe(@RequestBody(required = false) MaalForPersonRequest maalForPersonRequest) {
		Fnr maybeFodselsnummer = maalForPersonRequest == null ? null : maalForPersonRequest.fnr();
		Fnr fodselsnummer = authService.hentIdentForEksternEllerIntern(maybeFodselsnummer);
		List<MaalEntity> malDataList = maalService.hentMaalList(fodselsnummer);

		return malDataList.stream()
				.map(DtoMappers::tilDto)
				.collect(toList());
	}

	@PostMapping("/maal")
	public Maal oppdaterMaal(@RequestBody MaalRequest maalRequest) {
		Fnr fodselsnummer = authService.hentIdentForEksternEllerIntern(maalRequest.fnr());
		String endretAvVeileder = authService.erEksternBruker() ? null : authService.getInnloggetBrukerIdent();

		return tilDto(maalService.oppdaterMaal(maalRequest.mal(), fodselsnummer, endretAvVeileder));
	}

}
