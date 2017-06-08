package no.nav.fo.veilarbsituasjon.rest.api;

import no.nav.fo.veilarbsituasjon.rest.domain.AvslutningStatus;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

public interface SituasjonOversiktVeileder extends SituasjonOversikt {

    @GET
    @Path("/avslutningStatus")
    AvslutningStatus hentAvslutningStatus() throws Exception;

}