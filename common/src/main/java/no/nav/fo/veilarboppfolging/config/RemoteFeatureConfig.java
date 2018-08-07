package no.nav.fo.veilarboppfolging.config;

import no.nav.sbl.featuretoggle.unleash.UnleashService;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RemoteFeatureConfig {

    @Inject
    protected UnleashService unleashService;

    @Bean
    public RegistreringFeature registrereBrukerGenerellFeature() {
        return new RegistreringFeature();
    }

    @Bean
    public OpprettBrukerIArenaFeature registrereBrukerArenaFeature() {
        return new OpprettBrukerIArenaFeature();
    }

    @Bean
    public BrukervilkarFeature brukervilkarFeature(UnleashService unleashService) {
        return new BrukervilkarFeature();
    }

    public static class RegistreringFeature extends RemoteFeatureConfig {

        public boolean erAktiv() {
            return unleashService.isEnabled("veilarboppfolging.registrering");
        }

    }

    public static class OpprettBrukerIArenaFeature extends RemoteFeatureConfig {

        public boolean erAktiv() {
            return unleashService.isEnabled("veilarboppfolging.opprettbrukeriarena");
        }

    }

    public static class BrukervilkarFeature extends RemoteFeatureConfig {

        public boolean erAktiv() {
            return unleashService.isEnabled("aktivitetsplan.brukervilkar");
        }
    }
}
