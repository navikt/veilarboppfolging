package no.nav.veilarboppfolging.domain;

import lombok.Value;
import no.nav.veilarboppfolging.client.veilarbarena.ArenaOppfolging;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolging;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * Felles struktur for data som kan hentes både fra Arena og veilarbarena
 */
@Value
public class ArenaOppfolgingTilstand {
    String formidlingsgruppe;
    String servicegruppe;
    String rettighetsgruppe;
    String oppfolgingsenhet;
    LocalDate inaktiveringsdato;
    boolean direkteFraArena;
    Boolean kanEnkeltReaktiveres;

    public static ArenaOppfolgingTilstand fraArenaOppfolging(ArenaOppfolging arenaOppfolging) {
        return new ArenaOppfolgingTilstand(
                arenaOppfolging.getFormidlingsgruppe(),
                arenaOppfolging.getServicegruppe(),
                arenaOppfolging.getRettighetsgruppe(),
                arenaOppfolging.getOppfolgingsenhet(),
                arenaOppfolging.getInaktiveringsdato(),
                true,
                arenaOppfolging.getKanEnkeltReaktiveres());
    }

    public static ArenaOppfolgingTilstand fraArenaBruker(VeilarbArenaOppfolging veilarbArenaOppfolging) {
        return new ArenaOppfolgingTilstand(
                veilarbArenaOppfolging.getFormidlingsgruppekode(),
                veilarbArenaOppfolging.getKvalifiseringsgruppekode(),
                veilarbArenaOppfolging.getRettighetsgruppekode(),
                veilarbArenaOppfolging.getNav_kontor(),
                Optional.ofNullable(veilarbArenaOppfolging.getIserv_fra_dato()).map(ZonedDateTime::toLocalDate).orElse(null),
                false,
                null
        );
    }

}
