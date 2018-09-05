package no.nav.fo.veilarboppfolging.config;

import no.nav.sbl.featuretoggle.unleash.UnleashService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RemoteFeatureConfig {

    protected UnleashService unleashService;

    @Bean
    public BrukervilkarFeature brukervilkarFeature(UnleashService unleashService) {
        return new BrukervilkarFeature(unleashService);
    }

    public static class BrukervilkarFeature extends RemoteFeatureConfig {

        public BrukervilkarFeature(UnleashService unleashService) {
            this.unleashService = unleashService;
        }

        public boolean erAktiv() {
            return unleashService.isEnabled("aktivitetsplan.brukervilkar");
        }
    }
}
