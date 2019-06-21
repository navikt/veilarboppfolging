package no.nav.fo.veilarboppfolging.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Oppfolgingsenhet {
    private String navn;
    private String enhetId;
    private String enhetNr;
}
