package no.nav.fo.veilarboppfolging.rest;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import no.nav.fo.veilarboppfolging.rest.domain.UnderOppfolgingNiva3DTO;
import org.springframework.stereotype.Component;

import io.swagger.annotations.Api;
import no.nav.fo.veilarboppfolging.services.OppfolgingService;

/**
 * Denne ressursen er dedikert til å håndtere forespørsler på vegne av brukere som har innloggingstoken med nivå 3.
 * Ressursen ble opprettet for å dekke behov fra Ditt Nav.
 */
@Component
@Path("/niva3")
@Api(value = "OppfølgingNiva3")
@Produces(APPLICATION_JSON)
public class OppfolgingNiva3Ressurs {

    private final OppfolgingService oppfolgingService;
    private final FnrParameterUtil fnrParameterUtil;

    public OppfolgingNiva3Ressurs(OppfolgingService oppfolgingService, FnrParameterUtil fnrParameterUtil) {
        this.oppfolgingService = oppfolgingService;
        this.fnrParameterUtil = fnrParameterUtil;
    }

    @GET
    @Path("/underoppfolging")
    public UnderOppfolgingNiva3DTO underOppfolgingNiva3() throws Exception {
        return new UnderOppfolgingNiva3DTO().setUnderOppfolging(oppfolgingService.underOppfolgingNiva3(fnrParameterUtil.getFnr()));
    }
}
