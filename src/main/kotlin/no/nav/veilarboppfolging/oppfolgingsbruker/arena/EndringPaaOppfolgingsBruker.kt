package no.nav.veilarboppfolging.oppfolgingsbruker.arena

import no.nav.common.types.identer.AktorId
import no.nav.pto_schema.enums.arena.*
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2
import java.time.LocalDate
import java.time.ZonedDateTime

/*
* EndringPaaOppfolgingsBrukerV2 men med aktorId
* */
data class EndringPaaOppfolgingsBruker(
    val aktorId: AktorId,
    val fodselsnummer: String,
    val formidlingsgruppe: Formidlingsgruppe,
    val sistEndretDato: ZonedDateTime,
    val oppfolgingsenhet: String? = null,
    val iservFraDato: LocalDate? = null,
    val kvalifiseringsgruppe: Kvalifiseringsgruppe = Kvalifiseringsgruppe.IVURD,
    val rettighetsgruppe: Rettighetsgruppe? = null,
    val hovedmaal: Hovedmaal? = null,
) {
    companion object {
        fun from(endringPaaOppfolgingsBruker: EndringPaaOppfoelgingsBrukerV2, aktorId: AktorId): EndringPaaOppfolgingsBruker {
            return EndringPaaOppfolgingsBruker(
                aktorId,
                endringPaaOppfolgingsBruker.fodselsnummer,
                endringPaaOppfolgingsBruker.formidlingsgruppe,
                endringPaaOppfolgingsBruker.sistEndretDato,
                endringPaaOppfolgingsBruker.oppfolgingsenhet,
                endringPaaOppfolgingsBruker.iservFraDato,
                endringPaaOppfolgingsBruker.kvalifiseringsgruppe,
                endringPaaOppfolgingsBruker.rettighetsgruppe,
                endringPaaOppfolgingsBruker.hovedmaal,

            )
        }
    }
}
