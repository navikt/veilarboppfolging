package no.nav.fo.veilarboppfolging.config;

import no.nav.sbl.featuretoggle.remote.RemoteFeatureToggle;
import no.nav.sbl.featuretoggle.remote.RemoteFeatureToggleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RemoteFeatureConfig {

    @Value("${feature_endpoint.url}")
    private String remoteFeatureUrl;

    @Bean
    public RemoteFeatureToggleRepository remoteFeatureToggleRespository() {
        return new RemoteFeatureToggleRepository(remoteFeatureUrl);
    }

    @Bean
    public SjekkPagaendeYtelserFeature pagaendeYtelserFeature(RemoteFeatureToggleRepository repo) {
        return new SjekkPagaendeYtelserFeature(repo);
    }

    public static class SjekkPagaendeYtelserFeature extends RemoteFeatureToggle {
        public SjekkPagaendeYtelserFeature(RemoteFeatureToggleRepository repository) {
            super(repository, "veilarboppfolging.unngasjekkpagaendeytelser", false);
        }
    }

}
