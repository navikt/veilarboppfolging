package no.nav.veilarboppfolging.client.veilarbarena;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.annotation.Nullable;
import java.time.LocalDate;

/**
 * Har ikke feltene "hovedmaal" og "kvalifiseringsgruppe" (men servicegruppe er egentlig kvalifiseringsgruppe) som {@link no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolgingsBruker} har
 * @see no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolgingsBruker
 */
@Data
@Accessors(chain = true)
public class VeilarbArenaOppfolgingsStatus {
    String rettighetsgruppe;
    String formidlingsgruppe;
    String servicegruppe;
    String oppfolgingsenhet;
    LocalDate inaktiveringsdato;
    @Nullable
    Boolean kanEnkeltReaktiveres;
}
