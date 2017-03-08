package no.nav.fo.veilarbsituasjon.api;

import no.nav.fo.veilarbsituasjon.domain.OppfolgingStatus;
import no.nav.fo.veilarbsituasjon.domain.Vilkar;

import javax.ws.rs.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

@Path("/situasjon")
@Produces(APPLICATION_JSON)
public interface SituasjonOversikt {

    @GET
    public OppfolgingStatus hentOppfolgingsStatus() throws Exception;

    @GET
    @Path("/vilkar")
    public Vilkar hentVilkar() throws Exception;

    @POST
    @Path("/godta/{hash}")
    public OppfolgingStatus godta(@PathParam("hash") String hash) throws Exception;

}
