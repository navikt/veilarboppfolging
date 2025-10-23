package no.nav.veilarboppfolging.kafka

import java.util.*

class OppfolgingskontorConsumerService {
}

data class OppfolgingskontorMelding(
    val kontorId: String,
    val kontorNavn: String,
    val oppfolgingsperiodeId: UUID,
    val aktorId: String,
    val ident: String
)