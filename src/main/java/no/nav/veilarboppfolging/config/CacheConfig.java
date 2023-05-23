package no.nav.veilarboppfolging.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String HENT_ENHET_CACHE_NAME = "hent_enhet_cache";

    public static final String HENT_ARBEIDSFORHOLD_CACHE_NAME = "hent_arbeidsforhold_cache";

    public static final String DIGDIR_KONTAKTINFO_CACHE_NAME = "digdir_kontaktinfo_cache";

    @Bean
    public Cache hentEnhetCache() {
        return new CaffeineCache(HENT_ENHET_CACHE_NAME, Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(10_000)
                .build());
    }

    @Bean
    public Cache hentArbeidsforholdCache() {
        return new CaffeineCache(HENT_ARBEIDSFORHOLD_CACHE_NAME, Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(10_000)
                .build());
    }

    @Bean
    public Cache digdirKontaktinfoCache() {
        return new CaffeineCache(DIGDIR_KONTAKTINFO_CACHE_NAME, Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .build());
    }

}
