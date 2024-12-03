package no.nav.veilarboppfolging.client.veilarbarena;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.nav.common.types.identer.Id;
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.LocalArenaOppfolging;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * Felles struktur for data som kan hentes både fra Arena og veilarbarena
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArenaOppfolgingTilstand {
    String formidlingsgruppe;
    String servicegruppe;
    String oppfolgingsenhet;
    LocalDate inaktiveringsdato;

    public static ArenaOppfolgingTilstand fraArenaOppfolging(VeilarbArenaOppfolgingsStatus veilarbArenaOppfolgingsStatus) {
        return new ArenaOppfolgingTilstand(
                veilarbArenaOppfolgingsStatus.getFormidlingsgruppe(),
                veilarbArenaOppfolgingsStatus.getServicegruppe(),
                veilarbArenaOppfolgingsStatus.getOppfolgingsenhet(),
                veilarbArenaOppfolgingsStatus.getInaktiveringsdato()
        );
    }

    public static ArenaOppfolgingTilstand fraArenaBruker(VeilarbArenaOppfolgingsBruker veilarbArenaOppfolgingsBruker) {
        return new ArenaOppfolgingTilstand(
                veilarbArenaOppfolgingsBruker.getFormidlingsgruppekode(),
                veilarbArenaOppfolgingsBruker.getKvalifiseringsgruppekode(),
                veilarbArenaOppfolgingsBruker.getNav_kontor(),
                Optional.ofNullable(veilarbArenaOppfolgingsBruker.getIserv_fra_dato()).map(ZonedDateTime::toLocalDate).orElse(null)
        );
    }

    public static ArenaOppfolgingTilstand fraLocalArenaOppfolging(LocalArenaOppfolging lokaleData) {
        return new ArenaOppfolgingTilstand(
                lokaleData.getFormidlingsgruppe().name(),
                lokaleData.getKvalifiseringsgruppe().name(),
                Optional.ofNullable(lokaleData.getOppfolgingsenhet()).map(Id::get).orElse(null),
                Optional.ofNullable(lokaleData.getIservFraDato()).orElse(null)
        );
    }

}
