package no.nav.fo.veilarboppfolging.config;

import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import no.nav.sbl.rest.RestUtils;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.DigitalKontaktinformasjonV1;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import static no.nav.fo.veilarboppfolging.config.ApplicationConfig.VIRKSOMHET_DIGITALKONTAKINFORMASJON_V1_PROPERTY;
import static no.nav.sbl.dialogarena.types.Pingable.Ping.feilet;
import static no.nav.sbl.dialogarena.types.Pingable.Ping.lyktes;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Configuration
public class DigitalKontaktinformasjonConfig {

    @Bean
    public DigitalKontaktinformasjonV1 dkifV1() {
        return factory();
    }

    private UnleashService unleashService;

    @Inject
    public DigitalKontaktinformasjonConfig(UnleashService unleashService) {
        this.unleashService = unleashService;
    }

    @Bean
    public Pingable dkifV1Ping() {

        if (unleashService.isEnabled("veilarboppfolging.dkif_rest")) {

            Response response = RestUtils.withClient(c ->
                    c.target("https://dkif.nais.preprod.local/internal/isAlive")
                            .request()
                            .get()
            );

            if (response.getStatus() != 200) {
                throw new RuntimeException("DKIF er nede");
            }

        } else {
            Pingable.Ping.PingMetadata metadata = new Pingable.Ping.PingMetadata(
                    "DKIF_V1 via " + getRequiredProperty(VIRKSOMHET_DIGITALKONTAKINFORMASJON_V1_PROPERTY),
                    "Ping av DKIF_V1. Henter reservasjon fra KRR.",
                    false
            );
            return () -> {
                try {
                    factory().ping();
                    return lyktes(metadata);
                } catch (Exception e) {
                    return feilet(metadata, e);
                }
            };
        }

    }

    private DigitalKontaktinformasjonV1 factory() {
        return new CXFClient<>(DigitalKontaktinformasjonV1.class)
                .address(getRequiredProperty(VIRKSOMHET_DIGITALKONTAKINFORMASJON_V1_PROPERTY))
                .configureStsForSystemUserInFSS()
                .build();
    }
}
