package no.nav.fo.veilarbsituasjon.mock;

import no.nav.tjeneste.virksomhet.oppfoelging.v1.HentOppfoelgingskontraktListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.*;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingskontraktListeRequest;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingskontraktListeResponse;

import java.util.ArrayList;
import java.util.List;

public class OppfoelgingV1Mock implements OppfoelgingPortType {

    final private static String[] muligStatus = {"Aktiv", "LUKKET"};
    final private static String[] muligServicegruppe = {"Ikke vurdert", "Standardinnsats"};

    @Override
    public WSHentOppfoelgingskontraktListeResponse hentOppfoelgingskontraktListe(WSHentOppfoelgingskontraktListeRequest request) throws HentOppfoelgingskontraktListeSikkerhetsbegrensning {
        WSHentOppfoelgingskontraktListeResponse response = new WSHentOppfoelgingskontraktListeResponse()
                .withOppfoelgingskontraktListe(new ArrayList<>());

        for (String status : muligStatus) {
            for (String servicegruppe : muligServicegruppe) {
                leggTilStatusOgServicegruppePaResponse(response, status, servicegruppe);
            }
        }
        return response;
    }

    private static void leggTilStatusOgServicegruppePaResponse(WSHentOppfoelgingskontraktListeResponse response, String status, String servicegruppe) {
        final WSOppfoelgingskontrakt wsOppfoelgingskontrakt = new WSOppfoelgingskontrakt();

        if (!status.isEmpty()) {
            wsOppfoelgingskontrakt.setStatus(status);
        }
        final WSBruker bruker = new WSBruker();

        if (!servicegruppe.isEmpty()) {
            leggTilServiceGrupperForBruker(servicegruppe, bruker);
        }
        wsOppfoelgingskontrakt.setGjelderBruker(bruker);
        response.getOppfoelgingskontraktListe().add(wsOppfoelgingskontrakt);
    }

    private static void leggTilServiceGrupperForBruker(String servicegruppe, WSBruker bruker) {
        final List<WSServiceGruppe> servicegrupper = bruker.getServicegruppe();
        final WSServiceGruppe wsServiceGruppe = new WSServiceGruppe();
        wsServiceGruppe.setServiceGruppe(servicegruppe);
        servicegrupper.add(wsServiceGruppe);
    }

    @Override
    public void ping() {

    }
}
