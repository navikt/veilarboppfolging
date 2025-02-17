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
    public String fodselsnr;
    public String formidlingsgruppekode;
    public String kvalifiseringsgruppekode;
    public String rettighetsgruppekode;
    public ZonedDateTime iserv_fra_dato;
    public String hovedmaalkode;
    public String nav_kontor;
}
