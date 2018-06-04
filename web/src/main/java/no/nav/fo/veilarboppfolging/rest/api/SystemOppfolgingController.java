package no.nav.fo.veilarboppfolging.rest.api;

import no.nav.fo.veilarboppfolging.domain.AktiverArbeidssokerData;
import no.nav.fo.veilarboppfolging.domain.AktiverBrukerResponseStatus;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

public interface SystemOppfolgingController {
    @POST
    @Path("/aktiverbruker")
    AktiverBrukerResponseStatus aktiverBruker(AktiverArbeidssokerData aktiverArbeidssokerData) throws Exception;
}
