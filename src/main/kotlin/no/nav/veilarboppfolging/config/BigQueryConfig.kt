package no.nav.veilarboppfolging.config

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.BigQueryOptions
import no.nav.veilarboppfolging.bigquery.BigQueryMigrator
import no.nav.veilarboppfolging.eventsLogger.BigQueryClient
import no.nav.veilarboppfolging.eventsLogger.BigQueryClientImplementation
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.context.annotation.Profile

@Profile("!test")
@Configuration
open class BigQueryConfig(@Value("\${app.gcp.projectId}") val projectId: String) {

    @Bean
    open fun bigQuery(): BigQuery =
        BigQueryOptions.newBuilder().setProjectId(projectId).build().service

    @Bean
    open fun bigQueryMigrator(bigQuery: BigQuery): BigQueryMigrator =
        BigQueryMigrator(bigQuery, dataset = "oppfolging_metrikker").also { it.migrate() }

    @Bean
    @DependsOn("bigQueryMigrator")
    open fun bigQueryClient(bigQuery: BigQuery): BigQueryClient =
        BigQueryClientImplementation(bigQuery)
}
