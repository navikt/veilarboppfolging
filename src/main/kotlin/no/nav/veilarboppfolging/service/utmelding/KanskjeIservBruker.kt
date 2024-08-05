package no.nav.veilarboppfolging.service.utmelding

import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2
import java.time.LocalDate

class KanskjeIservBruker(
    val iservFraDato: LocalDate,
    val fnr: String,
    val formidlingsgruppe: Formidlingsgruppe
) {
    fun utmeldingsBruker(): UtmeldingsBruker {
        return UtmeldingsBruker(iservFraDato, fnr)
    }

    companion object {
        fun of(bruker: EndringPaaOppfoelgingsBrukerV2): KanskjeIservBruker {
            return KanskjeIservBruker(bruker.iservFraDato, bruker.fodselsnummer, bruker.formidlingsgruppe)
        }
    }
}
