package no.nav.fo.veilarboppfolging.rest.api;

import no.nav.fo.veilarboppfolging.domain.InnstillingsHistorikk;
import no.nav.fo.veilarboppfolging.rest.domain.*;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.util.List;

public interface VeilederOppfolgingController {
    @POST
    @Path("/startOppfolging")
    OppfolgingStatus startOppfolging() throws Exception;

    @GET
    @Path("/avslutningStatus")
    OppfolgingStatus hentAvslutningStatus() throws Exception;

    @POST
    @Path("/avsluttOppfolging")
    @Consumes("application/json")
    OppfolgingStatus avsluttOppfolging(VeilederBegrunnelseDTO avsluttOppfolgingsperiode) throws Exception;

    @POST
    @Path("/settManuell")
    OppfolgingStatus settTilManuell(VeilederBegrunnelseDTO settTilManuel) throws Exception;

    @POST
    @Path("/settDigital")
    OppfolgingStatus settTilDigital(VeilederBegrunnelseDTO settTilDigital) throws Exception;

    @GET
    @Path("/innstillingsHistorikk")
    List<InnstillingsHistorikk> hentInnstillingsHistorikk() throws Exception;

    @POST
    @Path("/startEskalering")
    void startEskalering(StartEskaleringDTO startEskalering) throws Exception;

    @POST
    @Path("/stoppEskalering")
    void stoppEskalering(StoppEskaleringDTO stoppEskalering) throws Exception;

    @POST
    @Path("/startKvp")
    void startKvp(StartKvpDTO startKvp) throws Exception;

    @POST
    @Path("/stoppKvp")
    void stoppKvp(StoppKvpDTO stoppKvp) throws Exception;
}
