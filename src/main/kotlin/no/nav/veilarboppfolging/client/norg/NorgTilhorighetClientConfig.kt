package no.nav.veilarboppfolging.client.norg

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class NorgTilhorighetClientConfig(
	@Value("\${app.env.norg2Url}") val norgUrl: String
) {

	@Bean
    fun norgClient(): INorgTilhorighetClient {
		return NorgTilhorighetCachedClient(NorgTilhorighetClient(norgUrl))
	}
}
