package no.nav.fo.veilarbsituasjon.config;

import net.sf.ehcache.config.CacheConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static net.sf.ehcache.store.MemoryStoreEvictionPolicy.LRU;
import static no.nav.sbl.dialogarena.common.abac.pep.context.AbacContext.ABAC_CACHE;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String HENT_ENHET = "hentEnhet";

    private static final CacheConfiguration HENT_ENHET_CACHE = new CacheConfiguration(HENT_ENHET, 10000)
            .memoryStoreEvictionPolicy(LRU)
            .timeToIdleSeconds(3600)
            .timeToLiveSeconds(3600);

    @Bean
    public CacheManager cacheManager() {
        net.sf.ehcache.config.Configuration config = new net.sf.ehcache.config.Configuration();
        config.addCache(HENT_ENHET_CACHE);
        config.addCache(ABAC_CACHE);
        return new EhCacheCacheManager(net.sf.ehcache.CacheManager.newInstance(config));
    }
}