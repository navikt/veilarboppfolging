package no.nav.veilarboppfolging.domain;

import lombok.Builder;
import lombok.Value;
import no.nav.veilarboppfolging.controller.request.SykmeldtBrukerType;

@Value
@Builder
public class Oppfolgingsbruker {
    String aktoerId;
    Innsatsgruppe innsatsgruppe;
    SykmeldtBrukerType sykmeldtBrukerType;
}
