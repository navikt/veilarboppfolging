package no.nav.veilarboppfolging.controller.v3.request;

import no.nav.common.types.identer.Fnr;

public record MaalForPersonRequest(
	Fnr fnr
) {
}