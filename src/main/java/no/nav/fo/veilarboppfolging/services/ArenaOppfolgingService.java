package no.nav.fo.veilarboppfolging.services;

import io.micrometer.core.instrument.Counter;
import no.nav.fo.veilarboppfolging.domain.ArenaOppfolging;
import no.nav.fo.veilarboppfolging.mappers.ArenaOppfolgingMapper;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.HentOppfoelgingskontraktListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.Periode;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingskontraktListeRequest;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingskontraktListeResponse;
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v2.binding.HentOppfoelgingsstatusPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v2.binding.HentOppfoelgingsstatusSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v2.binding.HentOppfoelgingsstatusUgyldigInput;
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v2.binding.OppfoelgingsstatusV2;
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v2.meldinger.HentOppfoelgingsstatusRequest;
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v2.meldinger.HentOppfoelgingsstatusResponse;
import org.slf4j.Logger;


import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.xml.datatype.XMLGregorianCalendar;

import static no.nav.metrics.MetricsFactory.getMeterRegistry;
import static org.slf4j.LoggerFactory.getLogger;

public class ArenaOppfolgingService {

    private static final Logger LOG = getLogger(ArenaOppfolgingService.class);
    private final OppfoelgingPortType oppfoelgingPortType;
    private final UnleashService unleash;
    private final ArenaNightKingService arenaNightKingService;
    private OppfoelgingsstatusV2 oppfoelgingsstatusV2Service;
    private Counter counter;

    public ArenaOppfolgingService(OppfoelgingsstatusV2 oppfoelgingsstatusV2Service,
                                  OppfoelgingPortType oppfoelgingPortType,
                                  UnleashService unleash,
                                  ArenaNightKingService arenaNightKingService) {
        this.oppfoelgingsstatusV2Service = oppfoelgingsstatusV2Service;
        this.oppfoelgingPortType = oppfoelgingPortType;
        this.unleash = unleash;
        this.arenaNightKingService = arenaNightKingService;
        counter = Counter.builder("veilarboppfolging.kall_mot_arena_oppfolging").register(getMeterRegistry());
    }

    public ArenaOppfolging hentArenaOppfolging(String identifikator) {
        if(unleash.isEnabled("veilarboppfolging.oppfolging.fra.nightking")) {
            return arenaNightKingService.hentArenaOppfolging(identifikator);
        } else {
            return getArenaOppfolgingsstatus(identifikator);
        }
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

    private ArenaOppfolging getArenaOppfolgingsstatus(String identifikator) {

        counter.increment();

        no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v2.informasjon.Person person = new no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v2.informasjon.Person();
        person.setIdent(identifikator);

        HentOppfoelgingsstatusRequest request = new HentOppfoelgingsstatusRequest();
        request.setBruker(person);
        try {
            HentOppfoelgingsstatusResponse hentOppfoelgingsstatusResponse = oppfoelgingsstatusV2Service.hentOppfoelgingsstatus(request);
            return ArenaOppfolgingMapper.mapTilArenaOppfolgingsstatusV2(hentOppfoelgingsstatusResponse);
        } catch (java.lang.reflect.UndeclaredThrowableException e) {
            Throwable undeclared = e.getUndeclaredThrowable();
            throw undeclared != null  && undeclared.getCause() instanceof HentOppfoelgingsstatusPersonIkkeFunnet 
                    ? notFound(identifikator, undeclared.getCause()) 
                    : e;
        } catch (HentOppfoelgingsstatusPersonIkkeFunnet e) {
            throw notFound(identifikator, e);
        } catch (HentOppfoelgingsstatusSikkerhetsbegrensning e) {
            throw new ForbiddenException("Ikke tilgang til bruker " + identifikator, e);
        } catch (HentOppfoelgingsstatusUgyldigInput e) {
            throw new BadRequestException("Ugyldig bruker identifikator: " + identifikator, e);
        }
    }

    private NotFoundException notFound(String identifikator, Throwable t) {
        return new NotFoundException("Fant ikke bruker: " + identifikator, t);
    }

}
