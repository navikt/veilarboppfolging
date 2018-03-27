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
    public RegistreringFeature registrereBrukerGenerellFeature(RemoteFeatureToggleRepository repo) {
        return new RegistreringFeature(repo);
    }

    @Bean
    public OpprettBrukerIArenaFeature registrereBrukerArenaFeature(RemoteFeatureToggleRepository repo) {
        return new OpprettBrukerIArenaFeature(repo);
    }

    @Bean
    public BrukervilkarFeature brukervilkarFeature(RemoteFeatureToggleRepository repo) {
        return new BrukervilkarFeature(repo);
    }

    public static class SjekkPagaendeYtelserFeature extends RemoteFeatureToggle {
        public SjekkPagaendeYtelserFeature(RemoteFeatureToggleRepository repository) {
            super(repository, "veilarboppfolging.unngasjekkpagaendeytelser", false);
        }
    }

    public static class RegistreringFeature extends RemoteFeatureToggle {
        public RegistreringFeature(RemoteFeatureToggleRepository repository) {
            super(repository, "veilarboppfolging.registrering", false);
        }
    }

    public static class OpprettBrukerIArenaFeature extends RemoteFeatureToggle {
        public OpprettBrukerIArenaFeature(RemoteFeatureToggleRepository repository) {
            super(repository, "veilarboppfolging.opprettbrukeriarena", false);
        }
    }

    public static class BrukervilkarFeature extends RemoteFeatureToggle {
        public BrukervilkarFeature(RemoteFeatureToggleRepository repository) {
            super(repository, "aktivitetsplan.brukervilkar", false);
        }
    }
}
