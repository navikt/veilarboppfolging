package no.nav.veilarboppfolging.rest;

import static java.lang.String.valueOf;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.veilarboppfolging.rest.domain.UnderOppfolgingNiva3DTO;
import no.nav.metrics.MetricsFactory;
import no.nav.veilarboppfolging.utils.FnrParameterUtil;
import org.springframework.stereotype.Component;

import io.swagger.annotations.Api;
import no.nav.veilarboppfolging.services.OppfolgingService;

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
    private static final MeterRegistry meterRegistry = MetricsFactory.getMeterRegistry();


    public OppfolgingNiva3Ressurs(OppfolgingService oppfolgingService, FnrParameterUtil fnrParameterUtil) {
        this.oppfolgingService = oppfolgingService;
        this.fnrParameterUtil = fnrParameterUtil;
    }

    @GET
    @Path("/underoppfolging")
    public UnderOppfolgingNiva3DTO underOppfolgingNiva3() throws Exception {
        UnderOppfolgingNiva3DTO underOppfolgingNiva3DTO = new UnderOppfolgingNiva3DTO().setUnderOppfolging(oppfolgingService.underOppfolgingNiva3(fnrParameterUtil.getFnr()));

        meterRegistry.counter("request_niva3_underoppfolging",
                "underoppfolging",
                valueOf(underOppfolgingNiva3DTO.isUnderOppfolging()))
                .increment();

        MetricsFactory.createEvent("request.niva3.underoppfolging")
                .addTagToReport("underoppfolging", valueOf(underOppfolgingNiva3DTO.isUnderOppfolging()))
                .report();

        return underOppfolgingNiva3DTO;
    }
}
