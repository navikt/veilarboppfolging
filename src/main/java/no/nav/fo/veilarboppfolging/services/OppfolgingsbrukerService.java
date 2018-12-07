package no.nav.fo.veilarboppfolging.services;

import no.nav.fo.veilarboppfolging.mappers.ArenaBruker;

import javax.ws.rs.client.Client;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.fo.veilarboppfolging.config.ApplicationConfig.VEILARBARENAAPI_URL_PROPERTY;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

public class OppfolgingsbrukerService {
    private final Client restClient;
    private final String host;

    public OppfolgingsbrukerService(Client restClient) {
        this.restClient = restClient;
        this.host = getRequiredProperty(VEILARBARENAAPI_URL_PROPERTY).toLowerCase();
    }

    public ArenaBruker hentOppfolgingsbruker(String fnr) {
        return restClient.target(String.format("%s/oppfolgingsbruker/%s", host, fnr))
                .request()
                .header(ACCEPT, APPLICATION_JSON)
                .get(ArenaBruker.class);
    }

}