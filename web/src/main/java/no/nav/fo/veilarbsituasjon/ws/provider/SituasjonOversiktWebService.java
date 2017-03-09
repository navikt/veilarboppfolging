package no.nav.fo.veilarbsituasjon.ws.provider;

import lombok.SneakyThrows;
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
    @SneakyThrows
    public OpprettVilkaarsstatusResponse opprettVilkaarsstatus(OpprettVilkaarsstatusRequest opprettVilkaarsstatusRequest) throws OpprettVilkaarsstatusSikkerhetsbegrensning, OpprettVilkaarsstatusUgyldigInput {
        // TODO
        // TODO
        // TODO
        // TODO
        // TODO
        OppfolgingStatus oppfolgingStatus = situasjonOversiktService.godtaVilkar(opprettVilkaarsstatusRequest.getHash(), opprettVilkaarsstatusRequest.getPersonident());
//        OpprettVilkaarsstatusResponse res = new OpprettVilkaarsstatusResponse();
//        res.setStatusForOppdatering(1); // TODO: Services er implementert anenrledes
        return new OpprettVilkaarsstatusResponse();
    }

    @Override
    @SneakyThrows
    public HentVilkaarResponse hentVilkaar(HentVilkaarRequest hentVilkaarRequest) throws HentVilkaarSikkerhetsbegrensning {
        return mapTilHentVilkaarResponse(situasjonOversiktService.hentVilkar());
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
