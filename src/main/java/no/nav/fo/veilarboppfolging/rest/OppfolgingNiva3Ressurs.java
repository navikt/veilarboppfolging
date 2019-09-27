package no.nav.fo.veilarboppfolging.rest;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.metrics.MetricsFactory.getMeterRegistry;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import io.micrometer.core.instrument.Counter;
import no.nav.fo.veilarboppfolging.rest.domain.UnderOppfolgingNiva3DTO;
import no.nav.metrics.MetricsFactory;
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
    private Counter counter;


    public OppfolgingNiva3Ressurs(OppfolgingService oppfolgingService, FnrParameterUtil fnrParameterUtil) {
        this.oppfolgingService = oppfolgingService;
        this.fnrParameterUtil = fnrParameterUtil;
        this.counter = Counter.builder("request_niva3_underoppfolging").register(getMeterRegistry());
    }

    @GET
    @Path("/underoppfolging")
    public UnderOppfolgingNiva3DTO underOppfolgingNiva3() throws Exception {
        UnderOppfolgingNiva3DTO underOppfolgingNiva3DTO = new UnderOppfolgingNiva3DTO().setUnderOppfolging(oppfolgingService.underOppfolgingNiva3(fnrParameterUtil.getFnr()));

        this.counter.increment();

        MetricsFactory.createEvent("request.niva3.underoppfolging").report();

        return underOppfolgingNiva3DTO;
    }
}
