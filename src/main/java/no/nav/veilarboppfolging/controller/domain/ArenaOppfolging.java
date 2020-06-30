package no.nav.veilarboppfolging.controller.domain;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.veilarboppfolging.domain.Oppfolgingsenhet;

import java.time.LocalDate;

@Data
@Accessors(chain = true)
public class ArenaOppfolging {
    private String rettighetsgruppe;
    private String formidlingsgruppe;
    private String servicegruppe;
    private Oppfolgingsenhet oppfolgingsenhet;
    private LocalDate inaktiveringsdato;
}
