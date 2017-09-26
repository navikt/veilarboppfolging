package no.nav.fo.veilarboppfolging.config;

import no.nav.apiapp.ApiApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import static no.nav.apiapp.ApiApplication.Sone.FSS;

@Configuration
@ComponentScan(basePackages = "no.nav.fo.veilarboppfolging")
public class ApplicationConfig implements ApiApplication {

    @Override
    public Sone getSone() {
        return FSS;
    }

}