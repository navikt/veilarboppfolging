package no.nav.veilarboppfolging.eventsLogger

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.InsertAllRequest
import com.google.cloud.bigquery.TableId
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Optional
import java.util.UUID
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe
import no.nav.veilarboppfolging.kandidatForUtmelding.hendelser.ForlengelseOpprettetEllerEndretHendelse
import no.nav.veilarboppfolging.kandidatForUtmelding.hendelser.KandidatForUtmeldingHendelseType
import no.nav.veilarboppfolging.oppfolgingsbruker.StartetAvType
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingStartBegrunnelse
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.Avregistrering
import org.slf4j.LoggerFactory

enum class BigQueryEventType {
    OPFOLGINGSPERIODE_START,
    OPPFOLGINGSPERIODE_SLUTT,
}

data class KandidaterForUtmeldingMetrikker(
    val antallKandidaterForUtmelding: Int,
    val antallUnderOppfolgingMedIserv: Int,
    val antallKandidaterForUtmeldingIkkeForlenget: Int,
    val antallKandidaterForUtmeldingForlenget: Int,
    val antallKandidaterSomIkkeKunneUtmeldes: Int,
)

interface BigQueryClient {
    fun loggStartOppfolgingsperiode(startBegrunnelse: OppfolgingStartBegrunnelse, oppfolgingPeriodeId: UUID, startedAvType: StartetAvType, kvalifiseringsgruppe: Optional<Kvalifiseringsgruppe>, manuellSjekkLovligOpphold: Boolean? = null, forrigePeriodeAvsluttet: ZonedDateTime?)
    fun loggAvsluttOppfolgingsperiode(oppfolgingPeriodeId: UUID, avregistrering: Avregistrering, aktivIArena: Boolean? = null, kandidatForUtmeldingHendelseType: KandidatForUtmeldingHendelseType?)
    fun loggKandidaterForUtmeldingMetrikker(metrikker: KandidaterForUtmeldingMetrikker)
    fun loggUnder18()
    fun loggForlengelseHendelse(hendelse: ForlengelseOpprettetEllerEndretHendelse, forrigeHendelseType: KandidatForUtmeldingHendelseType)
}

class BigQueryClientImplementation(private val bigQuery: BigQuery): BigQueryClient {
    val OPPFOLGING_EVENTS = "OPPFOLGINGSPERIODE_EVENTS"
    val KANDIDATER_FOR_UTMELDING_METRIKKER = "KANDIDATER_FOR_UTMELDING_METRIKKER"
    val UNDER18_EVENTS = "UNDER18_EVENTS"
    val DATASET_NAME = "oppfolging_metrikker"
    val kandidatForlengetHendelserTabellNavn = "KANDIDAT_FORLENGET_HENDELSER"
    val oppfolgingsperiodeEventsTable = TableId.of(DATASET_NAME, OPPFOLGING_EVENTS)
    val kandidaterForUtmeldingMetrikkerTable = TableId.of(DATASET_NAME, KANDIDATER_FOR_UTMELDING_METRIKKER)
    val under18EventsTable = TableId.of(DATASET_NAME, UNDER18_EVENTS)
    val forlengelseMetrikkerTable = TableId.of(DATASET_NAME, kandidatForlengetHendelserTabellNavn)

    private fun TableId.insertRequest(row: Map<String, Any?>): InsertAllRequest {
        return InsertAllRequest.newBuilder(this).addRow(row).build()
    }

    val log = LoggerFactory.getLogger(this.javaClass)

    override fun loggForlengelseHendelse(hendelse: ForlengelseOpprettetEllerEndretHendelse, forrigeHendelseType: KandidatForUtmeldingHendelseType) {
        insertIntoOppfolgingEvents(forlengelseMetrikkerTable) {
            mapOf(
                "hendelse" to hendelse.type.toString(),
                "forlenget_til" to hendelse.forlengetTil.toString(),
                "oppfolgingsperiode_id" to hendelse.oppfolgingsperiodeUuid.toString(),
                "hendelse_opprettet" to ZonedDateTime.ofInstant(hendelse.hendelseTidspunkt, ZoneId.of("Europe/Oslo")).toOffsetDateTime().toString(),
                "timestamp" to ZonedDateTime.now().toOffsetDateTime().toString(),
                "forrige_hendelse" to forrigeHendelseType.toString()
            )
        }
    }

