package no.nav.veilarboppfolging.controller.v3.request;

import no.nav.common.types.identer.Fnr;

import java.time.ZonedDateTime;

public record MaalRequest(
		Fnr fnr,
		String mal,
		String endretAv,
		ZonedDateTime dato
) {
}
