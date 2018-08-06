package no.nav.fo.veilarboppfolging.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Oppfolgingsbruker {
    private String aktoerId;
    private Innsatsgruppe innsatsgruppe;
}
