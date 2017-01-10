package no.nav.fo.veilarbsituasjon.services;


import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.HentYtelseskontraktListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.YtelseskontraktV3;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.informasjon.ytelseskontrakt.WSPeriode;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.WSHentYtelseskontraktListeRequest;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.WSHentYtelseskontraktListeResponse;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import javax.xml.datatype.XMLGregorianCalendar;

import static org.slf4j.LoggerFactory.getLogger;

public class YtelseskontraktService {

    private static final Logger LOG = getLogger(YtelseskontraktService.class);


    @Autowired
    private YtelseskontraktV3 ytelseskontraktV3;

    public void hentYtelseskontraktListe(XMLGregorianCalendar periodeFom, XMLGregorianCalendar periodeTom, String personId) {
        final WSPeriode periode = new WSPeriode();
        periode.setFom(periodeFom);
        periode.setTom(periodeTom);
        WSHentYtelseskontraktListeRequest request = new WSHentYtelseskontraktListeRequest()
                .withPeriode(periode)
                .withPersonidentifikator(personId);
        try {
            LOG.info("Sender request til Ytelsekontrakt_v3");
            final WSHentYtelseskontraktListeResponse response = ytelseskontraktV3.hentYtelseskontraktListe(request);
//            response.getYtelseskontraktListe().stream().forEach(WSYtelseskontrakt);
        } catch (HentYtelseskontraktListeSikkerhetsbegrensning hentYtelseskontraktListeSikkerhetsbegrensning) {
            hentYtelseskontraktListeSikkerhetsbegrensning.printStackTrace();
        }
    }
}
