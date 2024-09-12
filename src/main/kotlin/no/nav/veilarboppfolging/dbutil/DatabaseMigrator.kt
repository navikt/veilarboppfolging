package no.nav.veilarboppfolging.dbutil

import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import javax.sql.DataSource

val log = LoggerFactory.getLogger("DatabaseMigrator")
fun migrateDb(flywayDataSource: DataSource) {
    log.info("Starting database migration...")
    val flyway = Flyway(
        Flyway.configure()
            .dataSource(flywayDataSource)
            .table("schema_version")
    )
    flyway.migrate()
}