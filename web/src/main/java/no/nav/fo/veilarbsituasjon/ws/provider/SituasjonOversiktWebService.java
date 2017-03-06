package no.nav.fo.veilarbsituasjon.ws.provider;

import lombok.val;
import no.nav.fo.veilarbsituasjon.domain.VilkarStatus;
import no.nav.fo.veilarbsituasjon.rest.domain.OppfolgingOgVilkarStatus;
import no.nav.fo.veilarbsituasjon.rest.domain.OpprettVilkarStatusRequest;
import no.nav.fo.veilarbsituasjon.rest.domain.OpprettVilkarStatusResponse;
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
        return new HentVilkaarsstatusResponse(); // TODO
    }

    @Override
    public HentVilkaarsstatusListeResponse hentVilkaarsstatusListe(HentVilkaarsstatusListeRequest hentVilkaarsstatusListeRequest) throws HentVilkaarsstatusListeSikkerhetsbegrensning {
        return new HentVilkaarsstatusListeResponse(); //TODO
    }

    @Override
    public void ping() {
    }

    @Override
    public OpprettVilkaarsstatusResponse opprettVilkaarsstatus(OpprettVilkaarsstatusRequest opprettVilkaarsstatusRequest) throws OpprettVilkaarsstatusSikkerhetsbegrensning, OpprettVilkaarsstatusUgyldigInput {

        OpprettVilkarStatusRequest req = new OpprettVilkarStatusRequest();
        req.setFnr(opprettVilkaarsstatusRequest.getPersonident());

        VilkarStatus vilkarStatus = VilkarStatus.valueOf(opprettVilkaarsstatusRequest.getStatus().name());

        req.setStatus(vilkarStatus);
        req.setHash(opprettVilkaarsstatusRequest.getHash());



        OpprettVilkarStatusResponse opprettVilkarStatusResponse;
        try {
            opprettVilkarStatusResponse = situasjonOversiktService.opprettVilkaarstatus(req);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        OpprettVilkaarsstatusResponse res = new OpprettVilkaarsstatusResponse();
        res.setStatusForOppdatering(1); // TODO: Services er implementert anenrledes

        return res;
    }

    @Override
    public HentVilkaarResponse hentVilkaar(HentVilkaarRequest hentVilkaarRequest) throws HentVilkaarSikkerhetsbegrensning {
        String vilkar;
        try {
            vilkar = situasjonOversiktService.hentVilkar();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return mapTilHentVilkaarResponse(vilkar);
    }

    public HentVilkaarResponse mapTilHentVilkaarResponse(String vilkar) {
        HentVilkaarResponse res = new HentVilkaarResponse();
        res.setVilkaarstekst(vilkar);
        res.setHash("ASDF");
        return res;
    }

    private HentOppfoelgingsstatusResponse mapToHentOppfoelgingsstatusResponse(OppfolgingOgVilkarStatus oppfolgingOgVilkarStatus) {
        Oppfoelgingsstatus oppfoelgingsstatus = new Oppfoelgingsstatus();
        oppfoelgingsstatus.setErBrukerSattTilManuell(oppfolgingOgVilkarStatus.isManuell());
        oppfoelgingsstatus.setErBrukerUnderOppfoelging(oppfolgingOgVilkarStatus.isUnderOppfolging());
        oppfoelgingsstatus.setErReservertIKontaktOgReservasjonsregisteret(oppfolgingOgVilkarStatus.isReservasjonKRR());
        oppfoelgingsstatus.setMaaVilkaarBesvares(oppfolgingOgVilkarStatus.isVilkarMaBesvares());
        oppfoelgingsstatus.setPersonident(oppfolgingOgVilkarStatus.getFnr());

        val res = new HentOppfoelgingsstatusResponse();
        res.setOppfoelgingsstatus(oppfoelgingsstatus);

        return res;
    }
}
