package no.nav.veilarboppfolging.service


import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.EndringPaaOppfolgingsBruker
import no.nav.veilarboppfolging.repository.entity.OppfolgingEntity
import java.time.LocalDate
import java.util.*


fun resolveEndringPaaOppfolgingsbrukerEvent(endringPaaOppfolgingsBruker: EndringPaaOppfolgingsBruker,
                                            nåværendeOppfolgingsstatus: OppfolgingEntity?,
                                            kanReaktiveresIArena: () -> Optional<Boolean>
): OppfolgingsbrukerEndretEvent {
    val erSykmeldtUtenArbeidsgiver =  sykmeldtUtenArbeidsgiver(endringPaaOppfolgingsBruker.kvalifiseringsgruppe, endringPaaOppfolgingsBruker.formidlingsgruppe)
    val varSykmeldtUtenArbeidsgiver = nåværendeOppfolgingsstatus?.localArenaOppfolging?.orElse(null)?.let { sykmeldtUtenArbeidsgiver(it.kvalifiseringsgruppe, it.formidlingsgruppe) } ?: false
    if (erSykmeldtUtenArbeidsgiver && !varSykmeldtUtenArbeidsgiver) return BleSykmeldtUtenArbeidsgiver()

    val erInaktivIArena = Formidlingsgruppe.ISERV == endringPaaOppfolgingsBruker.formidlingsgruppe
    val erUnderOppfolging = nåværendeOppfolgingsstatus?.isUnderOppfolging ?: false

    if (erInaktivIArena && erUnderOppfolging) {
        val kanReaktiveres = kanEnkeltReaktiveresLokalt(nåværendeOppfolgingsstatus, endringPaaOppfolgingsBruker) && kanReaktiveresIArena().get()

    }

}


private fun sykmeldtUtenArbeidsgiver(kvalifiseringsgruppe: Kvalifiseringsgruppe, formidlingsgruppe: Formidlingsgruppe) =
    Kvalifiseringsgruppe.VURDU == kvalifiseringsgruppe &&
            formidlingsgruppe != Formidlingsgruppe.ISERV

private fun kanEnkeltReaktiveresLokalt(currentOppfolgingstatus: OppfolgingEntity?, brukerV2: EndringPaaOppfolgingsBruker): Boolean {
    val erIserv = brukerV2.formidlingsgruppe == Formidlingsgruppe.ISERV
    val harBlittIservILøpetAvDeSiste28Dagene =
        brukerV2.iservFraDato?.isAfter(LocalDate.now().minusDays(28)) ?: false
    val varArbeidssøker = currentOppfolgingstatus?.localArenaOppfolging?.orElse(null)?.formidlingsgruppe == Formidlingsgruppe.ARBS
    val harFått14aVedtak = brukerV2.kvalifiseringsgruppe !in listOf(Kvalifiseringsgruppe.BKART, Kvalifiseringsgruppe.IVURD) // Ikke 100% sikker at bruker ikke har fått 14a, men sjekken er god nok

    return erIserv && harBlittIservILøpetAvDeSiste28Dagene && varArbeidssøker && harFått14aVedtak
}

sealed interface OppfolgingsbrukerEndretEvent

class BleSykmeldtUtenArbeidsgiver : OppfolgingsbrukerEndretEvent
class BleInaktivertUtenKanReaktiveres : OppfolgingsbrukerEndretEvent
class BleInaktivertMedKanReaktiveres : OppfolgingsbrukerEndretEvent
class IrrelevantEndring : OppfolgingsbrukerEndretEvent
