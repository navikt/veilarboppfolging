package no.nav.fo.veilarbsituasjon.services;

import no.nav.fo.veilarbsituasjon.mappers.OppfoelgingMapper;
import no.nav.fo.veilarbsituasjon.rest.domain.OppfoelgingskontraktResponse;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.HentOppfoelgingskontraktListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.Periode;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingskontraktListeRequest;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingskontraktListeResponse;

import javax.xml.datatype.XMLGregorianCalendar;

public class OppfoelgingService {
    private final OppfoelgingPortType oppfoelgingPortType;

    public OppfoelgingService(OppfoelgingPortType oppfoelgingPortType) {
        this.oppfoelgingPortType = oppfoelgingPortType;
    }

    public OppfoelgingskontraktResponse hentOppfoelgingskontraktListe(XMLGregorianCalendar fom, XMLGregorianCalendar tom, String fnr) {
        HentOppfoelgingskontraktListeRequest request = new HentOppfoelgingskontraktListeRequest();
        final Periode periode = new Periode();
        periode.setFom(fom);
        periode.setTom(tom);
        request.setPeriode(periode);
        request.setPersonidentifikator(fnr);
        HentOppfoelgingskontraktListeResponse response = null;
        try {
            response = oppfoelgingPortType.hentOppfoelgingskontraktListe(request);
        } catch (HentOppfoelgingskontraktListeSikkerhetsbegrensning hentOppfoelgingskontraktListeSikkerhetsbegrensning) {
            hentOppfoelgingskontraktListeSikkerhetsbegrensning.printStackTrace();
        }

        return OppfoelgingMapper.tilOppfoelgingskontrakt(response);
    }
}
