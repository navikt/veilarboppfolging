package no.nav.fo.veilarboppfolging.services;

import no.nav.fo.veilarboppfolging.domain.ArenaOppfolging;
import no.nav.fo.veilarboppfolging.mappers.ArenaOppfolgingMapper;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.HentOppfoelgingskontraktListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.Periode;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingskontraktListeRequest;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingskontraktListeResponse;
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.binding.OppfoelgingsstatusV1;
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.informasjon.Person;
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

import static org.slf4j.LoggerFactory.getLogger;

public class ArenaOppfolgingService {

    private static final Logger LOG = getLogger(ArenaOppfolgingService.class);
    private final OppfoelgingPortType oppfoelgingPortType;
    private UnleashService unleashService;
    private OppfoelgingsstatusV1 oppfoelgingsstatusService;
    private OppfoelgingsstatusV2 oppfoelgingsstatusV2Service;
    public static final String UNLEASH_TOGGLE_OPPFOLGING_V2 = "oppfolging.bruk.ny.oppfolgingtjeneste.v2";

    public ArenaOppfolgingService(OppfoelgingsstatusV1 oppfoelgingsstatusService,
                                  OppfoelgingsstatusV2 oppfoelgingsstatusV2Service,
                                  OppfoelgingPortType oppfoelgingPortType,
                                  UnleashService unleashService) {
        this.oppfoelgingsstatusService = oppfoelgingsstatusService;
        this.oppfoelgingsstatusV2Service = oppfoelgingsstatusV2Service;
        this.oppfoelgingPortType = oppfoelgingPortType;
        this.unleashService = unleashService;
    }

    public ArenaOppfolging hentArenaOppfolging(String identifikator) {
        boolean skalBrukeNyArenaOppfolgingTjeneste = unleashService.isEnabled(UNLEASH_TOGGLE_OPPFOLGING_V2);
        if (skalBrukeNyArenaOppfolgingTjeneste) {
            return getArenaOppfolgingsstatusV2(identifikator);
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
        no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.meldinger.HentOppfoelgingsstatusRequest request =
                new no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.meldinger.HentOppfoelgingsstatusRequest();

        Person person = new Person();
        person.setIdent(identifikator);
        request.setBruker(person);

        try {
            no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.meldinger.HentOppfoelgingsstatusResponse oppfoelgingsstatus =
                    oppfoelgingsstatusService.hentOppfoelgingsstatus(request);

            return ArenaOppfolgingMapper.mapTilArenaOppfolgingsstatus(oppfoelgingsstatus);
        } catch (no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.binding.HentOppfoelgingsstatusSikkerhetsbegrensning e) {
            String logMessage = "Ikke tilgang til bruker " + identifikator;
            LOG.warn(logMessage, e);
            throw new ForbiddenException(logMessage, e);
        } catch (no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.binding.HentOppfoelgingsstatusUgyldigInput e) {
            String logMessage = "Ugyldig bruker identifikator: " + identifikator;
            LOG.warn(logMessage, e);
            throw new BadRequestException(logMessage, e);
        } catch (no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.binding.HentOppfoelgingsstatusPersonIkkeFunnet e) {
            String logMessage = "Fant ikke bruker: " + identifikator;
            throw new NotFoundException(logMessage, e);
        }
    }

    private ArenaOppfolging getArenaOppfolgingsstatusV2(String identifikator) {
        no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v2.informasjon.Person person = new no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v2.informasjon.Person();
        person.setIdent(identifikator);

        HentOppfoelgingsstatusRequest request = new HentOppfoelgingsstatusRequest();
        request.setBruker(person);
        try {
            HentOppfoelgingsstatusResponse hentOppfoelgingsstatusResponse = oppfoelgingsstatusV2Service.hentOppfoelgingsstatus(request);
            return ArenaOppfolgingMapper.mapTilArenaOppfolgingsstatusV2(hentOppfoelgingsstatusResponse);
        } catch (HentOppfoelgingsstatusPersonIkkeFunnet e) {
            String logMessage = "Fant ikke bruker: " + identifikator;
            throw new NotFoundException(logMessage, e);
        } catch (HentOppfoelgingsstatusSikkerhetsbegrensning e) {
            String logMessage = "Ikke tilgang til bruker " + identifikator;
            LOG.warn(logMessage, e);
            throw new ForbiddenException(logMessage, e);
        } catch (HentOppfoelgingsstatusUgyldigInput e) {
            String logMessage = "Ugyldig bruker identifikator: " + identifikator;
            LOG.warn(logMessage, e);
            throw new BadRequestException(logMessage, e);
        }
    }

}
