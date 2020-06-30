package no.nav.veilarboppfolging.controller.domain;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.veilarboppfolging.domain.Oppfolgingsenhet;

@Data
@Accessors(chain = true)
public class OppfolgingEnhetMedVeileder {
    private Oppfolgingsenhet oppfolgingsenhet;
    private String veilederId;
    private String formidlingsgruppe;
    private String servicegruppe;
    private String hovedmaalkode;
}

