package no.nav.veilarboppfolging.controller;

import io.swagger.annotations.Api;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.veilarboppfolging.db.VeilederTilordningerRepository;
import no.nav.veilarboppfolging.domain.AktorId;
import no.nav.veilarboppfolging.controller.domain.Veileder;
import no.nav.veilarboppfolging.services.AuthService;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.veilarboppfolging.utils.FnrUtils.getAktorIdOrElseThrow;

@Component
@Path("/person/{fnr}")
@Produces(APPLICATION_JSON)
@Api(value = "Veileder")
public class VeilederController {

    private AktorService aktorService;
    private VeilederTilordningerRepository veilederTilordningerRepository;
    private AuthService authService;

    public VeilederController(AktorService aktorService,
                              VeilederTilordningerRepository veilederTilordningerRepository,
                              AuthService authService) {
        this.aktorService = aktorService;
        this.veilederTilordningerRepository = veilederTilordningerRepository;
        this.authService = authService;
    }

    @GET
    @Path("/veileder")
    public Veileder getVeileder(@PathParam("fnr") String fnr) {
        authService.skalVereInternBruker();
        authService.sjekkLesetilgangMedFnr(fnr);

        AktorId aktorId = getAktorIdOrElseThrow(aktorService, fnr);
        String veilederIdent = veilederTilordningerRepository.hentTilordningForAktoer(aktorId.getAktorId());
        return new Veileder(veilederIdent);
    }

}
