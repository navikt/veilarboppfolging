package no.nav.fo.veilarbsituasjon.rest;

import no.nav.fo.veilarbsituasjon.ws.AktivitetsplanSituasjonWebService;
import org.glassfish.jersey.server.ResourceConfig;

public class RestConfig extends ResourceConfig {
    public RestConfig() {
        super(
                YtelseRessurs.class,
                OppfolgingRessurs.class,
                PortefoljeRessurs.class,
                VeilederRessurs.class,
                AktivitetsplanSituasjonWebService.class
        );
    }

}
