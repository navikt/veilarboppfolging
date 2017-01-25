package no.nav.fo.veilarbsituasjon.services;

import no.nav.fo.veilarbsituasjon.mappers.OppfoelgingMapper;
import no.nav.fo.veilarbsituasjon.rest.domain.OppfoelgingskontraktResponse;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.HentOppfoelgingskontraktListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.WSPeriode;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingskontraktListeRequest;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingskontraktListeResponse;

import javax.xml.datatype.XMLGregorianCalendar;

public class OppfoelgingService {
    private final OppfoelgingPortType oppfoelgingPortType;

    public OppfoelgingService(OppfoelgingPortType oppfoelgingPortType) {
        this.oppfoelgingPortType = oppfoelgingPortType;
    }

    public OppfoelgingskontraktResponse hentOppfoelgingskontraktListe(XMLGregorianCalendar fom, XMLGregorianCalendar tom, String fnr) {
        WSHentOppfoelgingskontraktListeRequest request = new WSHentOppfoelgingskontraktListeRequest();
        final WSPeriode periode = new WSPeriode();
        periode.setFom(fom);
        periode.setTom(tom);
        request.setPeriode(periode);
        request.setPersonidentifikator(fnr);
        WSHentOppfoelgingskontraktListeResponse response = null;
        try {
            response = oppfoelgingPortType.hentOppfoelgingskontraktListe(request);
        } catch (HentOppfoelgingskontraktListeSikkerhetsbegrensning hentOppfoelgingskontraktListeSikkerhetsbegrensning) {
            hentOppfoelgingskontraktListeSikkerhetsbegrensning.printStackTrace();
        }

        return OppfoelgingMapper.tilOppfoelgingskontrakt(response);
    }
}
