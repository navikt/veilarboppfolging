package no.nav.veilarboppfolging.controller.v3.request;

import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.request.SykmeldtBrukerType;

public record SykmeldtBrukerRequest(
	Fnr fnr,
	SykmeldtBrukerType sykmeldtBrukerType
) {
}
