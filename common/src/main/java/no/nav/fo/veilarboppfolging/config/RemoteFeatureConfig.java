package no.nav.fo.veilarboppfolging.config;

import no.nav.sbl.featuretoggle.unleash.UnleashService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RemoteFeatureConfig {

    protected UnleashService unleashService;

    @Bean
    public RegistreringFeature registrereBrukerGenerellFeature() {
        return new RegistreringFeature(unleashService);
    }

    @Bean
    public OpprettBrukerIArenaFeature registrereBrukerArenaFeature() {
        return new OpprettBrukerIArenaFeature(unleashService);
    }

    @Bean
    public BrukervilkarFeature brukervilkarFeature(UnleashService unleashService) {
        return new BrukervilkarFeature(unleashService);
    }

    public static class RegistreringFeature extends RemoteFeatureConfig {

        public RegistreringFeature(UnleashService unleashService) {
            this.unleashService = unleashService;
        }

        public boolean erAktiv() {
            return unleashService.isEnabled("veilarboppfolging.registrering");
        }

    }

    public static class OpprettBrukerIArenaFeature extends RemoteFeatureConfig {

        public OpprettBrukerIArenaFeature(UnleashService unleashService) {
            this.unleashService = unleashService;
        }

        public boolean erAktiv() {
            return unleashService.isEnabled("veilarboppfolging.opprettbrukeriarena");
        }

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
