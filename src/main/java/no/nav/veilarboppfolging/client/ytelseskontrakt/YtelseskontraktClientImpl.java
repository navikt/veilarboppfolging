package no.nav.veilarboppfolging.client.ytelseskontrakt;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.cxf.CXFClient;
import no.nav.common.cxf.StsConfig;
import no.nav.common.health.HealthCheckResult;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.HentYtelseskontraktListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.YtelseskontraktV3;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.informasjon.ytelseskontrakt.WSPeriode;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.WSHentYtelseskontraktListeRequest;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.WSHentYtelseskontraktListeResponse;
import org.apache.cxf.interceptor.LoggingOutInterceptor;

import javax.ws.rs.ForbiddenException;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Klient for å hente ytelser fra Arena
 */
@Slf4j
public class YtelseskontraktClientImpl implements YtelseskontraktClient {

    private final YtelseskontraktV3 ytelseskontrakt;

    private final YtelseskontraktV3 ytelseskontraktPing;

    public YtelseskontraktClientImpl(String ytelseskontraktV3Endpoint, StsConfig stsConfig) {
        ytelseskontrakt = new CXFClient<>(YtelseskontraktV3.class)
                .withOutInterceptor(new LoggingOutInterceptor())
                .configureStsForSubject(stsConfig)
                .address(ytelseskontraktV3Endpoint)
                .build();

        ytelseskontraktPing = new CXFClient<>(YtelseskontraktV3.class)
                .withOutInterceptor(new LoggingOutInterceptor())
                .configureStsForSystemUser(stsConfig)
                .address(ytelseskontraktV3Endpoint)
                .build();
    }

    @Override
    public YtelseskontraktResponse hentYtelseskontraktListe(XMLGregorianCalendar periodeFom, XMLGregorianCalendar periodeTom, String personId) {
        final WSPeriode periode = new WSPeriode();
        periode.setFom(periodeFom);
        periode.setTom(periodeTom);
        WSHentYtelseskontraktListeRequest request = new WSHentYtelseskontraktListeRequest()
                .withPeriode(periode)
                .withPersonidentifikator(personId);
        try {
            log.info("Sender request til Ytelseskontrakt_v3");
            WSHentYtelseskontraktListeResponse response = ytelseskontrakt.hentYtelseskontraktListe(request);
            return YtelseskontraktMapper.tilYtelseskontrakt(response);
        } catch (HentYtelseskontraktListeSikkerhetsbegrensning hentYtelseskontraktListeSikkerhetsbegrensning) {
            String logMessage = "Veileder har ikke tilgang til å søke opp " + personId;
            log.warn(logMessage, hentYtelseskontraktListeSikkerhetsbegrensning);
            throw new ForbiddenException(logMessage, hentYtelseskontraktListeSikkerhetsbegrensning);
        }
    }

    @Override
    public HealthCheckResult checkHealth() {
        try {
            ytelseskontraktPing.ping();
            return HealthCheckResult.healthy();
        } catch (Exception e) {
            return HealthCheckResult.unhealthy(e);
        }
    }

}
