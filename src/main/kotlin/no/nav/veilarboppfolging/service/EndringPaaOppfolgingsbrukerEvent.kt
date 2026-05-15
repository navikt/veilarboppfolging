package no.nav.veilarboppfolging.service


import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.EndringPaaOppfolgingsBruker
import no.nav.veilarboppfolging.repository.entity.OppfolgingEntity
import java.util.*

fun resolveEndringPaaOppfolgingsbrukerEvent(
    endringPaaOppfolgingsBruker: EndringPaaOppfolgingsBruker,
    nåværendeOppfolgingsstatus: OppfolgingEntity?,
    getKanReaktiveresIArena: () -> Optional<Boolean>,
    kanAvsluttes: (kanReaktiveres: Boolean) -> Boolean
): OppfolgingsbrukerEndretEvent {
    val erSykmeldtUtenArbeidsgiver =  sykmeldtUtenArbeidsgiver(endringPaaOppfolgingsBruker.kvalifiseringsgruppe, endringPaaOppfolgingsBruker.formidlingsgruppe)
    val varSykmeldtUtenArbeidsgiver = nåværendeOppfolgingsstatus?.localArenaOppfolging?.orElse(null)?.let { sykmeldtUtenArbeidsgiver(it.kvalifiseringsgruppe, it.formidlingsgruppe) } ?: false
    if (erSykmeldtUtenArbeidsgiver && !varSykmeldtUtenArbeidsgiver) return BleSykmeldtUtenArbeidsgiver()

    val erInaktivIArena = Formidlingsgruppe.ISERV == endringPaaOppfolgingsBruker.formidlingsgruppe
    val erUnderOppfolging = nåværendeOppfolgingsstatus?.isUnderOppfolging ?: false

    if (erInaktivIArena && erUnderOppfolging) {
        val kanReaktiveres = getKanReaktiveresIArena()
        if (kanReaktiveres.isEmpty) return IrrelevantEndring()
        if (!kanAvsluttes(kanReaktiveres.get())) return KanIkkeAvsluttes()
        return when (kanReaktiveres.get()) {
            true -> BleInaktivertMedKanReaktiveres()
            false -> BleInaktivertUtenKanReaktiveres()
        }
    } else {
        return IrrelevantEndring()
    }
}

private fun sykmeldtUtenArbeidsgiver(kvalifiseringsgruppe: Kvalifiseringsgruppe, formidlingsgruppe: Formidlingsgruppe) =
    Kvalifiseringsgruppe.VURDU == kvalifiseringsgruppe &&
            formidlingsgruppe != Formidlingsgruppe.ISERV

sealed interface OppfolgingsbrukerEndretEvent

class BleSykmeldtUtenArbeidsgiver : OppfolgingsbrukerEndretEvent
class BleInaktivertUtenKanReaktiveres : OppfolgingsbrukerEndretEvent
class BleInaktivertMedKanReaktiveres : OppfolgingsbrukerEndretEvent
class KanIkkeAvsluttes : OppfolgingsbrukerEndretEvent
class IrrelevantEndring : OppfolgingsbrukerEndretEvent
