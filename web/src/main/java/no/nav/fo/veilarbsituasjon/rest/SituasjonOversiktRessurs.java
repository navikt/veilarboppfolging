package no.nav.fo.veilarbsituasjon.rest;

import no.nav.fo.veilarbsituasjon.rest.domain.OppfolgingOgVilkarStatus;
import no.nav.fo.veilarbsituasjon.rest.domain.OpprettVilkarStatusRequest;
import no.nav.fo.veilarbsituasjon.rest.domain.OpprettVilkarStatusResponse;
import no.nav.fo.veilarbsituasjon.services.SituasjonOversiktService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.ws.rs.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("/ws/aktivitetsplan")
@Produces(APPLICATION_JSON)
public class SituasjonOversiktRessurs {

    @Inject
    private SituasjonOversiktService situasjonOversiktService;

    @GET
    @Path("/{fnr}")
    public OppfolgingOgVilkarStatus hentOppfolgingsStatus(@PathParam("fnr") String fnr) throws Exception {
        return situasjonOversiktService.hentOppfolgingsStatus(fnr);
    }

    @GET
    @Path("/vilkar")
    public String hentVilkar() throws Exception {
        return situasjonOversiktService.hentVilkar();
    }

    @POST
    public OpprettVilkarStatusResponse opprettVilkaarstatus(OpprettVilkarStatusRequest opprettVilkarStatusRequest) throws Exception {
        return situasjonOversiktService.opprettVilkaarstatus(opprettVilkarStatusRequest);
    }

}
