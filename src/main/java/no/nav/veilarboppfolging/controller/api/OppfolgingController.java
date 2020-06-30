package no.nav.veilarboppfolging.controller.api;

import no.nav.veilarboppfolging.controller.domain.Bruker;
import no.nav.veilarboppfolging.controller.domain.Mal;
import no.nav.veilarboppfolging.controller.domain.OppfolgingStatus;

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

}