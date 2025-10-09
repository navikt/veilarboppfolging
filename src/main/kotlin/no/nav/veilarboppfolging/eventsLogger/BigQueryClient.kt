package no.nav.veilarboppfolging.eventsLogger

import com.google.cloud.bigquery.BigQueryOptions
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
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.AvregistreringsType
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

data class UtmeldingsAntall(
    val personerIUtemelding: Int,
    val personIUtmeldingSomErUnderOppfolging: Int,
)

interface BigQueryClient {
    fun loggStartOppfolgingsperiode(oppfolging: OppfolgingStartBegrunnelse, oppfolgingPeriodeId: UUID, startedAvType: StartetAvType, kvalifiseringsgruppe: Optional<Kvalifiseringsgruppe>)
    fun loggAvsluttOppfolgingsperiode(oppfolgingPeriodeId: UUID, avregistreringsType: AvregistreringsType)
    fun loggUtmeldingsHendelse(utmelding: UtmeldingsHendelse)
    fun loggUtmeldingsCount(utmelding: UtmeldingsAntall)
}

class BigQueryClientImplementation(projectId: String): BigQueryClient {
    val OPPFOLGING_EVENTS = "OPPFOLGINGSPERIODE_EVENTS"
    val UTMELDING_EVENTS = "UTMELDING_EVENTS"
    val UTMELDING_COUNTS = "UTMELDING_COUNTS"
    val DATASET_NAME = "oppfolging_metrikker"
    val oppfolgingsperiodeEventsTable = TableId.of(DATASET_NAME, OPPFOLGING_EVENTS)
    val utmeldingEventsTable = TableId.of(DATASET_NAME, UTMELDING_EVENTS)
    val utmeldingCountsTable = TableId.of(DATASET_NAME, UTMELDING_COUNTS)

    private fun TableId.insertRequest(row: Map<String, Any>): InsertAllRequest {
        return InsertAllRequest.newBuilder(this).addRow(row).build()
    }

    val bigQuery = BigQueryOptions.newBuilder().setProjectId(projectId).build().service
    val log = LoggerFactory.getLogger(this.javaClass)

    override fun loggAvsluttOppfolgingsperiode(oppfolgingPeriodeId: UUID, avregistreringsType: AvregistreringsType) {
        val erAutomatiskAvsluttet = avregistreringsType != AvregistreringsType.ManuellAvregistrering
        insertIntoOppfolgingEvents(oppfolgingsperiodeEventsTable) {
            mapOf(
                "id" to oppfolgingPeriodeId.toString(),
                "automatiskAvsluttet" to erAutomatiskAvsluttet,
                "timestamp" to ZonedDateTime.now().toOffsetDateTime().toString(),
                "event" to BigQueryEventType.OPPFOLGINGSPERIODE_SLUTT.name,
                "avregistreringsType" to avregistreringsType.name
            )
        }
    }

    override fun loggStartOppfolgingsperiode(
            startBegrunnelse: OppfolgingStartBegrunnelse,
            oppfolgingPeriodeId: UUID,
            startedAvType: StartetAvType,
            kvalifiseringsgruppe: Optional<Kvalifiseringsgruppe>) {
        insertIntoOppfolgingEvents(oppfolgingsperiodeEventsTable) {
            mapOf(
                "id" to oppfolgingPeriodeId.toString(),
                "startBegrunnelse" to startBegrunnelse.name,
                "startedAvType" to startedAvType.name,
                "timestamp" to ZonedDateTime.now().toOffsetDateTime().toString(),
                "event" to BigQueryEventType.OPFOLGINGSPERIODE_START.name,
                "kvalifiseringsgruppe" to kvalifiseringsgruppe.map { it.name }.orElse(null)
            )
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

    override fun loggUtmeldingsCount(utmelding: UtmeldingsAntall) {
        insertIntoOppfolgingEvents(utmeldingCountsTable) {
            mapOf(
                "personerIUtmelding" to utmelding.personerIUtemelding,
                "personIUtmeldingSomErUnderOppfolging" to utmelding.personIUtmeldingSomErUnderOppfolging,
                "timestamp" to ZonedDateTime.now().toOffsetDateTime().toString()
            )
        }
    }

    private fun insertIntoOppfolgingEvents(table: TableId, getRow: () -> Map<String, Any>?) {
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
