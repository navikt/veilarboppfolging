package no.nav.fo.veilarbsituasjon.ws.provider;

import lombok.val;
import no.nav.fo.veilarbsituasjon.domain.OppfolgingStatus;
import no.nav.fo.veilarbsituasjon.domain.Vilkar;
import no.nav.fo.veilarbsituasjon.services.SituasjonOversiktService;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.binding.*;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.informasjon.Oppfoelgingsstatus;
import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.meldinger.*;
import org.springframework.stereotype.Service;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.inject.Inject;
import javax.jws.WebService;

@WebService
@Service
public class SituasjonOversiktWebService implements BehandleSituasjonV1 {

    @Inject
    private SituasjonOversiktService situasjonOversiktService;

    @Override
    public HentOppfoelgingsstatusResponse hentOppfoelgingsstatus(HentOppfoelgingsstatusRequest hentOppfoelgingsstatusRequest) throws HentOppfoelgingsstatusSikkerhetsbegrensning {
        OppfolgingStatus oppfolgingStatus = null;
        try {
            oppfolgingStatus = situasjonOversiktService.hentOppfolgingsStatus(hentOppfoelgingsstatusRequest.getPersonident());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return mapToHentOppfoelgingsstatusResponse(oppfolgingStatus);
    }

    @Override
    public HentVilkaarsstatusResponse hentVilkaarsstatus(HentVilkaarsstatusRequest hentVilkaarsstatusRequest) throws HentVilkaarsstatusSikkerhetsbegrensning {
        throw new NotImplementedException();
    }

    @Override
    public HentVilkaarsstatusListeResponse hentVilkaarsstatusListe(HentVilkaarsstatusListeRequest hentVilkaarsstatusListeRequest) throws HentVilkaarsstatusListeSikkerhetsbegrensning {
        throw new NotImplementedException();
    }

    @Override
    public void ping() {
    }

    @Override
    public OpprettVilkaarsstatusResponse opprettVilkaarsstatus(OpprettVilkaarsstatusRequest opprettVilkaarsstatusRequest) throws OpprettVilkaarsstatusSikkerhetsbegrensning, OpprettVilkaarsstatusUgyldigInput {

        OppfolgingStatus oppfolgingStatus;
        try {
            oppfolgingStatus = situasjonOversiktService.godtaVilkar(opprettVilkaarsstatusRequest.getHash(), opprettVilkaarsstatusRequest.getPersonident());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        OpprettVilkaarsstatusResponse res = new OpprettVilkaarsstatusResponse();
        res.setStatusForOppdatering(1); // TODO: Services er implementert anenrledes

        return res;
    }

    @Override
    public HentVilkaarResponse hentVilkaar(HentVilkaarRequest hentVilkaarRequest) throws HentVilkaarSikkerhetsbegrensning {
        Vilkar vilkar;
        try {
            vilkar = situasjonOversiktService.hentVilkar();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return mapTilHentVilkaarResponse(vilkar);
    }

    public HentVilkaarResponse mapTilHentVilkaarResponse(Vilkar vilkar) {
        HentVilkaarResponse res = new HentVilkaarResponse();
        res.setVilkaarstekst(vilkar.getText());
        res.setHash(vilkar.getHash());
        return res;
    }

    private HentOppfoelgingsstatusResponse mapToHentOppfoelgingsstatusResponse(OppfolgingStatus oppfolgingStatus) {
        Oppfoelgingsstatus oppfoelgingsstatus = new Oppfoelgingsstatus();
        oppfoelgingsstatus.setErBrukerSattTilManuell(oppfolgingStatus.isManuell());
        oppfoelgingsstatus.setErBrukerUnderOppfoelging(oppfolgingStatus.isUnderOppfolging());
        oppfoelgingsstatus.setErReservertIKontaktOgReservasjonsregisteret(oppfolgingStatus.isReservasjonKRR());
        oppfoelgingsstatus.setMaaVilkaarBesvares(oppfolgingStatus.isVilkarMaBesvares());
        oppfoelgingsstatus.setPersonident(oppfolgingStatus.getFnr());

        val res = new HentOppfoelgingsstatusResponse();
        res.setOppfoelgingsstatus(oppfoelgingsstatus);

        return res;
    }
}
