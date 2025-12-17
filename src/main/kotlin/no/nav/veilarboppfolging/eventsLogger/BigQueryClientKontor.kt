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

    val bigQuery = BigQueryOptions.newBuilder().setProjectId(projectId).build().service
    val log = LoggerFactory.getLogger(BigQueryClientKontor::class.java)

    fun loggAvvikendeArenaOgAoKontor(
        arenakontorUtenAoKontor: List<ArenakontorUtenAoKontor>,
        jobTimestamp: ZonedDateTime
    ) {
        if (arenakontorUtenAoKontor.isEmpty()) return

        val timestamp = jobTimestamp
            .withZoneSameInstant(ZoneOffset.UTC)
            .toOffsetDateTime()
            .toString()

        val request = InsertAllRequest.newBuilder(arenaKontorUtenAoKontorTable)
            .also { builder ->
                arenakontorUtenAoKontor.forEach {
                    builder.addRow(
                        it.oppfolgingsperiodeId, // insertId (idempotent)
                        mapOf(
                            "oppfolgingsperiodeId" to it.oppfolgingsperiodeId,
                            "arenaKontor" to it.arenaKontor,
                            "aoKontor" to it.aoKontor,
                            "timestamp" to timestamp
                        )
                    )
                }
            }
            .build()

        insertWhileToleratingErrors(request)
    }

    fun loggOppfolgingsperioderUtenAoKontor(
        oppfolgingsperioderUtenAoKontor: List<OppfolgingsperiodeUtenAoKontor>,
        jobTimestamp: ZonedDateTime
    ) {
        if (oppfolgingsperioderUtenAoKontor.isEmpty()) return

        val timestamp = jobTimestamp
            .withZoneSameInstant(ZoneOffset.UTC)
            .toOffsetDateTime()
            .toString()

        val request = InsertAllRequest.newBuilder(arenaKontorUtenAoKontorTable)
            .also { builder ->
                oppfolgingsperioderUtenAoKontor.forEach {
                    builder.addRow(
                        it.oppfolgingsperiodeId, // insertId (idempotent)
                        mapOf(
                            "oppfolgingsperiodeId" to it.oppfolgingsperiodeId,
                            "timestamp" to timestamp
                        )
                    )
                }
            }
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
    val oppfolgingsperiodeId: String, // Er det lov?
    val arenaKontor: String,
    val aoKontor: String
)

data class OppfolgingsperiodeUtenAoKontor(
    val oppfolgingsperiodeId: String,
)