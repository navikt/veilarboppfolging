package no.nav.fo.veilarbsituasjon.rest;

import io.swagger.annotations.Api;
import no.nav.fo.veilarbsituasjon.db.BrukerRepository;
import no.nav.fo.veilarbsituasjon.rest.domain.Veileder;
import no.nav.fo.veilarbsituasjon.services.AktoerIdService;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("/person/{fnr}")
@Produces(APPLICATION_JSON)
@Api(value= "Veileder")
public class VeilederRessurs {
    private AktoerIdService aktoerIdService;
    private BrukerRepository brukerRepository;

    public VeilederRessurs(AktoerIdService aktoerIdService, BrukerRepository brukerRepository) {
        this.aktoerIdService = aktoerIdService;
        this.brukerRepository = brukerRepository;
    }

    @GET
    @Path("/veileder")
    public Veileder getVeileder(@PathParam("fnr") String fnr) {
        String brukersAktoerId = aktoerIdService.findAktoerId(fnr);
        String veilederIdent = brukerRepository.hentVeilederForAktoer(brukersAktoerId);
        return new Veileder()
                .withIdent(veilederIdent);
    }
}
