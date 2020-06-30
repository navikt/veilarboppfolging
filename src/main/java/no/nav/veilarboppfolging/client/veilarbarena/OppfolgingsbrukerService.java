package no.nav.veilarboppfolging.client.veilarbarena;

import no.nav.veilarboppfolging.utils.mappers.VeilarbArenaOppfolging;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.util.Optional;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.veilarboppfolging.config.ApplicationConfig.VEILARBARENAAPI_URL_PROPERTY;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

public class OppfolgingsbrukerService {
    private final Client restClient;
    private final String host;

    public OppfolgingsbrukerService(Client restClient) {
        this.restClient = restClient;
        this.host = getRequiredProperty(VEILARBARENAAPI_URL_PROPERTY).toLowerCase();
    }

    public Optional<VeilarbArenaOppfolging> hentOppfolgingsbruker(String fnr) {
        Response response = restClient.target(String.format("%s/oppfolgingsbruker/%s", host, fnr))
                .request()
                .header(ACCEPT, APPLICATION_JSON)
                .get();

        if (response.getStatus() >= 300) {
            return Optional.empty();
        }

        return Optional.ofNullable(response.readEntity(VeilarbArenaOppfolging.class));
    }
}
