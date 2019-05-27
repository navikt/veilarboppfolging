package no.nav.fo.veilarboppfolging.rest;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import no.nav.fo.veilarboppfolging.domain.OppfolgingTable;
import org.springframework.stereotype.Component;

import io.swagger.annotations.Api;
import no.nav.fo.veilarboppfolging.rest.domain.UnderOppfolgingDTO;
import no.nav.fo.veilarboppfolging.services.OppfolgingService;

@Component
@Path("")
@Api(value = "UnderOppfølging")
@Produces(APPLICATION_JSON)
public class UnderOppfolgingRessurs {

    private final OppfolgingService oppfolgingService;

    private final FnrParameterUtil fnrParameterUtil;

    public UnderOppfolgingRessurs(OppfolgingService oppfolgingService, FnrParameterUtil fnrParameterUtil) {
        this.oppfolgingService = oppfolgingService;
        this.fnrParameterUtil = fnrParameterUtil;
    }

    @GET
    @Path("/underoppfolging")
    public UnderOppfolgingDTO underOppfolging() {
        OppfolgingTable oppfolgingData = oppfolgingService.oppfolgingData(fnrParameterUtil.getFnr());
        boolean harData = oppfolgingData != null;
        return new UnderOppfolgingDTO()
                .setUnderOppfolging(harData && oppfolgingData.isUnderOppfolging())
                .setErManuell(harData && oppfolgingData.getGjeldendeManuellStatusId() > 0);
    }

}
