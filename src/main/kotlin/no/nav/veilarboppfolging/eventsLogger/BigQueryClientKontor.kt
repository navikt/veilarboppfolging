package no.nav.veilarboppfolging.eventsLogger

import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.InsertAllRequest
import com.google.cloud.bigquery.TableId
import org.slf4j.LoggerFactory
import java.time.ZoneOffset
import java.time.ZonedDateTime

class BigQueryClientKontor(projectId: String) {
    companion object{
        val DATASET_NAME = "kontor_metrikker"
        val ARENAKONTOR_UTEN_AO_KONTOR = "ARENAKONTOR_UTEN_AO_KONTOR"
        val OPPFOLGINGSPERIODE_UTEN_AO_KONTOR = "OPPFOLGINGSPERIODE_UTEN_AO_KONTOR"
    }

    val arenaKontorUtenAoKontorTable = TableId.of(DATASET_NAME, ARENAKONTOR_UTEN_AO_KONTOR)
    val oppfolgingsperiodeUtenAoKontorTable = TableId.of(DATASET_NAME, OPPFOLGINGSPERIODE_UTEN_AO_KONTOR)

    val bigQuery = BigQueryOptions.newBuilder().setProjectId(projectId).build().service
    val log = LoggerFactory.getLogger(BigQueryClientKontor::class.java)

    fun loggAvvikendeArenaOgAoKontor(
        arenakontorUtenAoKontor: List<ArenakontorUtenAoKontor>,
        jobTimestamp: ZonedDateTime
    ) {
        if (arenakontorUtenAoKontor.isEmpty()) return

        val tidspunkt = jobTimestamp
            .withZoneSameInstant(ZoneOffset.UTC)
            .toOffsetDateTime()
            .toString()

        val rows = arenakontorUtenAoKontor.map {
            mapOf(
                "oppfolgingsperiodeId" to it.oppfolgingsperiodeId,
                "arenaKontor" to it.arenaKontor,
                "aoKontor" to it.aoKontor,
                "tidspunkt" to tidspunkt
            )
        }
        log.info("Sender ${rows.size} rader til BigQuery for avvikende arena og ao kontor")
        sendTilBigQuery(arenaKontorUtenAoKontorTable, rows)
    }

    fun loggOppfolgingsperioderUtenAoKontor(
        oppfolgingsperioderUtenAoKontor: List<OppfolgingsperiodeUtenAoKontor>,
        jobTimestamp: ZonedDateTime
    ) {
        if (oppfolgingsperioderUtenAoKontor.isEmpty()) return

        val tidspunkt = jobTimestamp
            .withZoneSameInstant(ZoneOffset.UTC)
            .toOffsetDateTime()
            .toString()

        val rows = oppfolgingsperioderUtenAoKontor.map {
            mapOf(
                "oppfolgingsperiodeId" to it.oppfolgingsperiodeId,
                "tidspunkt" to tidspunkt
            )
        }

        log.info("Sender ${rows.size} rader til BigQuery for oppfolgingsperioder uten ao kontor")
        sendTilBigQuery(oppfolgingsperiodeUtenAoKontorTable, rows)
    }

    private fun sendTilBigQuery(table: TableId, rows: List<Map<String, Any>>) {
        if (rows.isEmpty()) return
        val request = InsertAllRequest.newBuilder(table)
            .also { builder -> rows.forEach { builder.addRow(it) } }
            .build()
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

data class ArenakontorUtenAoKontor(
    val oppfolgingsperiodeId: String,
    val arenaKontor: String,
    val aoKontor: String
)

data class OppfolgingsperiodeUtenAoKontor(
    val oppfolgingsperiodeId: String,
)