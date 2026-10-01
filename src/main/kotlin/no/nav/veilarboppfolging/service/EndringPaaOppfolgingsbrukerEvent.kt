package no.nav.veilarboppfolging.service


import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.veilarboppfolging.repository.entity.OppfolgingEntity

fun resolveEndringPaaOppfolgingsbrukerEvent(
    formidlingsgruppe: Formidlingsgruppe,
    nåværendeOppfolgingsstatus: OppfolgingEntity?,
): OppfolgingsbrukerEndretEvent {
    val erInaktivIArena = Formidlingsgruppe.ISERV == formidlingsgruppe
    val varInaktivIArena = nåværendeOppfolgingsstatus?.localArenaOppfolging?.orElse(null)?.formidlingsgruppe == Formidlingsgruppe.ISERV
    val erUnderOppfolging = nåværendeOppfolgingsstatus?.underOppfolging ?: false

    if (!erUnderOppfolging) return IrrelevantEndring()
    if (!erInaktivIArena) return IrrelevantEndring()
    if (erInaktivIArena && varInaktivIArena) return IrrelevantEndring()

    val varArbsIArena = nåværendeOppfolgingsstatus?.localArenaOppfolging?.orElse(null)?.formidlingsgruppe == Formidlingsgruppe.ARBS

    return if (erInaktivIArena && varArbsIArena)
        BleInaktivertVarArbs()
    else
        BleInaktivertVarIarbs()
}

sealed interface OppfolgingsbrukerEndretEvent {
    fun loggMessage(): String
}

class BleInaktivertVarIarbs : OppfolgingsbrukerEndretEvent {
    override fun loggMessage(): String = "Bruker ble inaktivert, kunne ikke reaktiveres"
}

class IrrelevantEndring : OppfolgingsbrukerEndretEvent {
    override fun loggMessage(): String = "Irrelevant endring – gjør ingenting"
}

class BleInaktivertVarArbs : OppfolgingsbrukerEndretEvent {
    override fun loggMessage(): String = "Bruker var ARBS og ble ISERV, ignoreres"
}
