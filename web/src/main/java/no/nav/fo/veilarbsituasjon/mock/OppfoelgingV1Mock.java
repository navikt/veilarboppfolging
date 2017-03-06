package no.nav.fo.veilarbsituasjon.mock;

import no.nav.tjeneste.virksomhet.oppfoelging.v1.HentOppfoelgingskontraktListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.WSBruker;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.WSOppfoelgingskontrakt;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.WSServiceGruppe;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingskontraktListeRequest;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingskontraktListeResponse;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingsstatusRequest;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingsstatusResponse;

import java.util.List;

public class OppfoelgingV1Mock implements OppfoelgingPortType {

    public static final String AKTIV_STATUS = "Aktiv";

    final private static String[] muligStatus = {AKTIV_STATUS, "LUKKET"};
    final private static String[] muligServicegruppe = {"Ikke vurdert", "Standardinnsats"};

    @Override
    public WSHentOppfoelgingsstatusResponse hentOppfoelgingsstatus(WSHentOppfoelgingsstatusRequest hentOppfoelgingsstatusRequest) {
        return new WSHentOppfoelgingsstatusResponse();
    }

    @Override
    public WSHentOppfoelgingskontraktListeResponse hentOppfoelgingskontraktListe(WSHentOppfoelgingskontraktListeRequest request) throws HentOppfoelgingskontraktListeSikkerhetsbegrensning {
        WSHentOppfoelgingskontraktListeResponse response = new WSHentOppfoelgingskontraktListeResponse();

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
        final WSServiceGruppe wsServiceGruppe = new WSServiceGruppe().withServiceGruppe(servicegruppe);
        servicegrupper.add(wsServiceGruppe);
    }

    @Override
    public void ping() {

    }
}
