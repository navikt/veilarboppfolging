package no.nav.veilarboppfolging.client.veilarbarena;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;

/**
 * Har ikke feltene "hovedmaal" og "kvalifiseringsgruppe" (men servicegruppe er egentlig kvalifiseringsgruppe) som {@link no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolgingsBruker} har
 * @see no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolgingsBruker
 */
@Data
@Accessors(chain = true)
public class VeilarbArenaOppfolgingsStatus {
    private String rettighetsgruppe;
    private String formidlingsgruppe;
    private String servicegruppe;
    private String oppfolgingsenhet;
    private LocalDate inaktiveringsdato;
    private Boolean kanEnkeltReaktiveres;
}
