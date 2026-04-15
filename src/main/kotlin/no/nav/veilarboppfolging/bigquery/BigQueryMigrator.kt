package no.nav.veilarboppfolging.bigquery

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.TableId
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.zip.Adler32

/**
 * Kjører versjonerte SQL-migrasjoner mot BigQuery ved applikasjonsoppstart.
 *
 * Migrasjonshistorikk lagres i [dataset].bq_schema_history. Konvensjonen for
 * filnavn er identisk med Flyway: V{heltall}__{beskrivelse}.sql
 *
 * Migrasjonsfilene ligger under classpath:db/bigquery/ og bør kun inneholde
 * DDL som er idempotent (CREATE TABLE IF NOT EXISTS, ALTER TABLE ... ADD COLUMN IF NOT EXISTS).
 */
class BigQueryMigrator(
    private val bigQuery: BigQuery,
    private val dataset: String,
    private val migrationLocation: String = "db/bigquery",
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val historyTable = "bq_schema_history"

    fun migrate() {
        log.info("Starter BigQuery-migrasjoner mot dataset '$dataset'")
        ensureHistoryTableExists()

        val applied = fetchAppliedMigrations()
        val pending = findMigrationFiles().filter { it.version !in applied.map { a -> a.version } }

        if (pending.isEmpty()) {
            log.info("Ingen ventende BigQuery-migrasjoner")
            return
        }

        validateChecksums(findMigrationFiles(), applied)

        pending.forEach { migration ->
            log.info("Kjører BigQuery-migrasjon: ${migration.script}")
            runCatching { execute(migration) }
                .onSuccess { recordHistory(migration, success = true) }
                .onFailure { ex ->
                    recordHistory(migration, success = false)
                    throw IllegalStateException("BigQuery-migrasjon ${migration.script} feilet", ex)
                }
        }
        log.info("${pending.size} BigQuery-migrasjoner kjørt")
    }

    private fun ensureHistoryTableExists() {
        val sql = """
            CREATE TABLE IF NOT EXISTS $dataset.$historyTable (
                version      STRING    NOT NULL,
                description  STRING    NOT NULL,
                script       STRING    NOT NULL,
                checksum     INT64     NOT NULL,
                installed_on TIMESTAMP NOT NULL,
                success      BOOL      NOT NULL
            )
        """.trimIndent()
        bigQuery.query(QueryJobConfiguration.of(sql))
    }

    private fun fetchAppliedMigrations(): List<AppliedMigration> {
        val failedMigrations = bigQuery.query(
            QueryJobConfiguration.of("SELECT script FROM $dataset.$historyTable WHERE success = FALSE")
        ).iterateAll().map { it[0].stringValue }

        if (failedMigrations.isNotEmpty()) {
            throw IllegalStateException(
                "BigQuery har feilede migrasjoner som må rettes manuelt: $failedMigrations"
            )
        }

        return bigQuery.query(
            QueryJobConfiguration.of("SELECT version, checksum FROM $dataset.$historyTable WHERE success = TRUE")
        ).iterateAll().map {
            AppliedMigration(version = it[0].longValue, checksum = it[1].longValue)
        }
    }

    internal fun findMigrationFiles(): List<Migration> {
        val classLoader = Thread.currentThread().contextClassLoader
        val resourceDir = classLoader.getResource(migrationLocation)
            ?: run {
                log.warn("Fant ingen migrasjonsmappe på classpath: $migrationLocation")
                return emptyList()
            }

        return java.io.File(resourceDir.toURI())
            .listFiles { f -> f.name.matches(Regex("V\\d+__.+\\.sql")) }
            .orEmpty()
            .map { file ->
                val (version, description) = parseFileName(file.name)
                val sql = file.readText()
                Migration(
                    version = version,
                    description = description,
                    script = file.name,
                    sql = sql,
                    checksum = checksum(sql),
                )
            }
            .sortedBy { it.version }
    }

    internal fun parseFileName(filename: String): Pair<Long, String> {
        val match = Regex("V(\\d+)__(.+)\\.sql").matchEntire(filename)
            ?: throw IllegalArgumentException("Ugyldig migrasjonsfilnavn: $filename")
        val version = match.groupValues[1].toLong()
        val description = match.groupValues[2].replace('_', ' ')
        return version to description
    }

    internal fun checksum(sql: String): Long {
        val adler = Adler32()
        adler.update(sql.toByteArray(Charsets.UTF_8))
        return adler.value
    }

    private fun validateChecksums(allFiles: List<Migration>, applied: List<AppliedMigration>) {
        val appliedByVersion = applied.associateBy { it.version }
        allFiles.forEach { migration ->
            val appliedMigration = appliedByVersion[migration.version] ?: return@forEach
            if (appliedMigration.checksum != migration.checksum) {
                throw IllegalStateException(
                    "Sjekksum-avvik for BigQuery-migrasjon '${migration.script}'. " +
                    "Migrasjonen er allerede kjørt og kan ikke endres. " +
                    "Forventet sjekksum: ${appliedMigration.checksum}, faktisk: ${migration.checksum}"
                )
            }
        }
    }

    private fun execute(migration: Migration) {
        migration.sql
            .split(Regex(";\\s*\\n"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { statement ->
                bigQuery.query(QueryJobConfiguration.of(statement))
            }
    }

    private fun recordHistory(migration: Migration, success: Boolean) {
        val row = mapOf(
            "version" to migration.version.toString(),
            "description" to migration.description,
            "script" to migration.script,
            "checksum" to migration.checksum,
            "installed_on" to Instant.now().toString(),
            "success" to success,
        )
        bigQuery.insertAll(
            com.google.cloud.bigquery.InsertAllRequest.newBuilder(
                TableId.of(dataset, historyTable)
            ).addRow(row).build()
        )
    }

    data class Migration(
        val version: Long,
        val description: String,
        val script: String,
        val sql: String,
        val checksum: Long,
    )

    data class AppliedMigration(val version: Long, val checksum: Long)
}
