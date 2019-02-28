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

    public static final String VEILARBABAC_HOSTNAME_PROPERTY = "VEILARBABAC";


    private final SystemUserTokenProvider systemUserTokenProvider;

    private final String abacTargetUrl = EnvironmentUtils.getOptionalProperty(VEILARBABAC_HOSTNAME_PROPERTY)
            .orElseGet(() -> clusterUrlForApplication("veilarbabac"));

    @Inject
    public VeilArbAbacService(SystemUserTokenProvider systemUserTokenProvider) {
        this.systemUserTokenProvider = systemUserTokenProvider;
    }

    public boolean harVeilederSkriveTilgangTilFnr(String veilederId, String fnr) {
        return "permit".equals(RestUtils.withClient(c -> c.target(abacTargetUrl)
                .path("subject")
                .path("person")
                .queryParam("fnr", fnr)
                .queryParam("action", "update")
                .request()
                .header(AUTHORIZATION, "Bearer " + systemUserTokenProvider.getToken())
                .header("subject", veilederId)
                .get(String.class)
        ));
    }

    @Override
    public void helsesjekk() {
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
