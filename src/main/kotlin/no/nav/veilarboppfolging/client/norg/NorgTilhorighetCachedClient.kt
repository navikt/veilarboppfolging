package no.nav.veilarboppfolging.client.norg

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine

import no.nav.veilarboppfolging.utils.CacheUtils.tryCacheFirstNullable
import java.util.concurrent.TimeUnit

typealias NavEnhetId = String

class NorgTilhorighetCachedClient(private val INorgTilhorighetClient: INorgTilhorighetClient) : INorgTilhorighetClient {

    private val hentTilhorendeNavEnhetIdCache: Cache<NorgTilhorighetRequest, Enhet> = Caffeine.newBuilder()
        .expireAfterWrite(12, TimeUnit.HOURS)
        .build()

	override fun hentTilhorendeEnhet(
        norgTilhorighetRequest: NorgTilhorighetRequest
    ): Enhet? {
        return tryCacheFirstNullable(hentTilhorendeNavEnhetIdCache, norgTilhorighetRequest) {
			INorgTilhorighetClient.hentTilhorendeEnhet(norgTilhorighetRequest)
		}
	}
}
