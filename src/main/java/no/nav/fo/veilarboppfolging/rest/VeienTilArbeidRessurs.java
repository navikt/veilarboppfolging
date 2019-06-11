package no.nav.fo.veilarboppfolging.rest;

import io.swagger.annotations.Api;
import lombok.experimental.var;
import no.nav.fo.veilarboppfolging.rest.domain.VeienTilArbeidDTO;
import no.nav.fo.veilarboppfolging.services.ArenaOppfolgingService;
import no.nav.fo.veilarboppfolging.services.OppfolgingService;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("/veientilarbeid")
@Api(value = "VeienTilArbeid")
@Produces(APPLICATION_JSON)
public class VeienTilArbeidRessurs {
    private final FnrParameterUtil fnrParameterUtil;
    private final OppfolgingService oppfolgingService;
    private final ArenaOppfolgingService arenaOppfolgingService;

    public VeienTilArbeidRessurs(OppfolgingService oppfolgingService, FnrParameterUtil fnrParameterUtil, ArenaOppfolgingService arenaOppfolgingService) {
        this.oppfolgingService = oppfolgingService;
        this.fnrParameterUtil = fnrParameterUtil;
        this.arenaOppfolgingService = arenaOppfolgingService;
    }

    @GET
    public VeienTilArbeidDTO veienTilArbeid() throws Exception {
        var oppfolgingData = oppfolgingService.hentOppfolgingsStatus(fnrParameterUtil.getFnr());
        var arenaData = arenaOppfolgingService.hentArenaOppfolging(fnrParameterUtil.getFnr());

        return new VeienTilArbeidDTO()
                .setUnderOppfolging(oppfolgingData.underOppfolging)
                .setKanReaktiveres(oppfolgingData.kanReaktiveres)
                .setReservasjonKRR(oppfolgingData.reservasjonKRR)
                .setServicegruppe(arenaData.getServicegruppe());
    }
}
