package no.nav.fo.veilarbsituasjon.rest.api;

import no.nav.fo.veilarbsituasjon.domain.InnstillingsHistorikk;
import no.nav.fo.veilarbsituasjon.domain.StartEskaleringDTO;
import no.nav.fo.veilarbsituasjon.rest.domain.EndreSituasjonDTO;
import no.nav.fo.veilarbsituasjon.rest.domain.OppfolgingStatus;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.util.List;

public interface VeilederSituasjonOversikt extends SituasjonOversikt {
    @POST
    @Path("/startOppfolging")
    OppfolgingStatus startOppfolging() throws Exception;

    @GET
    @Path("/avslutningStatus")
    OppfolgingStatus hentAvslutningStatus() throws Exception;

    @POST
    @Path("/avsluttOppfolging")
    @Consumes("application/json")
    OppfolgingStatus avsluttOppfolging(EndreSituasjonDTO avsluttOppfolgingsperiode) throws Exception;

    @POST
    @Path("/settManuell")
    OppfolgingStatus settTilManuell(EndreSituasjonDTO settTilManuel) throws Exception;

    @POST
    @Path("/settDigital")
    OppfolgingStatus settTilDigital(EndreSituasjonDTO settTilDigital) throws Exception;

    @GET
    @Path("/innstillingsHistorikk")
    List<InnstillingsHistorikk> hentInnstillingsHistorikk() throws Exception;

    @POST
    @Path("/startEskalering")
    void startEskalering(StartEskaleringDTO startEskalering) throws Exception;

    @POST
    @Path("/stoppEskalering")
    void stoppEskalering() throws Exception;
}
