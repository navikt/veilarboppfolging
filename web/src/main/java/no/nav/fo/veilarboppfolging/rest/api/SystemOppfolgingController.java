package no.nav.fo.veilarboppfolging.rest.api;

import no.nav.fo.veilarboppfolging.domain.AktiverArbeidssokerData;
import no.nav.fo.veilarboppfolging.domain.Fnr;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

public interface SystemOppfolgingController {
    @POST
    @Path("/aktiverbruker")
    void aktiverBruker(AktiverArbeidssokerData aktiverArbeidssokerData) throws Exception;

    @POST
    @Path("/reaktiverbruker")
    void reaktiverBruker(Fnr fnr) throws Exception;
}
