package no.nav.fo.veilarboppfolging.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;

@Data
@Accessors(chain = true)
public class ArenaOppfolging {
    private String rettighetsgruppe;
    private String formidlingsgruppe;
    private String servicegruppe;
    private String oppfolgingsenhet;
    private LocalDate inaktiveringsdato;
    private Boolean harMottaOppgaveIArena;
    private Boolean kanEnkeltReaktiveres;
}
