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
    kanAvsluttes: (kanReaktiveres: Boolean) -> OppfolgingService.KanAvslutteMedBegrunnelse
): OppfolgingsbrukerEndretEvent {
    val erSykmeldtUtenArbeidsgiver =  sykmeldtUtenArbeidsgiver(endringPaaOppfolgingsBruker.kvalifiseringsgruppe, endringPaaOppfolgingsBruker.formidlingsgruppe)
    val varSykmeldtUtenArbeidsgiver = nåværendeOppfolgingsstatus?.localArenaOppfolging?.orElse(null)?.let { sykmeldtUtenArbeidsgiver(it.kvalifiseringsgruppe, it.formidlingsgruppe) } ?: false
    if (erSykmeldtUtenArbeidsgiver && !varSykmeldtUtenArbeidsgiver) return BleSykmeldtUtenArbeidsgiver()

    val erInaktivIArena = Formidlingsgruppe.ISERV == endringPaaOppfolgingsBruker.formidlingsgruppe
    val erUnderOppfolging = nåværendeOppfolgingsstatus?.isUnderOppfolging ?: false

    if (erInaktivIArena && erUnderOppfolging) {
        val kanReaktiveres = getKanReaktiveresIArena()
        if (kanReaktiveres.isEmpty) return IrrelevantEndring()
        val kanAvsluttesMedBegrunnelse = kanAvsluttes(kanReaktiveres.get())
        if (!kanAvsluttesMedBegrunnelse.kanAvslutte) return KanIkkeAvsluttes(kanAvsluttesMedBegrunnelse.begrunnelse)
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

sealed interface OppfolgingsbrukerEndretEvent {
    fun loggMessage(): String
}

class BleSykmeldtUtenArbeidsgiver : OppfolgingsbrukerEndretEvent {
    override fun loggMessage(): String = "BleSykmeldtUtenArbeidsgiver"
}

class BleInaktivertUtenKanReaktiveres : OppfolgingsbrukerEndretEvent {
    override fun loggMessage(): String = "Bruker ble inaktivert, kunne ikke reaktiveres"
}

class BleInaktivertMedKanReaktiveres : OppfolgingsbrukerEndretEvent {
    override fun loggMessage(): String = "Bruker ble inaktivert, kan reaktiveres"
}

class KanIkkeAvsluttes(val begrunnelse: String) : OppfolgingsbrukerEndretEvent {
    override fun loggMessage(): String = "Bruker kunne ikke avsluttes, første hindring er: $begrunnelse"
}

class IrrelevantEndring : OppfolgingsbrukerEndretEvent {
    override fun loggMessage(): String = "Irrelevant endring – gjør ingenting"
}
