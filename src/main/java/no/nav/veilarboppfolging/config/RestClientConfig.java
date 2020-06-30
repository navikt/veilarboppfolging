package no.nav.veilarboppfolging.config;

import no.nav.veilarboppfolging.client.DkifService;
import no.nav.veilarboppfolging.client.VeilarbaktivtetService;
import no.nav.sbl.dialogarena.restclient.RestClient;
import no.nav.sbl.util.EnvironmentUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;

import static no.nav.veilarboppfolging.config.ApplicationConfig.VEILARBAKTIVITETAPI_URL_PROPERTY;

@Configuration
public class RestClientConfig {

    @Bean
    public VeilarbaktivtetService veilarbaktivtetService(Provider<HttpServletRequest> httpServletRequestProvider) {
        return new VeilarbaktivtetService(RestClient.build(httpServletRequestProvider, EnvironmentUtils.getRequiredProperty(VEILARBAKTIVITETAPI_URL_PROPERTY)));
    }

    @Bean
    public DkifService dkifService() {
        return new DkifService();
    }

}
