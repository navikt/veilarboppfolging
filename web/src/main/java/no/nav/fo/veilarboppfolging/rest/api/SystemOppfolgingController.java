package no.nav.fo.veilarboppfolging.rest.api;

import no.nav.fo.veilarboppfolging.domain.AktiverArbeidssokerData;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

public interface SystemOppfolgingController {
    @POST
    @Path("/aktiveBruker")
    void aktiverBruker(AktiverArbeidssokerData aktiverArbeidssokerData) throws Exception;
}
