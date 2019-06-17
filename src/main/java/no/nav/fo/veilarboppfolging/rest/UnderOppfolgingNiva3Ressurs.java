package no.nav.fo.veilarboppfolging.rest;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import no.nav.fo.veilarboppfolging.rest.domain.UnderOppfolgingNiva3DTO;
import org.springframework.stereotype.Component;

import io.swagger.annotations.Api;
import no.nav.fo.veilarboppfolging.services.OppfolgingService;

@Component
@Path("")
@Api(value = "UnderOppfølgingNiva3")
@Produces(APPLICATION_JSON)
public class UnderOppfolgingNiva3Ressurs {

    private final OppfolgingService oppfolgingService;

    private final FnrParameterUtil fnrParameterUtil;

    public UnderOppfolgingNiva3Ressurs(OppfolgingService oppfolgingService, FnrParameterUtil fnrParameterUtil) {
        this.oppfolgingService = oppfolgingService;
        this.fnrParameterUtil = fnrParameterUtil;
    }

    @GET
    @Path("/underoppfolgingniva3")
    public UnderOppfolgingNiva3DTO underOppfolgingNiva3() throws Exception {
        return new UnderOppfolgingNiva3DTO().setUnderOppfolging(oppfolgingService.underOppfolgingNiva3(fnrParameterUtil.getFnr()));
    }
}
