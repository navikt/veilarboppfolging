package no.nav.fo.veilarbsituasjon.selftest;


import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.types.Pingable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

@Configuration
public class PingConfig {

    private final Pep pep;

    PingConfig(Pep pep) {
        this.pep = pep;
    }

    @Bean
    public Pingable pepPing() {
        return () -> {
            try {
                pep.ping();
                return Pingable.Ping.lyktes("ABAC");
            } catch( Exception e) {
                return Pingable.Ping.feilet("ABAC",e);
            }
        };
    }

    @Bean
    public Pingable issoPing() throws IOException {
        return () -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(System.getProperty("isso.isalive.url")).openConnection();
                connection.connect();
                if (connection.getResponseCode() == 200) {
                    return Pingable.Ping.lyktes("ISSO");
                }
                return Pingable.Ping.feilet("ISSO", new Exception("Statuskode: " + connection.getResponseCode()));
            } catch (Exception e) {
                return Pingable.Ping.feilet("ISSO", e);
            }
        };
    }

}
