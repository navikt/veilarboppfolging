package no.nav.veilarboppfolging.kafka

import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.pto_schema.enums.arena.Hovedmaal
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe
import no.nav.pto_schema.enums.arena.Rettighetsgruppe
import no.nav.pto_schema.enums.arena.SikkerhetstiltakType
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2
import java.time.LocalDate
import java.time.ZonedDateTime

object TestUtils {

    fun oppfølgingsBrukerEndret(
        fnr: String,
        iservFraDato: LocalDate = LocalDate.now(),
        formidlingsgruppe: Formidlingsgruppe = Formidlingsgruppe.ARBS,
        enhetId: String = "8686",
        hovedmaal: Hovedmaal? = Hovedmaal.BEHOLDEA,
        kvalifiseringsgruppe: Kvalifiseringsgruppe? = Kvalifiseringsgruppe.IVURD
    ): EndringPaaOppfoelgingsBrukerV2 {
        return EndringPaaOppfoelgingsBrukerV2(
            fnr,
            formidlingsgruppe,
            iservFraDato,
            "Sig",
            ":)",
            enhetId,
            kvalifiseringsgruppe,
            Rettighetsgruppe.INDS,
            hovedmaal,
            SikkerhetstiltakType.TFUS,
            null,
            true,
            false,
            false,
            null,
            ZonedDateTime.now()
        )
    }
}
