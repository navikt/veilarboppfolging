package no.nav.veilarboppfolging.eventsLogger

import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.InsertAllRequest
import com.google.cloud.bigquery.TableId
import org.slf4j.LoggerFactory
import java.time.ZoneOffset
import java.time.ZonedDateTime

class BigQueryClientKontor(projectId: String) {
    val DATASET_NAME = "kontor_metrikker"
    val ARENAKONTOR_UTEN_AO_KONTOR = "ARENAKONTOR_UTEN_AO_KONTOR"
    val arenaKontorUtenAoKontorTable = TableId.of(DATASET_NAME, ARENAKONTOR_UTEN_AO_KONTOR)

    private fun TableId.insertRequest(row: Map<String, Any>): InsertAllRequest {
        return InsertAllRequest.newBuilder(this).addRow(row).build()
    }

    val bigQuery = BigQueryOptions.newBuilder().setProjectId(projectId).build().service
    val log = LoggerFactory.getLogger(BigQueryClient::class.java)

    fun loggAntallAvvikendeArenaOgAoKontor() {
        val fordelingRow = mapOf(
            "antallAvvik" to AntallArenakontorUtenAoKontor.antallAvvik,
            "timestamp" to ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime().toString(),
        )
        val request = arenaKontorUtenAoKontorTable.insertRequest(fordelingRow)
        insertWhileToleratingErrors(request)
    }

    fun loggAvvikendeArenaOgAoKontor() {
        val fordelingRow = mapOf(
            "oppfølgingsperiodeId" to ArenakontorUtenAoKontor.oppfolgingsperiodeId,
            "arenaKontor" to ArenakontorUtenAoKontor.arenaKontor,
            "aoKontor" to ArenakontorUtenAoKontor.aoKontor,
            "timestamp" to ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime().toString(),
        )
        val request = arenaKontorUtenAoKontorTable.insertRequest(fordelingRow)
        insertWhileToleratingErrors(request)
    }

    private fun insertWhileToleratingErrors(insertRequest: InsertAllRequest) {
        runCatching {
            val response = bigQuery.insertAll(insertRequest)
            val errors = response.insertErrors
            if (errors.isNotEmpty()) {
                log.error("Error inserting bigquery rows", errors)
            }
        }.onFailure {
            log.error("BigQuery error", it)
        }
    }
}

data class AntallArenakontorUtenAoKontor(
    val antallAvvik: Int
)

data class ArenakontorUtenAoKontor(
    val oppfolgingsperiodeId: String, // Er det lov?
    val arenaKontor: String,
    val aoKontor: String
)
