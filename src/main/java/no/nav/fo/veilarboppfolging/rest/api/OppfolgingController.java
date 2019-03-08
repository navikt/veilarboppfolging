package no.nav.fo.veilarboppfolging.rest.api;

import no.nav.fo.veilarboppfolging.rest.domain.Bruker;
import no.nav.fo.veilarboppfolging.rest.domain.Mal;
import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingStatus;
import no.nav.fo.veilarboppfolging.rest.domain.UnderOppfolgingDTO;

import javax.ws.rs.*;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/oppfolging")
@Produces(APPLICATION_JSON)
public interface OppfolgingController {

    @GET
    @Path("/me")
    Bruker hentBrukerInfo() throws Exception;

    @GET
    OppfolgingStatus hentOppfolgingsStatus() throws Exception;

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
    @Path("/underOppfolging")
    UnderOppfolgingDTO underOppfolging() throws Exception;

}