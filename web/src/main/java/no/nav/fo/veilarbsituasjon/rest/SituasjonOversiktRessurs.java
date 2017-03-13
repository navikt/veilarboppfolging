package no.nav.fo.veilarbsituasjon.rest;

import no.nav.fo.veilarbsituasjon.domain.OppfolgingStatusData;
import no.nav.fo.veilarbsituasjon.domain.VilkarData;
import no.nav.fo.veilarbsituasjon.rest.api.SituasjonOversikt;
import no.nav.fo.veilarbsituasjon.rest.domain.OppfolgingStatus;
import no.nav.fo.veilarbsituasjon.rest.domain.Vilkar;
import no.nav.fo.veilarbsituasjon.services.SituasjonOversiktService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;

@Component
public class SituasjonOversiktRessurs implements SituasjonOversikt {

    @Inject
    private SituasjonOversiktService situasjonOversiktService;

    @Inject
    private Provider<HttpServletRequest> requestProvider;

    @Override
    public OppfolgingStatus hentOppfolgingsStatus() throws Exception {
        return tilDto(situasjonOversiktService.hentOppfolgingsStatus(getFnr()));
    }

    @Override
    public Vilkar hentVilkar() throws Exception {
        return tilDto(situasjonOversiktService.hentVilkar());
    }

    @Override
    public OppfolgingStatus godta(String hash) throws Exception {
        return tilDto(situasjonOversiktService.godtaVilkar(hash, getFnr()));
    }

    public String getFnr() {
        return requestProvider.get().getParameter("fnr");
    }

    private OppfolgingStatus tilDto(OppfolgingStatusData oppfolgingStatusData) {
        return new OppfolgingStatus()
                .setFnr(oppfolgingStatusData.fnr)
                .setManuell(oppfolgingStatusData.manuell)
                .setReservasjonKRR(oppfolgingStatusData.reservasjonKRR)
                .setUnderOppfolging(oppfolgingStatusData.underOppfolging)
                .setVilkarMaBesvares(oppfolgingStatusData.vilkarMaBesvares)
                ;
    }

    private Vilkar tilDto(VilkarData vilkarData) {
        return new Vilkar()
                .setText(vilkarData.text)
                .setHash(vilkarData.hash);
    }

}
