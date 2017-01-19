package no.nav.fo.veilarbsituasjon.mock;

import no.nav.tjeneste.virksomhet.oppfoelging.v1.HentOppfoelgingskontraktListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.*;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingskontraktListeRequest;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingskontraktListeResponse;

import java.util.ArrayList;
import java.util.List;

public class OppfoelgingV1Mock implements OppfoelgingPortType {
    @Override
    public WSHentOppfoelgingskontraktListeResponse hentOppfoelgingskontraktListe(WSHentOppfoelgingskontraktListeRequest request) throws HentOppfoelgingskontraktListeSikkerhetsbegrensning {
        WSHentOppfoelgingskontraktListeResponse response = new WSHentOppfoelgingskontraktListeResponse()
                .withOppfoelgingskontraktListe(new ArrayList<>());
        final WSOppfoelgingskontrakt wsOppfoelgingskontrakt = new WSOppfoelgingskontrakt();
        final WSBruker bruker = new WSBruker();
        final List<WSServiceGruppe> servicegrupper = bruker.getServicegruppe();
        final WSServiceGruppe wsServiceGruppe = new WSServiceGruppe();
        wsServiceGruppe.setServiceGruppe("Ikke vurdert");
        servicegrupper.add(wsServiceGruppe);
        wsOppfoelgingskontrakt.setGjelderBruker(bruker);
        response.getOppfoelgingskontraktListe().add(wsOppfoelgingskontrakt);
        return response;
    }

    @Override
    public void ping() {

    }
}
