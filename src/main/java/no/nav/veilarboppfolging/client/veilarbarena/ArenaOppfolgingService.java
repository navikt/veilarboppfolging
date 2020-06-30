package no.nav.veilarboppfolging.client.veilarbarena;

import io.micrometer.core.instrument.Counter;
import no.nav.veilarboppfolging.domain.ArenaOppfolging;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.HentOppfoelgingskontraktListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.Periode;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingskontraktListeRequest;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingskontraktListeResponse;
import org.slf4j.Logger;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.client.Client;
import javax.xml.datatype.XMLGregorianCalendar;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.apiapp.util.UrlUtils.clusterUrlForApplication;
import static no.nav.apiapp.util.UrlUtils.joinPaths;
import static no.nav.veilarboppfolging.config.ApplicationConfig.VEILARBARENAAPI_URL_PROPERTY;
import static no.nav.metrics.MetricsFactory.getMeterRegistry;
import static no.nav.sbl.util.EnvironmentUtils.getOptionalProperty;
import static org.slf4j.LoggerFactory.getLogger;

public class ArenaOppfolgingService {

    private static final Logger LOG = getLogger(ArenaOppfolgingService.class);
    private final OppfoelgingPortType oppfoelgingPortType;
    private final Client restClient;
    private final String host;
    private Counter counter;

    public ArenaOppfolgingService(OppfoelgingPortType oppfoelgingPortType,
                                  Client restClient) {
        this.oppfoelgingPortType = oppfoelgingPortType;
        this.restClient = restClient;
        this.host = getOptionalProperty(VEILARBARENAAPI_URL_PROPERTY).orElseGet(() ->
                joinPaths(clusterUrlForApplication("veilarbarena"), "veilarbarena", "api"));
        counter = Counter.builder("veilarboppfolging.kall_mot_arena_oppfolging").register(getMeterRegistry());
    }

    public ArenaOppfolging hentArenaOppfolging(String identifikator) {
        counter.increment();
        return getArenaOppfolgingsstatus(identifikator);
    }

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
            LOG.warn(logMessage, hentOppfoelgingskontraktListeSikkerhetsbegrensning);
            throw new ForbiddenException(logMessage, hentOppfoelgingskontraktListeSikkerhetsbegrensning);
        }

        return response;
    }

    private ArenaOppfolging getArenaOppfolgingsstatus(String fnr) {
        return
                restClient.target(String.format("%s/oppfolgingsstatus/%s", host, fnr))
                        .request()
                        .header(ACCEPT, APPLICATION_JSON)
                        .get(ArenaOppfolging.class);
    }
}
