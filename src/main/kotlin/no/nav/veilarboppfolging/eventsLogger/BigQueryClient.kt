package no.nav.veilarboppfolging.eventsLogger

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.InsertAllRequest
import com.google.cloud.bigquery.TableId
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe
import no.nav.veilarboppfolging.oppfolgingsbruker.StartetAvType
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingStartBegrunnelse
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.ArbeidsøkerRegSync_AlleredeUteAvOppfolging
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.ArbeidsøkerRegSync_BleIserv
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.ArbeidsøkerRegSync_IkkeLengerIserv
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.ArbeidsøkerRegSync_NoOp
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.ArbeidsøkerRegSync_OppdaterIservDato
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.Avregistrering
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.OppdateringFraArena_AlleredeUteAvOppfolging
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.OppdateringFraArena_BleIserv
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.OppdateringFraArena_IkkeLengerIserv
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.OppdateringFraArena_NoOp
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.OppdateringFraArena_OppdaterIservDato
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.ScheduledJob_AlleredeUteAvOppfolging
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.ScheduledJob_UtAvOppfolgingPga28DagerIserv
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.UtmeldingsHendelse
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime
import java.util.*

enum class BigQueryEventType {
    OPFOLGINGSPERIODE_START,
    OPPFOLGINGSPERIODE_SLUTT,
}

data class KandidaterForUtmeldingMetrikker(
    val antallKandidaterForUtmelding: Int,
    val antallUnderOppfolgingMedIserv: Int,
    val antallKandidaterForUtmeldingForlenget: Int,
)

interface BigQueryClient {
    fun loggStartOppfolgingsperiode(startBegrunnelse: OppfolgingStartBegrunnelse, oppfolgingPeriodeId: UUID, startedAvType: StartetAvType, kvalifiseringsgruppe: Optional<Kvalifiseringsgruppe>, manuellSjekkLovligOpphold: Boolean? = null, forrigePeriodeAvsluttet: ZonedDateTime?)
    fun loggAvsluttOppfolgingsperiode(oppfolgingPeriodeId: UUID, avregistrering: Avregistrering, aktivIArena: Boolean? = null)
    fun loggUtmeldingsHendelse(utmelding: UtmeldingsHendelse)
    fun loggKandidaterForUtmeldingMetrikker(metrikker: KandidaterForUtmeldingMetrikker)
    fun loggUnder18()
}

class BigQueryClientImplementation(private val bigQuery: BigQuery): BigQueryClient {
    val OPPFOLGING_EVENTS = "OPPFOLGINGSPERIODE_EVENTS"
    val UTMELDING_EVENTS = "UTMELDING_EVENTS"
    val KANDIDATER_FOR_UTMELDING_METRIKKER = "KANDIDATER_FOR_UTMELDING_METRIKKER"
    val UNDER18_EVENTS = "UNDER18_EVENTS"
    val DATASET_NAME = "oppfolging_metrikker"
    val oppfolgingsperiodeEventsTable = TableId.of(DATASET_NAME, OPPFOLGING_EVENTS)
    val utmeldingEventsTable = TableId.of(DATASET_NAME, UTMELDING_EVENTS)
    val kandidaterForUtmeldingMetrikkerTable = TableId.of(DATASET_NAME, KANDIDATER_FOR_UTMELDING_METRIKKER)
    val under18EventsTable = TableId.of(DATASET_NAME, UNDER18_EVENTS)

    private fun TableId.insertRequest(row: Map<String, Any?>): InsertAllRequest {
        return InsertAllRequest.newBuilder(this).addRow(row).build()
    }

    val log = LoggerFactory.getLogger(this.javaClass)

    override fun loggAvsluttOppfolgingsperiode(oppfolgingPeriodeId: UUID, avregistrering: Avregistrering, aktivIArena: Boolean?) {
        val erAutomatiskAvsluttet = !avregistrering.getAvregistreringsType().erManuellAvregistrering()
        insertIntoOppfolgingEvents(oppfolgingsperiodeEventsTable) {
            mapOf(
                "id" to oppfolgingPeriodeId.toString(),
                "automatiskAvsluttet" to erAutomatiskAvsluttet,
                "timestamp" to ZonedDateTime.now().toOffsetDateTime().toString(),
                "event" to BigQueryEventType.OPPFOLGINGSPERIODE_SLUTT.name,
                "avregistreringsType" to avregistrering.getAvregistreringsType().name,
                "erAktivIArena" to aktivIArena
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

    override fun loggUtmeldingsHendelse(utmelding: UtmeldingsHendelse) {
        insertIntoOppfolgingEvents(utmeldingEventsTable) {
            val eventType = when (utmelding) {
                // Starter grace periode
                is OppdateringFraArena_BleIserv -> mapOf("event" to "start_graceperiode", "trigger" to "EndringPaaOppfolgingsbruker")
                is ArbeidsøkerRegSync_BleIserv -> mapOf("event" to "start_graceperiode", "trigger" to "ArbeidsøkerRegSync")

                is OppdateringFraArena_IkkeLengerIserv -> mapOf("event" to "avbryt_graceperiode", "trigger" to "EndringPaaOppfolgingsbruker")
                is ArbeidsøkerRegSync_IkkeLengerIserv -> mapOf("event" to "avbryt_graceperiode", "trigger" to "ArbeidsøkerRegSync")

                // Disse er opprydding av tabell, bruker var allerede ute av oppfølging
                is OppdateringFraArena_AlleredeUteAvOppfolging -> mapOf("event" to "slett_fra_utmelding_allerede_ute", "trigger" to "EndringPaaOppfolgingsbruker")
                is ArbeidsøkerRegSync_AlleredeUteAvOppfolging -> mapOf("event" to "slett_fra_utmelding_allerede_ute", "trigger" to "ArbeidsøkerRegSync")
                is ScheduledJob_AlleredeUteAvOppfolging -> mapOf("event" to "slett_fra_utmelding_allerede_ute", "trigger" to "ScheduledJob")

                is ScheduledJob_UtAvOppfolgingPga28DagerIserv -> mapOf("event" to "avregistrert", "trigger" to "ScheduledJob")

                is OppdateringFraArena_OppdaterIservDato -> return@insertIntoOppfolgingEvents null
                is ArbeidsøkerRegSync_OppdaterIservDato -> return@insertIntoOppfolgingEvents null
                is ArbeidsøkerRegSync_NoOp -> return@insertIntoOppfolgingEvents null
                is OppdateringFraArena_NoOp -> return@insertIntoOppfolgingEvents null
            }
            eventType + mapOf(
                "timestamp" to ZonedDateTime.now().toOffsetDateTime().toString()
            )
        }
    }

    override fun loggKandidaterForUtmeldingMetrikker(metrikker: KandidaterForUtmeldingMetrikker) {
        insertIntoOppfolgingEvents(kandidaterForUtmeldingMetrikkerTable) {
            mapOf(
                "antallKandidaterForUtmelding" to metrikker.antallKandidaterForUtmelding,
                "antallUnderOppfolgingMedIserv" to metrikker.antallUnderOppfolgingMedIserv,
                "antallKandidaterForUtmeldingForlenget" to metrikker.antallKandidaterForUtmeldingForlenget,
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
