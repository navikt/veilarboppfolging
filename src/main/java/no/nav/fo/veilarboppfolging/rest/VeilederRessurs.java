package no.nav.fo.veilarboppfolging.rest;

import io.swagger.annotations.Api;
import no.nav.apiapp.security.veilarbabac.Bruker;
import no.nav.apiapp.security.veilarbabac.VeilarbAbacPepClient;
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
    private VeilarbAbacPepClient pepClient;
    private AutorisasjonService autorisasjonService;

    public VeilederRessurs(AktorService aktorService,
                           VeilederTilordningerRepository veilederTilordningerRepository,
                           VeilarbAbacPepClient pepClient,
                           AutorisasjonService autorisasjonService) {
        this.aktorService = aktorService;
        this.veilederTilordningerRepository = veilederTilordningerRepository;
        this.pepClient = pepClient;
        this.autorisasjonService = autorisasjonService;
    }

    @GET
    @Path("/veileder")
    public Veileder getVeileder(@PathParam("fnr") String fnr) {
        autorisasjonService.skalVereInternBruker();

        Bruker bruker = Bruker.fraFnr(fnr)
                .medAktoerIdSupplier(() -> aktorService.getAktorId(fnr)
                        .orElseThrow(() -> new IllegalArgumentException("Aktoerid ikke funnet")));

        pepClient.sjekkLesetilgangTilBruker(bruker);
        String veilederIdent = veilederTilordningerRepository.hentTilordningForAktoer(bruker.getAktoerId());
        return new Veileder(veilederIdent);
    }

}
