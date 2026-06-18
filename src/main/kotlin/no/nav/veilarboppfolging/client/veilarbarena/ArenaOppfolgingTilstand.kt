package no.nav.veilarboppfolging.client.veilarbarena

import no.nav.veilarboppfolging.oppfolgingsbruker.arena.LocalArenaOppfolging
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*
import java.util.function.Function


/**
 * Felles struktur for data som kan hentes både fra Arena og veilarbarena
 */
class ArenaOppfolgingTilstand(
    val formidlingsgruppe: String?,
    val servicegruppe: String?,
    var inaktiveringsdato: LocalDate?
) {
    companion object {
        fun fraArenaBruker(veilarbArenaOppfolgingsBruker: VeilarbArenaOppfolgingsBruker): ArenaOppfolgingTilstand {
            return ArenaOppfolgingTilstand(
                veilarbArenaOppfolgingsBruker.formidlingsgruppekode,
                veilarbArenaOppfolgingsBruker.kvalifiseringsgruppekode,
                Optional.ofNullable<ZonedDateTime?>(veilarbArenaOppfolgingsBruker.iservFraDato).map(
                    Function { obj: ZonedDateTime? -> obj!!.toLocalDate() }).orElse(null)
            )
        }

        fun fraLocalArenaOppfolging(lokaleData: LocalArenaOppfolging): ArenaOppfolgingTilstand {
            return ArenaOppfolgingTilstand(
                lokaleData.formidlingsgruppe.name,
                lokaleData.kvalifiseringsgruppe.name,
                Optional.ofNullable<LocalDate?>(lokaleData.iservFraDato).orElse(null)
            )
        }
    }
}
