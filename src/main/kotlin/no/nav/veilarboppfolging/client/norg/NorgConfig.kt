package no.nav.veilarboppfolging.client.norg

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class NorgConfig(
	@Value("\${norg.url}") val norgUrl: String
) {

	@Bean
    fun norgClient(): NorgClient {
		return NorgCachedClient(NorgHttpClient(norgUrl))
	}
}
