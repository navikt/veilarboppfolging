package no.nav.veilarboppfolging.client.oppgave

import java.time.DayOfWeek
import java.time.LocalDate

const val TEMA_OPPFOLGING: String = "OPP"
const val OPPGAVETYPE_KONTAKT_BRUKER: String = "KONT_BRUK"

const val BESKRIVELSE = """
Personen har forsøkt å be om arbeidsrettet oppfølging, men er sperret fra å gjøre dette da personen er under 18 år.
For mindreårige trengs det samtykke fra begge foresatte for å kunne motta arbeidsrettet oppfølging.
Se "Samtykke fra foresatte til unge under 18 år - registrering som arbeidssøker, øvrige tiltak og tjenester".

Når samtykke er innhentet kan du starte arbeidsrettet oppfølging i Modia arbeidsrettet oppfølging.
"""

data class OpprettOppgaveRequest(
    val personident: String,
    val opprettetAvEnhetsnr: String = "9999",
    val beskrivelse: String? = BESKRIVELSE,
    val tema: String = TEMA_OPPFOLGING,
    val oppgavetype: String = OPPGAVETYPE_KONTAKT_BRUKER,
    val aktivDato: LocalDate = LocalDate.now(),
    val fristFerdigstillelse: LocalDate = finnFristForFerdigstillingAvOppgave(LocalDate.now().plusDays(2)),
    val prioritet: PrioritetType = PrioritetType.NORM,
)

fun finnFristForFerdigstillingAvOppgave(ferdigstillDato: LocalDate): LocalDate {
    return finnNesteArbeidsdag(ferdigstillDato)
}

fun finnNesteArbeidsdag(ferdigstillDato: LocalDate): LocalDate =
    when (ferdigstillDato.dayOfWeek) {
        DayOfWeek.SATURDAY -> ferdigstillDato.plusDays(2)
        DayOfWeek.SUNDAY -> ferdigstillDato.plusDays(1)
        else -> ferdigstillDato
    }

enum class PrioritetType {
    HOY,
    NORM,
    LAV,
}

data class OppgaveResponse(
    val id: Int,
    val fristFerdigstillelse: LocalDate?,
)

data class FinnOppgaveResponse(
    val antallTreffTotalt: Int,
    val oppgaver: List<OppgaveResponse>,
)
