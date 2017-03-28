package no.nav.fo.veilarbsituasjon.domain;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.fo.veilarbsituasjon.domain.Oppfolgingsenhet;

@Data
@Accessors(chain = true)
public class Oppfolgingsstatus {
    private String rettighetsgruppe;
    private String formidlingsgruppe;
    private String servicegruppe;
    private Oppfolgingsenhet oppfolgingsenhet;
}
