package no.nav.veilarboppfolging.kafka

import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.pto_schema.enums.arena.Hovedmaal
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe
import no.nav.pto_schema.enums.arena.Rettighetsgruppe
import no.nav.pto_schema.enums.arena.SikkerhetstiltakType
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.LocalArenaOppfolging
import no.nav.veilarboppfolging.repository.entity.KvpPeriodeEntity
import no.nav.veilarboppfolging.repository.entity.OppfolgingEntity
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.Optional
import java.util.UUID

object TestUtils {

    fun oppfølgingsBrukerEndret(
        fnr: String,
        iservFraDato: LocalDate? = null,
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

    fun oppfølgingEntity(
        aktorId: String,
        veilederId: String = "A111111",
        underOppfolging: Boolean = true,
        localArenaOppfølging: LocalArenaOppfolging = localArenaOppfolging(),
    ) = OppfolgingEntity(
            aktorId = aktorId,
            veilederId = veilederId,
            underOppfolging = underOppfolging,
            gjeldendeManuellStatusId = 1L,
            gjeldendeMaalId = 1L,
            gjeldendeKvpId = 0L,
            localArenaOppfolging = Optional.of(localArenaOppfølging),
            oppfolgingsEnhet = null
        )

    fun oppfølgingPeriodeEntity(
        kvpPerioder: List<KvpPeriodeEntity>? = listOf(),
    ) = OppfolgingsperiodeEntity(
        uuid = UUID.randomUUID(),
        aktorId = "11111111111",
        startDato = ZonedDateTime.now(),
        kvpPerioder = kvpPerioder,
    )

    fun localArenaOppfolging(kvalifiseringsgruppe: Kvalifiseringsgruppe = Kvalifiseringsgruppe.IVURD, formidlingsgruppe: Formidlingsgruppe = Formidlingsgruppe.ARBS) =
        LocalArenaOppfolging(
            hovedmaal = Hovedmaal.BEHOLDEA,
            kvalifiseringsgruppe = kvalifiseringsgruppe,
            formidlingsgruppe = formidlingsgruppe,
            iservFraDato = null
        )
}
