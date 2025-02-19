package no.nav.veilarboppfolging.client.veilarbarena;

import lombok.*;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;

/**
 * Har IKKE feltet kanReaktiveres eller iservFraDato som oppfølgingsStatus {@link no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolgingsStatus} har, men har hovedmaalkode
 * @see no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolgingsStatus
 */
@Data
@Accessors(chain = true)
public class VeilarbArenaOppfolgingsBruker {
    String fodselsnr;
    String formidlingsgruppekode;
    String kvalifiseringsgruppekode;
    String rettighetsgruppekode;
    ZonedDateTime iserv_fra_dato;
    String hovedmaalkode;
    String nav_kontor;
}
