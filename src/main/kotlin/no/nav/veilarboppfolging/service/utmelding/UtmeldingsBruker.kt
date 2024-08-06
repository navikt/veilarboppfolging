package no.nav.veilarboppfolging.service.utmelding

import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2
import java.time.LocalDate

class UtmeldingsBruker(
    val iservFraDato: LocalDate,
    val fnr: String
) {
    companion object {
        fun of(bruker: EndringPaaOppfoelgingsBrukerV2): UtmeldingsBruker {
            return UtmeldingsBruker(bruker.iservFraDato, bruker.fodselsnummer)
        }
    }
}
