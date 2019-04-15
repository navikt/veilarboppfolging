package no.nav.fo.veilarboppfolging.domain;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;

@Value
@Builder
@Wither
public class Oppfolgingsbruker {
    private String aktoerId;
    private Innsatsgruppe innsatsgruppe;
    private SykmeldtBrukerType sykmeldtBrukerType;
    private Boolean erManuell;
}
