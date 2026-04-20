package no.nav.veilarboppfolging.datastream

import no.nav.poao.dab.bigquery.datastream.DatastreamKontraktTestBase
import no.nav.poao.dab.bigquery.datastream.Tabell
import no.nav.veilarboppfolging.LocalDatabaseSingleton
import org.springframework.jdbc.core.JdbcTemplate

/**
 * Verifiserer at [DatastreamKontrakt] stemmer overens med det faktiske Postgres-skjemaet.
 *
 * De tre testene (tabeller eksisterer, kolonner eksisterer, ingen udokumenterte kolonner)
 * arves fra [DatastreamKontraktTestBase].
 */
class DatastreamSkjemaTest : DatastreamKontraktTestBase() {
    override val tabeller: List<Tabell> = DatastreamKontrakt.tabeller
    override val jdbcTemplate: JdbcTemplate = LocalDatabaseSingleton.jdbcTemplate
}
