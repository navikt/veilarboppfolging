package no.nav.fo.veilarbsituasjon.rest;


import org.springframework.stereotype.Component;

import javax.ws.rs.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("/internal")
@Produces(APPLICATION_JSON)
public class InternalRessurs {

    @GET
    @Path("/isAlive")
    public String isAlive() {
        return "Application: UP";
    }
}
