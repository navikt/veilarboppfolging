package no.nav.veilarboppfolging.eventsLogger

import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.InsertAllRequest
import com.google.cloud.bigquery.TableId
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe
import no.nav.veilarboppfolging.domain.StartetAvType
import no.nav.veilarboppfolging.oppfolgingsbruker.OppfolgingStartBegrunnelse
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime
import java.util.*

enum class BigQueryEventType {
    OPFOLGINGSPERIODE_START,
    OPPFOLGINGSPERIODE_SLUTT,
}

interface BigQueryClient {
    fun loggStartOppfolgingsperiode(oppfolging: OppfolgingStartBegrunnelse, oppfolgingPeriodeId: UUID, startedAvType: StartetAvType, kvalifiseringsgruppe: Optional<Kvalifiseringsgruppe>)
    fun loggAvsluttOppfolgingsperiode(oppfolgingPeriodeId: UUID, erAutomstiskAvsluttet: Boolean)
}

class BigQueryClientImplementation(projectId: String): BigQueryClient {
    val OPPFOLGING_EVENTS = "OPPFOLGINGSPERIODE_EVENTS"
    val DATASET_NAME = "oppfolging_metrikker"
    val forhaandsvarselEventsTable = TableId.of(DATASET_NAME, OPPFOLGING_EVENTS)

    private fun TableId.insertRequest(row: Map<String, Any>): InsertAllRequest {
        return InsertAllRequest.newBuilder(this).addRow(row).build()
    }

    val bigQuery = BigQueryOptions.newBuilder().setProjectId(projectId).build().service
    val log = LoggerFactory.getLogger(this.javaClass)

    override fun loggAvsluttOppfolgingsperiode(oppfolgingPeriodeId: UUID, erAutomatiskAvsluttet: Boolean) {
        insertIntoOppfolgingEvents {
            mapOf(
                "id" to oppfolgingPeriodeId.toString(),
                "automatiskAvsluttet" to erAutomatiskAvsluttet,
                "timestamp" to ZonedDateTime.now().toOffsetDateTime().toString(),
                "event" to BigQueryEventType.OPPFOLGINGSPERIODE_SLUTT.name
            )
        }
    }

    override fun loggStartOppfolgingsperiode(
            startBegrunnelse: OppfolgingStartBegrunnelse,
            oppfolgingPeriodeId: UUID,
            startedAvType: StartetAvType,
            kvalifiseringsgruppe: Optional<Kvalifiseringsgruppe>) {
        insertIntoOppfolgingEvents {
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

    private fun insertIntoOppfolgingEvents(getRow: () -> Map<String, Any>) {
        runCatching {
            val insertRequest =forhaandsvarselEventsTable.insertRequest(getRow())
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
