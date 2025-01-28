package no.nav.veilarboppfolging.client.norg

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine

import no.nav.veilarboppfolging.utils.CacheUtils.tryCacheFirstNullable
import java.util.concurrent.TimeUnit

typealias NavEnhetId = String

class NorgCachedClient(private val norgClient: NorgClient) : NorgClient {

    private val hentTilhorendeNavEnhetIdCache: Cache<NorgRequest, Enhet> = Caffeine.newBuilder()
        .expireAfterWrite(12, TimeUnit.HOURS)
        .build()

	override fun hentTilhorendeEnhet(
        norgRequest: NorgRequest
    ): Enhet? {
        return tryCacheFirstNullable(hentTilhorendeNavEnhetIdCache, norgRequest) {
			norgClient.hentTilhorendeEnhet(norgRequest)
		}
	}
}
