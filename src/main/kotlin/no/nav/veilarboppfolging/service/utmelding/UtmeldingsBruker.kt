package no.nav.veilarboppfolging.service.utmelding

import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2
import java.time.LocalDate

public class UtmeldingsBruker(
    val iservFraDato: LocalDate?,
    val fnr: String,
    val trigger: IservTrigger
)
