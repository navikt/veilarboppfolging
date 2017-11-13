package no.nav.fo.veilarboppfolging.rest;

import io.swagger.annotations.Api;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.VeilederTilordningerRepository;
import no.nav.fo.veilarboppfolging.rest.domain.Veileder;
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
    private VeilederTilordningerRepository veilederTilordningerRepository;
    private PepClient pepClient;

    public VeilederRessurs(AktorService aktorService,
                           VeilederTilordningerRepository veilederTilordningerRepository,
                           PepClient pepClient) {
        this.aktorService = aktorService;
        this.veilederTilordningerRepository = veilederTilordningerRepository;
        this.pepClient = pepClient;
    }

    @GET
    @Path("/veileder")
    public Veileder getVeileder(@PathParam("fnr") String fnr) {
        pepClient.sjekkLeseTilgangTilFnr(fnr);
        String brukersAktoerId = aktorService.getAktorId(fnr)
                .orElseThrow(() -> new IllegalArgumentException("Fant ikke aktør for fnr: " + fnr));
        String veilederIdent = veilederTilordningerRepository.hentTilordningForAktoer(brukersAktoerId);
        return new Veileder()
                .withIdent(veilederIdent);
    }
}
