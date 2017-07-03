package no.nav.fo.veilarbsituasjon.rest.api;

import no.nav.fo.veilarbsituasjon.domain.InnstillingsHistorikk;
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
    public OppfolgingStatus startOppfolging() throws Exception;

    @GET
    @Path("/avslutningStatus")
    public OppfolgingStatus hentAvslutningStatus() throws Exception;

    @POST
    @Path("/avsluttOppfolging")
    @Consumes("application/json")
    public OppfolgingStatus avsluttOppfolging(EndreSituasjonDTO avsluttOppfolgingsperiode) throws Exception;

    @POST
    @Path("/settManuell")
    public OppfolgingStatus settTilManuell(EndreSituasjonDTO settTilManuel) throws Exception;
    @POST
    @Path("/settDigital")
    public OppfolgingStatus settTilDigital(EndreSituasjonDTO settTilDigital) throws Exception;
    @GET
    @Path("hentInstillingsHistorikk")
    public List<InnstillingsHistorikk> hentInstillingsHistorikk() throws Exception;
}
