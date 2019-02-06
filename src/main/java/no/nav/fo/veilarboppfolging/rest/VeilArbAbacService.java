package no.nav.fo.veilarboppfolging.rest;

import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import no.nav.brukerdialog.security.oidc.SystemUserTokenProvider;
import no.nav.sbl.rest.RestUtils;
import no.nav.sbl.util.EnvironmentUtils;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static no.nav.apiapp.util.UrlUtils.clusterUrlForApplication;

@Component
public class VeilArbAbacService implements Helsesjekk {

    @Inject
    SystemUserTokenProvider systemUserTokenProvider;

    private final String abacTargetUrl = EnvironmentUtils.getOptionalProperty("VEILARBABAC")
            .orElseGet(() -> clusterUrlForApplication("veilarbabac"));

    public boolean harVeilederSkriveTilgangTilFnr(String veilederId, String fnr) {
        return 200 == RestUtils.withClient(c -> c.target(abacTargetUrl)
                .path(veilederId)
                .path("person")
                .queryParam("fnr", fnr)
                .queryParam("action", "update")
                .request()
                .header(AUTHORIZATION, "Bearer " + systemUserTokenProvider.getToken())
                .get()
                .getStatus()
        );
    }

    @Override
    public void helsesjekk() throws Throwable {
        int status = RestUtils.withClient(c -> c.target(abacTargetUrl)
                .path("ping")
                .request()
                .get()
                .getStatus());

        if (status != 200) {
            throw new IllegalStateException("Rest kall mot veilarbabac feilet");
        }
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return new HelsesjekkMetadata(
                "veilarbabac helsesjekk",
                 abacTargetUrl,
                "Sjekker om veilarbabac endepunkt svarer",
                true
        );
    }
}
