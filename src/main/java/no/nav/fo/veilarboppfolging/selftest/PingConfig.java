package no.nav.fo.veilarboppfolging.selftest;


import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.sbl.dialogarena.types.Pingable.Ping.PingMetadata;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static no.nav.sbl.dialogarena.common.abac.pep.service.AbacServiceConfig.ABAC_ENDPOINT_URL_PROPERTY_NAME;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Configuration
public class PingConfig {

    private final Pep pep;

    PingConfig(Pep pep) {
        this.pep = pep;
    }

    @Bean
    public Pingable pepPing() {
        PingMetadata metadata = new PingMetadata(
                "ABAC via " + getRequiredProperty(ABAC_ENDPOINT_URL_PROPERTY_NAME),
                "Tilgangskontroll, sjekk om NAV-ansatt har tilgang til bruker.",
                true
        );

        return () -> {
            try {
                pep.ping();
                return Pingable.Ping.lyktes(metadata);
            } catch (Exception e) {
                return Pingable.Ping.feilet(metadata, e);
            }
        };
    }

    @Bean
    public Pingable issoPing() throws IOException {
        PingMetadata metadata = new PingMetadata(
                "ISSO via " + System.getProperty("isso.isalive.url"),
                "Sjekker om is-alive til ISSO svarer. Single-signon pålogging.",
                true
        );

        return () -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(System.getProperty("isso.isalive.url")).openConnection();
                connection.connect();
                if (connection.getResponseCode() == 200) {
                    return Pingable.Ping.lyktes(metadata);
                }
                return Pingable.Ping.feilet(metadata, "Isalive returnerte statuskode: " + connection.getResponseCode());
            } catch (Exception e) {
                return Pingable.Ping.feilet(metadata, e);
            }
        };
    }

}
