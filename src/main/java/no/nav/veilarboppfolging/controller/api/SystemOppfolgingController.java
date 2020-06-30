package no.nav.veilarboppfolging.controller.api;

import no.nav.veilarboppfolging.domain.AktiverArbeidssokerData;
import no.nav.veilarboppfolging.domain.Fnr;
import no.nav.veilarboppfolging.domain.SykmeldtBrukerType;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

public interface SystemOppfolgingController {
    @POST
    @Path("/aktiverbruker")
    void aktiverBruker(AktiverArbeidssokerData aktiverArbeidssokerData) throws Exception;

    @POST
    @Path("/reaktiverbruker")
    void reaktiverBruker(Fnr fnr) throws Exception;

    @POST
    @Path("/aktiverSykmeldt")
    void aktiverSykmeldt(SykmeldtBrukerType sykmeldtBrukerType) throws Exception;
}
