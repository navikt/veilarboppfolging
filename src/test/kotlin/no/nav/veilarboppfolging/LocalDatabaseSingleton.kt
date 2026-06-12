package no.nav.veilarboppfolging

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import no.nav.veilarboppfolging.test.DbTestUtils
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

object LocalDatabaseSingleton {
    val postgres = EmbeddedPostgres.start().postgresDatabase.also {
        DbTestUtils.initDb(it)
    }
    val jdbcTemplate = JdbcTemplate(postgres)
}