package no.nav.fo.veilarboppfolging.services;


import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.HentYtelseskontraktListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.YtelseskontraktV3;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.informasjon.ytelseskontrakt.WSPeriode;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.WSHentYtelseskontraktListeRequest;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.WSHentYtelseskontraktListeResponse;
import org.slf4j.Logger;

import javax.ws.rs.ForbiddenException;
import javax.xml.datatype.XMLGregorianCalendar;

import static org.slf4j.LoggerFactory.getLogger;

public class YtelseskontraktService {

    private static final Logger LOG = getLogger(YtelseskontraktService.class);

    private final YtelseskontraktV3 ytelseskontraktV3;

    public YtelseskontraktService(YtelseskontraktV3 ytelseskontraktV3) {
        this.ytelseskontraktV3 = ytelseskontraktV3;
    }

    public WSHentYtelseskontraktListeResponse hentYtelseskontraktListe(XMLGregorianCalendar periodeFom, XMLGregorianCalendar periodeTom, String personId) {
        final WSPeriode periode = new WSPeriode();
        periode.setFom(periodeFom);
        periode.setTom(periodeTom);
        WSHentYtelseskontraktListeRequest request = new WSHentYtelseskontraktListeRequest()
                .withPeriode(periode)
                .withPersonidentifikator(personId);
        try {
            LOG.info("Sender request til Ytelseskontrakt_v3");
            final WSHentYtelseskontraktListeResponse response = ytelseskontraktV3.hentYtelseskontraktListe(request);

            return response;

        } catch (HentYtelseskontraktListeSikkerhetsbegrensning hentYtelseskontraktListeSikkerhetsbegrensning) {
            String logMessage = "Veileder har ikke tilgang til å søke opp " + personId;
            LOG.warn(logMessage, hentYtelseskontraktListeSikkerhetsbegrensning);
            throw new ForbiddenException(logMessage, hentYtelseskontraktListeSikkerhetsbegrensning);
        }
    }

}