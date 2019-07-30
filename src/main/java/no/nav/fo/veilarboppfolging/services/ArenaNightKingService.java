package no.nav.fo.veilarboppfolging.services;

import no.nav.fo.veilarboppfolging.domain.ArenaOppfolging;

import javax.ws.rs.client.Client;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.fo.veilarboppfolging.config.ApplicationConfig.ARENA_NIGHT_KING_URL_PROPERTY;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

public class ArenaNightKingService {
    private final Client restClient;
    private final String host;

    public ArenaNightKingService(Client restClient) {
        this.restClient = restClient;
        this.host = getRequiredProperty(ARENA_NIGHT_KING_URL_PROPERTY).toLowerCase();
    }

    public ArenaOppfolging hentArenaOppfolging(String fnr) {
        return
                restClient.target(String.format("%s/oppfolging?fnr=%s", host, fnr))
                        .request()
                        .header(ACCEPT, APPLICATION_JSON)
                        .get(ArenaOppfolging.class);
    }

}