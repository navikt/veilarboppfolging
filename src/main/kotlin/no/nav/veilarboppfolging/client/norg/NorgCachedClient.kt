package no.nav.veilarboppfolging.client.norg

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.common.types.identer.EnhetId
import no.nav.veilarboppfolging.client.pdl.GeografiskTilknytningNr
import no.nav.veilarboppfolging.utils.CacheUtils.tryCacheFirstNullable
import java.util.concurrent.TimeUnit

typealias NavEnhetId = String

class NorgCachedClient(private val norgClient: NorgClient) : NorgClient {

    private val hentTilhorendeNavEnhetIdCache: Cache<String, NavEnhetId> = Caffeine.newBuilder()
        .expireAfterWrite(12, TimeUnit.HOURS)
        .build()

	override fun hentTilhorendeEnhet(geografiskTilknytning: GeografiskTilknytningNr): EnhetId? {
        val value = tryCacheFirstNullable(hentTilhorendeNavEnhetIdCache, geografiskTilknytning.nr) {
			norgClient.hentTilhorendeEnhet(geografiskTilknytning)?.get()
		}
        return value?.let { EnhetId.of(it) }
	}
}