    override fun loggAvsluttOppfolgingsperiode(oppfolgingPeriodeId: UUID, avregistrering: Avregistrering, aktivIArena: Boolean?, kandidatForUtmeldingHendelseType: KandidatForUtmeldingHendelseType?) {
        val erAutomatiskAvsluttet = !avregistrering.getAvregistreringsType().erManuellAvregistrering()
        insertIntoOppfolgingEvents(oppfolgingsperiodeEventsTable) {
            mapOf(
                "id" to oppfolgingPeriodeId.toString(),
                "automatiskAvsluttet" to erAutomatiskAvsluttet,
                "timestamp" to ZonedDateTime.now().toOffsetDateTime().toString(),
                "event" to BigQueryEventType.OPPFOLGINGSPERIODE_SLUTT.name,
                "avregistreringsType" to avregistrering.getAvregistreringsType().name,
                "erAktivIArena" to aktivIArena,
                "erKandidatForUtmelding" to (kandidatForUtmeldingHendelseType != null),
                "kandidatForUtmeldingHendelseType" to kandidatForUtmeldingHendelseType?.toString()
            )
        }
    }

    override fun loggStartOppfolgingsperiode(
            startBegrunnelse: OppfolgingStartBegrunnelse,
            oppfolgingPeriodeId: UUID,
            startedAvType: StartetAvType,
            kvalifiseringsgruppe: Optional<Kvalifiseringsgruppe>,
            manuellSjekkLovligOpphold: Boolean?,
            forrigePeriodeAvsluttet: ZonedDateTime?
        ) {
        insertIntoOppfolgingEvents(oppfolgingsperiodeEventsTable) {
            mapOf(
                "id" to oppfolgingPeriodeId.toString(),
                "startBegrunnelse" to startBegrunnelse.name,
                "startedAvType" to startedAvType.name,
                "timestamp" to ZonedDateTime.now().toOffsetDateTime().toString(),
                "event" to BigQueryEventType.OPFOLGINGSPERIODE_START.name,
                "kvalifiseringsgruppe" to kvalifiseringsgruppe.map { it.name }.orElse(null),
                "forrigePeriodeAvsluttet" to forrigePeriodeAvsluttet?.toOffsetDateTime()?.toString(),
            ) + (if (manuellSjekkLovligOpphold != null) mapOf("manuellSjekkLovligOpphold" to manuellSjekkLovligOpphold) else emptyMap())
        }
    }

    override fun loggKandidaterForUtmeldingMetrikker(metrikker: KandidaterForUtmeldingMetrikker) {
        insertIntoOppfolgingEvents(kandidaterForUtmeldingMetrikkerTable) {
            mapOf(
                "antallKandidaterForUtmelding" to metrikker.antallKandidaterForUtmelding,
                "antallUnderOppfolgingMedIserv" to metrikker.antallUnderOppfolgingMedIserv,
                "antallKandidaterForUtmeldingIkkeForlenget" to metrikker.antallKandidaterForUtmeldingIkkeForlenget,
                "antallKandidaterForUtmeldingForlenget" to metrikker.antallKandidaterForUtmeldingForlenget,
                "antallKandidaterSomIkkeKunneUtmeldes" to metrikker.antallKandidaterSomIkkeKunneUtmeldes,
                "timestamp" to ZonedDateTime.now().toOffsetDateTime().toString()
            )
        }
    }

    override fun loggUnder18() {
        insertIntoOppfolgingEvents(under18EventsTable) {
            mapOf(
                "timestamp" to ZonedDateTime.now().toOffsetDateTime().toString()
            )
        }
    }

    private fun insertIntoOppfolgingEvents(table: TableId, getRow: () -> Map<String, Any?>?) {
        runCatching {
            val row = getRow()
            if (row == null) return
            val insertRequest = table.insertRequest(row)
            insertWhileToleratingErrors(insertRequest)
        }
            .onFailure { log.warn("Kunne ikke lage start event i bigquery", it) }
    }

    private fun insertWhileToleratingErrors(insertRequest: InsertAllRequest) {
        val response = bigQuery.insertAll(insertRequest)
        val errors = response.insertErrors
        if (errors.isNotEmpty()) {
            log.error("Error inserting bigquery rows: $errors")
        }
    }
}
