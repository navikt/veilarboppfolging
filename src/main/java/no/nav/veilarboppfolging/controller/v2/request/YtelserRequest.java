package no.nav.veilarboppfolging.controller.v2.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.common.types.identer.Fnr;

public record YtelserRequest(
	@JsonProperty(required = true) Fnr fnr
) {
}
