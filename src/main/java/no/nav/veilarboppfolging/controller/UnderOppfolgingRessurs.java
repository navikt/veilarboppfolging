package no.nav.veilarboppfolging.controller;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import no.nav.veilarboppfolging.utils.FnrParameterUtil;
import org.springframework.stereotype.Component;

import io.swagger.annotations.Api;
import no.nav.veilarboppfolging.controller.domain.UnderOppfolgingDTO;
import no.nav.veilarboppfolging.services.OppfolgingService;

/**
 * Denne ressursen skal kun brukes til å hente data som veilarboppfolging har selv, uavhengig av integrasjoner. Hvis
 * behovet ikke dekkes av veilarboppfolging på egen hånd, bruk eksisterende OppfolgingRessurs
 */
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
        return oppfolgingService.oppfolgingData(fnrParameterUtil.getFnr());
    }

}
