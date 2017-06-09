package no.nav.fo.veilarbsituasjon.config;

import no.nav.fo.veilarbsituasjon.services.VeilarbaktivtetService;
import no.nav.sbl.dialogarena.restclient.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;

@Configuration
public class RestClientConfig {

    @Bean
    public VeilarbaktivtetService aktoerV2Ping(Provider<HttpServletRequest> httpServletRequestProvider) {
        return new VeilarbaktivtetService(RestClient.build(httpServletRequestProvider, System.getProperty("veilarbaktivitet.url")));
    }

}
