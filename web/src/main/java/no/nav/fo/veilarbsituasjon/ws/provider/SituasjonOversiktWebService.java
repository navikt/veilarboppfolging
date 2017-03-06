package no.nav.fo.veilarbsituasjon.ws.provider;

import lombok.val;
import no.nav.fo.veilarbsituasjon.rest.domain.OppfolgingOgVilkarStatus;
import no.nav.fo.veilarbsituasjon.services.SituasjonOversiktService;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.binding.*;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.informasjon.Oppfoelgingsstatus;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.meldinger.*;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.jws.WebService;

@WebService
@Service
public class SituasjonOversiktWebService implements BehandleSituasjonV1 {

    @Inject
    private SituasjonOversiktService situasjonOversiktService;

    @Override
    public HentOppfoelgingsstatusResponse hentOppfoelgingsstatus(HentOppfoelgingsstatusRequest hentOppfoelgingsstatusRequest) throws HentOppfoelgingsstatusSikkerhetsbegrensning {

        OppfolgingOgVilkarStatus oppfolgingOgVilkarStatus = null;

        try {
            oppfolgingOgVilkarStatus = situasjonOversiktService.hentOppfolgingsStatus(hentOppfoelgingsstatusRequest.getPersonident());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return mapToHentOppfoelgingsstatusResponse(oppfolgingOgVilkarStatus);
    }

    @Override
    public HentVilkaarsstatusResponse hentVilkaarsstatus(HentVilkaarsstatusRequest hentVilkaarsstatusRequest) throws HentVilkaarsstatusSikkerhetsbegrensning {


        return new HentVilkaarsstatusResponse();
    }

    @Override
    public HentVilkaarsstatusListeResponse hentVilkaarsstatusListe(HentVilkaarsstatusListeRequest hentVilkaarsstatusListeRequest) throws HentVilkaarsstatusListeSikkerhetsbegrensning {

        return new HentVilkaarsstatusListeResponse();
    }

    @Override
    public void ping() {
    }

    @Override
    public OpprettVilkaarsstatusResponse opprettVilkaarsstatus(OpprettVilkaarsstatusRequest opprettVilkaarsstatusRequest) throws OpprettVilkaarsstatusSikkerhetsbegrensning, OpprettVilkaarsstatusUgyldigInput {
        return new OpprettVilkaarsstatusResponse();
    }

    @Override
    public HentVilkaarResponse hentVilkaar(HentVilkaarRequest hentVilkaarRequest) throws HentVilkaarSikkerhetsbegrensning {

        String vilkar;
        try {
            vilkar = situasjonOversiktService.hentVilkar();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        HentVilkaarResponse res = new HentVilkaarResponse();
        res.setVilkaarstekst(vilkar);

        return res;
    }

    private HentOppfoelgingsstatusResponse mapToHentOppfoelgingsstatusResponse(OppfolgingOgVilkarStatus oppfolgingOgVilkarStatus) {
        Oppfoelgingsstatus oppfoelgingsstatus = new Oppfoelgingsstatus();
        oppfoelgingsstatus.setErBrukerSattTilManuell(oppfolgingOgVilkarStatus.isManuell());
        oppfoelgingsstatus.setErBrukerUnderOppfoelging(oppfolgingOgVilkarStatus.isUnderOppfolging());
        oppfoelgingsstatus.setErReservertIKontaktOgReservasjonsregisteret(oppfolgingOgVilkarStatus.isReservasjonKRR());
        oppfoelgingsstatus.setMaaVilkaarBesvares(oppfolgingOgVilkarStatus.isVilkarMaBesvares());
        oppfoelgingsstatus.setPersonident(oppfolgingOgVilkarStatus.getFnr());

        val result = new HentOppfoelgingsstatusResponse();
        result.setOppfoelgingsstatus(oppfoelgingsstatus);

        return result;
    }
}
