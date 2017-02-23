package no.nav.fo.veilarbsituasjon.rest;

import org.glassfish.jersey.server.ResourceConfig;

public class RestConfig extends ResourceConfig {
    public RestConfig() {
        super(
                YtelseRessurs.class,
                OppfoelgingRessurs.class,
                PortefoljeRessurs.class
        );
    }

}
