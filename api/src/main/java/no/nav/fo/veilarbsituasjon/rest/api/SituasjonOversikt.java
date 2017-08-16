package no.nav.fo.veilarbsituasjon.rest.api;

import no.nav.fo.veilarbsituasjon.rest.domain.*;

import javax.ws.rs.*;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/situasjon")
@Produces(APPLICATION_JSON)
public interface SituasjonOversikt {

    @GET
    @Path("/me")
    Bruker hentBrukerInfo() throws Exception;

    @GET
    OppfolgingStatus hentOppfolgingsStatus() throws Exception;

    @GET
    @Path("/vilkar")
    Vilkar hentVilkar() throws Exception;

    @GET
    @Path("/hentVilkaarStatusListe")
    List<Vilkar> hentVilkaarStatusListe() throws Exception;

    @POST
    @Path("/godta/{hash}")
    OppfolgingStatus godta(@PathParam("hash") String hash) throws Exception;

    @POST
    @Path("/avslaa/{hash}")
    OppfolgingStatus avslaa(@PathParam("hash") String hash) throws Exception;

    @GET
    @Path("/mal")
    Mal hentMal() throws Exception;

    @GET
    @Path("/malListe")
    List<Mal> hentMalListe() throws Exception;

    @POST
    @Path("/mal")
    Mal oppdaterMal(Mal mal) throws Exception;

    @GET
    @Path("/hentEskaleringstatus")
    Eskaleringstatus hentEskaleringstatus() throws Exception;

    @GET
    @Path("/hentEskaleringhistorikk")
    List<Eskaleringstatus> hentEskaleringhistorikk() throws Exception;
}