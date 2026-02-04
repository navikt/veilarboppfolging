package no.nav.veilarboppfolging.config

import no.nav.veilarboppfolging.eventsLogger.BigQueryClient
import no.nav.veilarboppfolging.eventsLogger.BigQueryClientImplementation
import no.nav.veilarboppfolging.eventsLogger.BigQueryClientKontor
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Profile("!test")
@Configuration
open class BigQueryConfig(@Value("\${app.gcp.projectId}") val projectId: String) {

    @Bean
    open fun bigQueryClient(): BigQueryClient {
        return BigQueryClientImplementation(projectId)
    }

    @Bean
    open fun bigQueryClientKontor(): BigQueryClientKontor {
        return BigQueryClientKontor(projectId)
    }
}
