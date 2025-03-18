package no.nav.veilarboppfolging.service.utmelding

import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.UtmeldingsHendelse
import java.time.LocalDate

enum class IservTrigger {
    OppdateringPaaOppfolgingsBruker,
    ArbeidssøkerRegistreringSync,
}

class KanskjeIservBruker(
    val iservFraDato: LocalDate?,
    val fnr: String,
    val formidlingsgruppe: Formidlingsgruppe,
    val trigger: IservTrigger
) {
    fun utmeldingsBruker(): UtmeldingsBruker {
        return UtmeldingsBruker(iservFraDato, fnr, trigger)
    }

    fun erIserv() = formidlingsgruppe == Formidlingsgruppe.ISERV

    companion object {
        fun of(bruker: EndringPaaOppfoelgingsBrukerV2): KanskjeIservBruker {
            return KanskjeIservBruker(bruker.iservFraDato, bruker.fodselsnummer, bruker.formidlingsgruppe, IservTrigger.OppdateringPaaOppfolgingsBruker)
        }
    }
}
