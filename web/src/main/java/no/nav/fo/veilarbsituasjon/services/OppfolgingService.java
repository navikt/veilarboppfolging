package no.nav.fo.veilarbsituasjon.services;

import no.nav.fo.veilarbsituasjon.rest.domain.*;
import no.nav.fo.veilarbsituasjon.mappers.OppfolgingMapper;
import no.nav.fo.veilarbsituasjon.rest.domain.Oppfolgingsenhet;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.*;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.WSPeriode;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.*;
import org.slf4j.Logger;

import javax.ws.rs.*;
import javax.xml.datatype.XMLGregorianCalendar;

import static no.nav.fo.veilarbsituasjon.mappers.OppfolgingsstatusMapper.tilOppfolgingsstatus;
import static org.slf4j.LoggerFactory.getLogger;

public class OppfolgingService {

    private static final Logger LOG = getLogger(OppfolgingService.class);
    private final OppfoelgingPortType oppfoelgingPortType;
    private OrganisasjonsenhetService organisasjonsenhetService;

    public OppfolgingService(OppfoelgingPortType oppfoelgingPortType, OrganisasjonsenhetService organisasjonsenhetService) {
        this.oppfoelgingPortType = oppfoelgingPortType;
        this.organisasjonsenhetService = organisasjonsenhetService;
    }

    public OppfolgingskontraktResponse hentOppfolgingskontraktListe(XMLGregorianCalendar fom, XMLGregorianCalendar tom, String fnr) {
        WSHentOppfoelgingskontraktListeRequest request = new WSHentOppfoelgingskontraktListeRequest();
        final WSPeriode periode = new WSPeriode().withFom(fom).withTom(tom);
        request.withPeriode(periode).withPersonidentifikator(fnr);
        WSHentOppfoelgingskontraktListeResponse response;

        try {
            response = oppfoelgingPortType.hentOppfoelgingskontraktListe(request);
        } catch (HentOppfoelgingskontraktListeSikkerhetsbegrensning hentOppfoelgingskontraktListeSikkerhetsbegrensning) {
            String logMessage = "Veileder har ikke tilgang til å søke opp " + fnr;
            LOG.warn(logMessage, hentOppfoelgingskontraktListeSikkerhetsbegrensning);
            throw new ForbiddenException(logMessage, hentOppfoelgingskontraktListeSikkerhetsbegrensning);
        }

        return OppfolgingMapper.tilOppfolgingskontrakt(response);
    }

    public Oppfolgingsstatus hentOppfolgingsstatus(String identifikator) {
        WSHentOppfoelgingsstatusRequest request = new WSHentOppfoelgingsstatusRequest()
                .withPersonidentifikator(identifikator);

        try {
            WSHentOppfoelgingsstatusResponse oppfoelgingsstatus = oppfoelgingPortType.hentOppfoelgingsstatus(request);
            Oppfolgingsenhet oppfolgingsenhet = organisasjonsenhetService
                    .hentEnhet(oppfoelgingsstatus.getNavOppfoelgingsenhet());
            return tilOppfolgingsstatus(oppfoelgingsstatus, oppfolgingsenhet);
        } catch (HentOppfoelgingsstatusSikkerhetsbegrensning e) {
            String logMessage = "Ikke tilgang til bruker " + identifikator;
            LOG.warn(logMessage, e);
            throw new ForbiddenException(logMessage, e);
        } catch (HentOppfoelgingsstatusUgyldigInput e) {
            String logMessage = "Ugyldig bruker identifikator: " + identifikator;
            LOG.warn(logMessage, e);
            throw new BadRequestException(logMessage, e);
        } catch (HentOppfoelgingsstatusPersonIkkeFunnet e) {
            String logMessage = "Fant ikke bruker: " + identifikator;
            LOG.debug(logMessage, e);
            throw new NotFoundException(logMessage, e);
        }
    }
}
