package no.nav.fo.veilarbsituasjon.rest.domain;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Oppfolgingsstatus {
    private String rettighetsgruppe;
    private String formidlingsgruppe;
    private String servicegruppe;
    private Organisasjonsenhet organisasjonsenhet;
}
