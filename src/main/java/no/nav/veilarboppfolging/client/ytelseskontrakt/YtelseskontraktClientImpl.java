package no.nav.veilarboppfolging.client.ytelseskontrakt;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.cxf.CXFClient;
import no.nav.common.cxf.StsConfig;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.types.identer.Fnr;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.HentYtelseskontraktListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.YtelseskontraktV3;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.informasjon.ytelseskontrakt.Periode;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.HentYtelseskontraktListeRequest;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.HentYtelseskontraktListeResponse;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Klient for å hente ytelser fra Arena
 */
@Slf4j
public class YtelseskontraktClientImpl implements YtelseskontraktClient {

    private final YtelseskontraktV3 ytelseskontrakt;

    public YtelseskontraktClientImpl(String ytelseskontraktV3Endpoint, StsConfig stsConfig) {
        ytelseskontrakt = new CXFClient<>(YtelseskontraktV3.class)
                .configureStsForSystemUser(stsConfig)
                .address(ytelseskontraktV3Endpoint)
                .build();
    }

    @Override
    public YtelseskontraktResponse hentYtelseskontraktListe(XMLGregorianCalendar periodeFom, XMLGregorianCalendar periodeTom, Fnr personId) {
        final Periode periode = new Periode();
        periode.setFom(periodeFom);
        periode.setTom(periodeTom);
        HentYtelseskontraktListeRequest request = new HentYtelseskontraktListeRequest();
        request.setPeriode(periode);
        request.setPersonidentifikator(personId.get());

        return hentYtelseskontraktListe(request);
    }

    @Override
    public YtelseskontraktResponse hentYtelseskontraktListe(Fnr personId) {
        HentYtelseskontraktListeRequest request = new HentYtelseskontraktListeRequest();
        request.setPersonidentifikator(personId.get());

        return hentYtelseskontraktListe(request);
    }

    private YtelseskontraktResponse hentYtelseskontraktListe(HentYtelseskontraktListeRequest request) {
        try {
            HentYtelseskontraktListeResponse response = ytelseskontrakt.hentYtelseskontraktListe(request);
            return YtelseskontraktMapper.tilYtelseskontrakt(response);
        } catch (HentYtelseskontraktListeSikkerhetsbegrensning hentYtelseskontraktListeSikkerhetsbegrensning) {
            log.error("Systembruker har ikke tilgang til å søke opp bruker", hentYtelseskontraktListeSikkerhetsbegrensning);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public HealthCheckResult checkHealth() {
        try {
            ytelseskontrakt.ping();
            return HealthCheckResult.healthy();
        } catch (Exception e) {
            log.warn("Feil ved ping av YtelseskontraktV3", e);
            return HealthCheckResult.unhealthy(e);
        }
    }

}
