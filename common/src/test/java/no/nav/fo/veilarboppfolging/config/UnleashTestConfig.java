package no.nav.fo.veilarboppfolging.config;

import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Configuration
public class UnleashTestConfig {

    @Bean
    public UnleashService unleashService(){
        UnleashService unleashService = mock(UnleashService.class);
        when(unleashService.isEnabled(anyString())).thenReturn(true);
        return unleashService;
    }

}
