package no.nav.veilarboppfolging.client.oppfolging;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.common.cxf.CXFClient;
import no.nav.common.cxf.StsConfig;
import no.nav.common.health.HealthCheckResult;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.HentOppfoelgingskontraktListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.Periode;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingskontraktListeRequest;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingskontraktListeResponse;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingsstatusRequest;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.List;

/**
 * Klient for å hente oppfølgingsinformasjon fra Arena.
 */
@Slf4j
public class OppfolgingClientImpl implements OppfolgingClient {

    private final OppfoelgingPortType oppfoelgingPortType;

    private final OppfoelgingPortType oppfoelgingPortTypePing;

    public OppfolgingClientImpl(String virksomhetOppfolgingV1Endpoint, StsConfig stsConfig) {
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
    public List<OppfolgingskontraktData> hentOppfolgingskontraktListe(XMLGregorianCalendar fom, XMLGregorianCalendar tom, String fnr) {
        HentOppfoelgingskontraktListeRequest request = new HentOppfoelgingskontraktListeRequest();
        final Periode periode = new Periode();
        periode.setFom(fom);
        periode.setTom(tom);
        request.setPeriode(periode);
        request.setPersonidentifikator(fnr);

        try {
            HentOppfoelgingskontraktListeResponse response = oppfoelgingPortType.hentOppfoelgingskontraktListe(request);
            return OppfolgingMapper.tilOppfolgingskontrakt(response);
        } catch (HentOppfoelgingskontraktListeSikkerhetsbegrensning hentOppfoelgingskontraktListeSikkerhetsbegrensning) {
            log.warn("Veileder har ikke tilgang til å søke opp bruker", hentOppfoelgingskontraktListeSikkerhetsbegrensning);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    @SneakyThrows
    @Override
    public String finnEnhetId(String fnr) {
        val req = new HentOppfoelgingsstatusRequest();
        req.setPersonidentifikator(fnr);
        val res = oppfoelgingPortType.hentOppfoelgingsstatus(req);
        return res.getNavOppfoelgingsenhet();
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
