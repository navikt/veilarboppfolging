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

    @Bean
    public SjekkRegistrereBrukerGenerellFeature registrereBrukerGenerellFeature(RemoteFeatureToggleRepository repo) {
        return new SjekkRegistrereBrukerGenerellFeature(repo);
    }

    @Bean
    public SjekkRegistrereBrukerArenaFeature registrereBrukerArenaFeature(RemoteFeatureToggleRepository repo) {
        return new SjekkRegistrereBrukerArenaFeature(repo);
    }

    public static class SjekkPagaendeYtelserFeature extends RemoteFeatureToggle {
        public SjekkPagaendeYtelserFeature(RemoteFeatureToggleRepository repository) {
            super(repository, "veilarboppfolging.unngasjekkpagaendeytelser", false);
        }
    }

    public static class SjekkRegistrereBrukerGenerellFeature extends RemoteFeatureToggle {
        public SjekkRegistrereBrukerGenerellFeature(RemoteFeatureToggleRepository repository) {
            super(repository, "veilarboppfolging.registreringgenerell", false);
        }
    }

    public static class SjekkRegistrereBrukerArenaFeature extends RemoteFeatureToggle {
        public SjekkRegistrereBrukerArenaFeature(RemoteFeatureToggleRepository repository) {
            super(repository, "veilarboppfolging.registreringarena", false);
        }
    }
}
