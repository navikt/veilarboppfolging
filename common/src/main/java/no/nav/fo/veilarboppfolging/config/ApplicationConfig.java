package no.nav.fo.veilarboppfolging.config;

import no.nav.apiapp.ApiApplication;
import no.nav.dialogarena.aktor.AktorConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import static no.nav.apiapp.ApiApplication.Sone.FSS;

@Configuration
@EnableScheduling
@ComponentScan(
        basePackages = "no.nav.fo.veilarboppfolging",
        excludeFilters = { @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*Test")}
        )
@Import(AktorConfig.class)
public class ApplicationConfig implements ApiApplication {

    public static final String APPLICATION_NAME = "veilarboppfolging";

    @Override
    public String getApplicationName() {
        return APPLICATION_NAME;
    }

    @Override
    public Sone getSone() {
        return FSS;
    }

}