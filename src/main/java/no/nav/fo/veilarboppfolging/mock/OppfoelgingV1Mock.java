package no.nav.fo.veilarboppfolging.mock;

import no.nav.tjeneste.virksomhet.oppfoelging.v1.HentOppfoelgingskontraktListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.Bruker;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.Oppfoelgingskontrakt;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.ServiceGruppe;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingskontraktListeRequest;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingskontraktListeResponse;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingsstatusRequest;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingsstatusResponse;

import java.util.List;

public class OppfoelgingV1Mock implements OppfoelgingPortType {

    public static final String AKTIV_STATUS = "Aktiv";

    final private static String[] muligStatus = {AKTIV_STATUS, "LUKKET"};
    final private static String[] muligServicegruppe = {"Ikke vurdert", "Standardinnsats"};

    @Override
    public HentOppfoelgingsstatusResponse hentOppfoelgingsstatus(HentOppfoelgingsstatusRequest hentOppfoelgingsstatusRequest) {
        return new HentOppfoelgingsstatusResponse();
    }

    @Override
    public HentOppfoelgingskontraktListeResponse hentOppfoelgingskontraktListe(HentOppfoelgingskontraktListeRequest request) throws HentOppfoelgingskontraktListeSikkerhetsbegrensning {
        HentOppfoelgingskontraktListeResponse response = new HentOppfoelgingskontraktListeResponse();

        for (String status : muligStatus) {
            for (String servicegruppe : muligServicegruppe) {
                leggTilStatusOgServicegruppePaResponse(response, status, servicegruppe);
            }
        }
        return response;
    }

    private static void leggTilStatusOgServicegruppePaResponse(HentOppfoelgingskontraktListeResponse response, String status, String servicegruppe) {
        final Oppfoelgingskontrakt wsOppfoelgingskontrakt = new Oppfoelgingskontrakt();

        if (!status.isEmpty()) {
            wsOppfoelgingskontrakt.setStatus(status);
        }
        final Bruker bruker = new Bruker();

        if (!servicegruppe.isEmpty()) {
            leggTilServiceGrupperForBruker(servicegruppe, bruker);
        }
        wsOppfoelgingskontrakt.setGjelderBruker(bruker);
        response.getOppfoelgingskontraktListe().add(wsOppfoelgingskontrakt);
    }

    private static void leggTilServiceGrupperForBruker(String servicegruppe, Bruker bruker) {
        final List<ServiceGruppe> servicegrupper = bruker.getServicegruppe();
        final ServiceGruppe wsServiceGruppe = new ServiceGruppe();
        wsServiceGruppe.setServiceGruppe(servicegruppe);
        servicegrupper.add(wsServiceGruppe);
    }

    @Override
    public void ping() {

    }
}
