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
    public String rettighetsgruppe;
    public String formidlingsgruppe;
    public String servicegruppe;
    public String oppfolgingsenhet;
    public LocalDate inaktiveringsdato;
    public Boolean kanEnkeltReaktiveres;
}
