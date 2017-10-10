package no.nav.fo.veilarbsituasjon.rest;

import io.swagger.annotations.Api;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarbsituasjon.db.BrukerRepository;
import no.nav.fo.veilarbsituasjon.rest.domain.Veileder;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("/person/{fnr}")
@Produces(APPLICATION_JSON)
@Api(value = "Veileder")
public class VeilederRessurs {
    private AktorService aktorService;
    private BrukerRepository brukerRepository;

    public VeilederRessurs(AktorService aktorService, BrukerRepository brukerRepository) {
        this.aktorService = aktorService;
        this.brukerRepository = brukerRepository;
    }

    @GET
    @Path("/veileder")
    public Veileder getVeileder(@PathParam("fnr") String fnr) {
        String brukersAktoerId = aktorService.getAktorId(fnr)
                .orElseThrow(() -> new IllegalArgumentException("Fant ikke aktør for fnr: " + fnr));
        String veilederIdent = brukerRepository.hentTilordningForAktoer(brukersAktoerId).getVeileder();
        return new Veileder()
                .withIdent(veilederIdent);
    }
}
