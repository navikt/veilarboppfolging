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
    private String fodselsnr;
    private String formidlingsgruppekode;
    private String kvalifiseringsgruppekode;
    private String rettighetsgruppekode;
    private ZonedDateTime iserv_fra_dato;
    private String hovedmaalkode;
    private String nav_kontor;
}
