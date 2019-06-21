package no.nav.fo.veilarboppfolging.services;

import no.nav.fo.veilarboppfolging.domain.Oppfolgingsenhet;
import org.springframework.cache.annotation.Cacheable;

import static no.nav.fo.veilarboppfolging.config.CacheConfig.HENT_ENHET;
import static no.nav.sbl.rest.RestUtils.withClient;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;


public class OrganisasjonEnhetService {

    @Cacheable(HENT_ENHET)
    public Oppfolgingsenhet hentEnhet(String enhetId) {
        return withClient(client ->
                client.target(getRequiredProperty("NORG2_REST_API_URL_PROPERTY") + "/enhet/" + enhetId)
                        .request()
                        .get(Oppfolgingsenhet.class)
        );
    }
}
