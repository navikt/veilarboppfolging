package no.nav.fo.veilarboppfolging.config;

import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import org.springframework.context.annotation.Configuration;

import static no.nav.sbl.rest.RestUtils.withClient;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Configuration
class Norg2RestHelsesjekk implements Helsesjekk {

    @Override
    public void helsesjekk() {
        withClient(client ->
                client.target(getRequiredProperty("NORG2_REST_API_URL_PROPERTY") + "/enhet/0101")
                        .request()
                        .get()
        );
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return new HelsesjekkMetadata(
                "norg2 rest api",
                getRequiredProperty("NORG2_REST_API_URL_PROPERTY") + "/enhet/",
                "REST-endepunkt for uhenting av navn på enhet basert på enhetId",
                false
        );
    }
}
