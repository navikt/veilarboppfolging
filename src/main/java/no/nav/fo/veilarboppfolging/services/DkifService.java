package no.nav.fo.veilarboppfolging.services;

import no.nav.brukerdialog.security.oidc.SystemUserTokenProvider;
import no.nav.sbl.rest.RestUtils;
import javax.inject.Inject;
import java.util.UUID;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static no.nav.fo.veilarboppfolging.config.ApplicationConfig.APPLICATION_NAME;

public class DkifService {

    @Inject
    private SystemUserTokenProvider systemUserTokenProvider;

    public String sjekkDkifRest(String fnr) {
        UUID uuid = UUID.randomUUID();
        String callId = Long.toHexString(uuid.getMostSignificantBits()) + Long.toHexString(uuid.getLeastSignificantBits());

        return RestUtils.withClient(c ->
                c.target("http://dkif.default.svc.nais.local/api/v1/personer/kontaktinformasjon")
                        .queryParam("inkluderSikkerDigitalPost", "false")
                        .request()
                        .header(AUTHORIZATION, "Bearer " + systemUserTokenProvider.getToken())
                        .header("Nav-Personidenter", fnr)
                        .header("Nav-Call-Id", callId)
                        .header("Nav-Consumer-Id", APPLICATION_NAME)
                        .get(String.class));
    }
}