package no.nav.fo.veilarbsituasjon.rest.api;

import no.nav.fo.veilarbsituasjon.rest.domain.Mal;
import no.nav.fo.veilarbsituasjon.rest.domain.OppfolgingStatus;
import no.nav.fo.veilarbsituasjon.rest.domain.Vilkar;

import javax.ws.rs.*;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/situasjon")
@Produces(APPLICATION_JSON)
public interface SituasjonOversikt {

    @GET
    OppfolgingStatus hentOppfolgingsStatus() throws Exception;

    @GET
    @Path("/vilkar")
    Vilkar hentVilkar() throws Exception;

    @POST
    @Path("/godta/{hash}")
    OppfolgingStatus godta(@PathParam("hash") String hash) throws Exception;

    @GET
    @Path("/mal")
    Mal hentMal();

    @GET
    @Path("/malListe")
    List<Mal> hentMalListe();

    @POST
    @Path("/mal")
    Mal oppdaterMal(Mal mal);
}