package no.nav.fo.veilarbsituasjon.config;

import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.sbl.dialogarena.types.Pingable.Ping.PingMetadata;
import no.nav.tjeneste.virksomhet.aktoer.v2.binding.AktoerV2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.lang.System.getProperty;
import static no.nav.sbl.dialogarena.types.Pingable.Ping.feilet;
import static no.nav.sbl.dialogarena.types.Pingable.Ping.lyktes;

@Configuration
public class AktoerConfig {

    @Bean
    public AktoerV2 aktoerV2() {
        return factory();
    }

    @Bean
    public Pingable aktoerV2Ping() {
        PingMetadata metadata = new PingMetadata(
                "AKTOER_V2 via " + getProperty("aktoer.endpoint.url"),
                "Ping Aktoer_V2. Henter aktørid for for fnummer.",
                true
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

    public AktoerV2 factory() {
        return new CXFClient<>(AktoerV2.class)
                .address(getProperty("aktoer.endpoint.url"))
                .configureStsForSystemUserInFSS()
                .withMetrics()
                .build();
    }
}
