package no.nav.fo.veilarbsituasjon.config;

import no.nav.modig.security.ws.SystemSAMLOutInterceptor;
import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.DigitalKontaktinformasjonV1;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.lang.System.getProperty;
import static no.nav.sbl.dialogarena.types.Pingable.Ping.feilet;
import static no.nav.sbl.dialogarena.types.Pingable.Ping.lyktes;

@Configuration
public class DigitalKontaktinformasjonConfig {

    @Bean
    public DigitalKontaktinformasjonV1 dkifV1() {
        return factory();
    }

    @Bean
    public Pingable dkifV1Ping() {
        return () -> {
            try {
                factory().ping();
                return lyktes("DKIF_V1");
            } catch (Exception e) {
                return feilet("DKIF_V1", e);
            }
        };
    }

    private DigitalKontaktinformasjonV1 factory() {
        return new CXFClient<>(DigitalKontaktinformasjonV1.class)
                .address(getProperty("dkif.endpoint.url"))
                .withOutInterceptor(new SystemSAMLOutInterceptor())
                .build();
    }
}
