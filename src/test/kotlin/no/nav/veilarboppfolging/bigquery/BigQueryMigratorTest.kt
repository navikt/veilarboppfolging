package no.nav.veilarboppfolging.bigquery

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.InsertAllRequest
import com.google.cloud.bigquery.InsertAllResponse
import com.google.cloud.bigquery.JobId
import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.TableResult
import no.nav.poao.dab.bigquery.migrator.BigQueryMigrator
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any

class BigQueryMigratorTest {

    private val bigQuery = mock(BigQuery::class.java)
    private val emptyTableResult = mock(TableResult::class.java)
    private val emptyInsertResponse = mock(InsertAllResponse::class.java)

    private val migrator = BigQueryMigrator(
        bigQuery = bigQuery,
        dataset = "test_dataset",
        migrationLocation = "db/bigquery",
    )

    @BeforeEach
    fun setup() {
        `when`(emptyTableResult.iterateAll()).thenReturn(emptyList())
        `when`(emptyInsertResponse.insertErrors).thenReturn(emptyMap())
        `when`(bigQuery.query(any<QueryJobConfiguration>())).thenReturn(emptyTableResult)
        `when`(bigQuery.insertAll(any<InsertAllRequest>())).thenReturn(emptyInsertResponse)
    }

    @Test
    fun `parser versjon og beskrivelse fra filnavn`() {
        val (versjon, beskrivelse) = migrator.parseFileName("V3__utmelding_counts.sql")
        assertThat(versjon).isEqualTo(3L)
        assertThat(beskrivelse).isEqualTo("utmelding counts")
    }

    @Test
    fun `parser filnavn med underscores i beskrivelse`() {
        val (versjon, beskrivelse) = migrator.parseFileName("V1__oppfolgingsperiode_events.sql")
        assertThat(versjon).isEqualTo(1L)
        assertThat(beskrivelse).isEqualTo("oppfolgingsperiode events")
    }

    @Test
    fun `feiler på ugyldig filnavn`() {
        assertThatThrownBy { migrator.parseFileName("ugyldig.sql") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("ugyldig.sql")
    }

    @Test
    fun `sorterer migrasjoner etter versjonsnummer, ikke leksikografisk`() {
        val filer = migrator.findMigrationFiles()
        val versjoner = filer.map { it.version }
        assertThat(versjoner).isSortedAccordingTo(compareBy { it })
        // V10 skal komme etter V9, ikke etter V1
        if (versjoner.size >= 2) {
            assertThat(versjoner).doesNotContainSequence(10L, 2L)
        }
    }

    @Test
    fun `beregner stabil sjekksum for samme innhold`() {
        val sql = "CREATE TABLE IF NOT EXISTS test (id STRING)"
        assertThat(migrator.checksum(sql)).isEqualTo(migrator.checksum(sql))
    }

    @Test
    fun `sjekksum er ulik for ulikt innhold`() {
        val sql1 = "CREATE TABLE IF NOT EXISTS test (id STRING)"
        val sql2 = "CREATE TABLE IF NOT EXISTS test (id STRING, navn STRING)"
        assertThat(migrator.checksum(sql1)).isNotEqualTo(migrator.checksum(sql2))
    }

    @Test
    fun `hopper over allerede kjørte migrasjoner`() {
        val migrasjoner = migrator.findMigrationFiles()
        if (migrasjoner.isEmpty()) return

        val historikkResultat = mock(TableResult::class.java)
        `when`(historikkResultat.iterateAll()).thenReturn(
            migrasjoner.map { mockRow(it.version.toString(), it.checksum) }
        )

        `when`(bigQuery.query(any<QueryJobConfiguration>()))
            .thenReturn(emptyTableResult)  // CREATE history table
            .thenReturn(emptyTableResult)  // SELECT failed
            .thenReturn(historikkResultat) // SELECT applied – alle er kjørt

        migrator.migrate()

        // Kun de 3 innledende spørringene – ingen migrasjoner kjøres
        verify(bigQuery, times(3)).query(any<QueryJobConfiguration>())
        verify(bigQuery, times(0)).insertAll(any<InsertAllRequest>())
    }

    @Test
    fun `feiler hardt ved sjekksum-avvik`() {
        val migrasjoner = migrator.findMigrationFiles()
        if (migrasjoner.isEmpty()) return

        val forsteMigrasjon = migrasjoner.first()
        val historikkResultat = mock(TableResult::class.java)
        `when`(historikkResultat.iterateAll()).thenReturn(
            listOf(mockRow(forsteMigrasjon.version.toString(), forsteMigrasjon.checksum + 1))
        )

        `when`(bigQuery.query(any<QueryJobConfiguration>()))
            .thenReturn(emptyTableResult)
            .thenReturn(emptyTableResult)
            .thenReturn(historikkResultat)

        assertThatThrownBy { migrator.migrate() }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Sjekksum-avvik")
            .hasMessageContaining(forsteMigrasjon.script)
    }

    @Test
    fun `feiler hardt ved feilet migrasjon i historikk`() {
        val migrasjoner = migrator.findMigrationFiles()
        if (migrasjoner.isEmpty()) return

        val feiletMigrasjon = migrasjoner.first()
        val feiletResultat = mock(TableResult::class.java)
        `when`(feiletResultat.iterateAll()).thenReturn(
            listOf(mockRow(feiletMigrasjon.script, 0))
        )

        `when`(bigQuery.query(any<QueryJobConfiguration>()))
            .thenReturn(emptyTableResult)  // CREATE history table
            .thenReturn(feiletResultat)    // SELECT failed → returnerer en feilet migrasjon

        assertThatThrownBy { migrator.migrate() }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("feilede migrasjoner")
    }

    @Test
    fun `skriver historikk med suksess=true etter vellykket migrasjon`() {
        `when`(bigQuery.query(any<QueryJobConfiguration>()))
            .thenReturn(emptyTableResult) // CREATE history table
            .thenReturn(emptyTableResult) // SELECT failed
            .thenReturn(emptyTableResult) // SELECT applied (ingen kjørt)
            .thenAnswer { emptyTableResult } // alle migration-queries

        migrator.migrate()

        val captor = ArgumentCaptor.forClass(InsertAllRequest::class.java)
        verify(bigQuery, times(migrator.findMigrationFiles().size)).insertAll(captor.capture())

        captor.allValues.forEach { request ->
            val row = request.rows.first().content
            assertThat(row["success"]).isEqualTo(true)
        }
    }

    private fun mockRow(vararg values: Any): com.google.cloud.bigquery.FieldValueList {
        val fieldValues = values.map { value ->
            com.google.cloud.bigquery.FieldValue.of(
                com.google.cloud.bigquery.FieldValue.Attribute.PRIMITIVE,
                value.toString()
            )
        }
        return com.google.cloud.bigquery.FieldValueList.of(fieldValues)
    }
}
