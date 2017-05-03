package no.nav.fo.veilarbsituasjon.config;

import no.nav.apiapp.ApiApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import static no.nav.apiapp.ApiApplication.Sone.FSS;

@Configuration
@ComponentScan(basePackages = "no.nav.fo.veilarbsituasjon")
public class ApplicationConfig implements ApiApplication {

    @Override
    public Sone getSone() {
        return FSS;
    }

}