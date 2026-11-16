package no.nav.veilarboppfolging.sykmeldingstatus

import java.time.LocalDate
import java.util.UUID

data class SisteSykmeldingstatus(
    val oppfolgingsperiodeId: UUID,
    val fnr: String,
    val startdato: LocalDate,
    val sluttdato: LocalDate,
    val harArbeidsgiver: Boolean
) {
    fun erSykmeldtUtenArbeidsgiver(iDag: LocalDate = LocalDate.now()): Boolean {
        return !harArbeidsgiver && !iDag.isAfter(sluttdato) && !iDag.isBefore(startdato)
    }
}
