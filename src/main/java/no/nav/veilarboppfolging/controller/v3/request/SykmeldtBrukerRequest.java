package no.nav.veilarboppfolging.controller.v3.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.request.SykmeldtBrukerType;

public record SykmeldtBrukerRequest(
	@JsonProperty(required = true) Fnr fnr,
	SykmeldtBrukerType sykmeldtBrukerType
) {
}
