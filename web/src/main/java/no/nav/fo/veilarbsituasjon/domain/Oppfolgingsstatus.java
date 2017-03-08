package no.nav.fo.veilarbsituasjon.domain;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Oppfolgingsstatus {
    private String oppfolginsenhet;
    private String rettighetsgruppe;
    private String formidlingsgruppe;
    private String servicegruppe;
}
