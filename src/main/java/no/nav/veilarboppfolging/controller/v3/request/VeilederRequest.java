package no.nav.veilarboppfolging.controller.v3.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.common.types.identer.Fnr;

public record VeilederRequest(
	@JsonProperty(required = true) Fnr fnr
) {
}
