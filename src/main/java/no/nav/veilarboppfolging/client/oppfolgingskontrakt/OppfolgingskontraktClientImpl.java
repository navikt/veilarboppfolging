package no.nav.veilarboppfolging.client.oppfolgingskontrakt;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.cxf.CXFClient;
import no.nav.common.cxf.StsConfig;
import no.nav.common.health.HealthCheckResult;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.HentOppfoelgingskontraktListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.Periode;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingskontraktListeRequest;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingskontraktListeResponse;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.HentYtelseskontraktListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.YtelseskontraktV3;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.informasjon.ytelseskontrakt.WSPeriode;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.WSHentYtelseskontraktListeRequest;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.WSHentYtelseskontraktListeResponse;
import org.apache.cxf.interceptor.LoggingOutInterceptor;

import javax.ws.rs.ForbiddenException;
import javax.xml.datatype.XMLGregorianCalendar;

import static no.nav.veilarboppfolging.config.ApplicationConfig.VIRKSOMHET_OPPFOLGING_V1_PROPERTY;

/**
 * Klient for å hente oppfølgingskontrakt fra Arena.
 */
@Slf4j
public class OppfolgingskontraktClientImpl implements OppfolgingskontraktClient {

    private final OppfoelgingPortType oppfoelgingPortType;

    private final OppfoelgingPortType oppfoelgingPortTypePing;

    public OppfolgingskontraktClientImpl(String virksomhetOppfolgingV1Endpoint, StsConfig stsConfig) {
        oppfoelgingPortType = new CXFClient<>(OppfoelgingPortType.class)
                .withOutInterceptor(new LoggingOutInterceptor())
                .configureStsForSubject(stsConfig)
                .address(virksomhetOppfolgingV1Endpoint)
                .build();

        oppfoelgingPortTypePing = new CXFClient<>(OppfoelgingPortType.class)
                .withOutInterceptor(new LoggingOutInterceptor())
                .configureStsForSystemUser(stsConfig)
                .address(virksomhetOppfolgingV1Endpoint)
                .build();
    }

    @Override
    public HentOppfoelgingskontraktListeResponse hentOppfolgingskontraktListe(XMLGregorianCalendar fom, XMLGregorianCalendar tom, String fnr) {
        HentOppfoelgingskontraktListeRequest request = new HentOppfoelgingskontraktListeRequest();
        final Periode periode = new Periode();
        periode.setFom(fom);
        periode.setTom(tom);
        request.setPeriode(periode);
        request.setPersonidentifikator(fnr);
        HentOppfoelgingskontraktListeResponse response;

        try {
            response = oppfoelgingPortType.hentOppfoelgingskontraktListe(request);
        } catch (HentOppfoelgingskontraktListeSikkerhetsbegrensning hentOppfoelgingskontraktListeSikkerhetsbegrensning) {
            String logMessage = "Veileder har ikke tilgang til å søke opp bruker";
            log.warn(logMessage, hentOppfoelgingskontraktListeSikkerhetsbegrensning);
            throw new ForbiddenException(logMessage, hentOppfoelgingskontraktListeSikkerhetsbegrensning);
        }

        return response;
    }

    @Override
    public HealthCheckResult checkHealth() {
        try {
            oppfoelgingPortTypePing.ping();
            return HealthCheckResult.healthy();
        } catch (Exception e) {
            return HealthCheckResult.unhealthy(e);
        }
    }

}
