package no.nav.veilarboppfolging.controller.v3;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.response.AvslutningStatus;
import no.nav.veilarboppfolging.controller.response.OppfolgingPeriodeDTO;
import no.nav.veilarboppfolging.controller.response.OppfolgingPeriodeMinimalDTO;
import no.nav.veilarboppfolging.controller.v2.request.AvsluttOppfolgingV2Request;
import no.nav.veilarboppfolging.controller.v2.response.UnderOppfolgingV2Response;
import no.nav.veilarboppfolging.controller.v3.request.OppfolgingRequest;
import no.nav.veilarboppfolging.repository.entity.KvpPeriodeEntity;
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.OppfolgingService;
import no.nav.veilarboppfolging.utils.DtoMappers;
import no.nav.veilarboppfolging.utils.auth.AuthorizeAktorId;
import no.nav.veilarboppfolging.utils.auth.AuthorizeFnr;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

import static no.nav.veilarboppfolging.utils.DtoMappers.*;

@RestController
@RequestMapping("/api/v3")
@RequiredArgsConstructor
public class OppfolgingV3Controller {
	private final OppfolgingService oppfolgingService;

	private final AuthService authService;

	@AuthorizeFnr(allowlist = {"veilarbvedtaksstotte", "veilarbdialog", "veilarbaktivitet", "veilarbregistrering", "veilarbportefolje"})
	@PostMapping("/hent-oppfolging")
	public UnderOppfolgingV2Response underOppfolging(@RequestBody OppfolgingRequest oppfolgingRequest) {
		return new UnderOppfolgingV2Response(oppfolgingService.erUnderOppfolging(oppfolgingRequest.fnr()));
	}

	@PostMapping("/oppfolging/start")
	public ResponseEntity<?> startOppfolging(@RequestBody OppfolgingRequest oppfolgingRequest) {
		authService.skalVereInternBruker();
		oppfolgingService.startOppfolging(oppfolgingRequest.fnr());
		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
	}

	@PostMapping("/oppfolging/hent-avslutning-status")
	public AvslutningStatus hentAvslutningStatus(@RequestBody OppfolgingRequest oppfolgingRequest) {
		authService.skalVereInternBruker();
		return tilDto(oppfolgingService.hentAvslutningStatus(oppfolgingRequest.fnr()));
	}

	@AuthorizeFnr(allowlist = {"veilarbvedtaksstotte", "veilarbdialog", "veilarbaktivitet"})
	@PostMapping("/oppfolging/hent-gjeldende-periode")
	public ResponseEntity<OppfolgingPeriodeMinimalDTO> hentGjeldendePeriode(@RequestBody OppfolgingRequest oppfolgingRequest) {
		return oppfolgingService.hentGjeldendeOppfolgingsperiode(oppfolgingRequest.fnr())
				.map(DtoMappers::tilOppfolgingPeriodeMinimalDTO)
				.map(op -> new ResponseEntity<>(op, HttpStatus.OK))
				.orElse(new ResponseEntity<>(HttpStatus.NO_CONTENT));
	}

	@PostMapping(value = "/oppfolging/hent-perioder")
	public List<OppfolgingPeriodeDTO> hentOppfolgingsperioder(@RequestBody OppfolgingRequest oppfolgingRequest) {
		AktorId aktorId = authService.getAktorIdOrThrow(oppfolgingRequest.fnr());
		return hentOppfolgingsperioder(aktorId);
	}

	private List<OppfolgingPeriodeDTO> hentOppfolgingsperioder(AktorId aktorId) {
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
