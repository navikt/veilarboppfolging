package no.nav.fo.veilarboppfolging.config;

import no.nav.fo.veilarboppfolging.services.VeilarbaktivtetService;
import no.nav.sbl.dialogarena.restclient.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;

@Configuration
public class RestClientConfig {

    @Bean
    public VeilarbaktivtetService veilarbaktivtetService(Provider<HttpServletRequest> httpServletRequestProvider) {
        return new VeilarbaktivtetService(RestClient.build(httpServletRequestProvider, System.getProperty("veilarbaktivitet.url")));
    }

}
