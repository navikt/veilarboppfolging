package no.nav.fo.veilarbsituasjon.rest;

import no.nav.fo.veilarbsituasjon.services.SituasjonOversiktService;
import org.glassfish.jersey.server.ResourceConfig;

public class RestConfig extends ResourceConfig {
    public RestConfig() {
        super(
                YtelseRessurs.class,
                OppfolgingRessurs.class,
                PortefoljeRessurs.class,
                SituasjonOversiktRessurs.class
        );
    }

}
