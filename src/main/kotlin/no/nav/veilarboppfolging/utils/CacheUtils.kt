package no.nav.veilarboppfolging.utils

import com.github.benmanes.caffeine.cache.Cache

object CacheUtils {

	fun <K : Any, V: Any> tryCacheFirstNullable(cache: Cache<K, V>, key: K, valueSupplier: () -> V?): V? {
		val value = cache.getIfPresent(key)

		return when (value) {
			null -> {
				valueSupplier()
					?.also { newValue -> cache.put(key, newValue) }
			}
			else -> value
		}
	}
}
