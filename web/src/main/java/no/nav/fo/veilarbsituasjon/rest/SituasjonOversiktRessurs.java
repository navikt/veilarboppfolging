package no.nav.fo.veilarbsituasjon.rest;

import no.nav.fo.veilarbsituasjon.api.SituasjonOversikt;
import no.nav.fo.veilarbsituasjon.domain.OppfolgingStatus;
import no.nav.fo.veilarbsituasjon.domain.Vilkar;
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
        return situasjonOversiktService.hentOppfolgingsStatus(getFnr());
    }

    @Override
    public Vilkar hentVilkar() throws Exception {
        return situasjonOversiktService.hentVilkar();
    }

    @Override
    public OppfolgingStatus godta(String hash) throws Exception {
        return situasjonOversiktService.godtaVilkar(hash, getFnr());
    }

    public String getFnr() {
        return requestProvider.get().getParameter("fnr");
    }

}
