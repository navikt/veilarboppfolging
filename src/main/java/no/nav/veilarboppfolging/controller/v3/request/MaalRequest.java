package no.nav.veilarboppfolging.controller.v3.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.common.types.identer.Fnr;

import java.time.ZonedDateTime;

public record MaalRequest(
		@JsonProperty Fnr fnr,

		@JsonProperty(required = true) MaalInnhold maalInnhold
) {
}
