package no.nav.fo.veilarbsituasjon.services;

import no.nav.fo.veilarbsituasjon.domain.Oppfolgingsstatus;
import no.nav.fo.veilarbsituasjon.mappers.OppfoelgingMapper;
import no.nav.fo.veilarbsituasjon.rest.domain.OppfoelgingskontraktResponse;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.*;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.WSPeriode;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingskontraktListeRequest;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingskontraktListeResponse;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingsstatusRequest;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingsstatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.xml.datatype.XMLGregorianCalendar;

import static no.nav.fo.veilarbsituasjon.mappers.OppfolgingsstatusMapper.tilOppfolgingsstatus;

public class OppfoelgingService {
    private final OppfoelgingPortType oppfoelgingPortType;
    private Logger logger = LoggerFactory.getLogger(OppfoelgingService.class);

    public OppfoelgingService(OppfoelgingPortType oppfoelgingPortType) {
        this.oppfoelgingPortType = oppfoelgingPortType;
    }

    public OppfoelgingskontraktResponse hentOppfoelgingskontraktListe(XMLGregorianCalendar fom, XMLGregorianCalendar tom, String fnr) {
        WSHentOppfoelgingskontraktListeRequest request = new WSHentOppfoelgingskontraktListeRequest();
        final WSPeriode periode = new WSPeriode().withFom(fom).withTom(tom);
        request.withPeriode(periode).withPersonidentifikator(fnr);
        WSHentOppfoelgingskontraktListeResponse response = null;
        try {
            response = oppfoelgingPortType.hentOppfoelgingskontraktListe(request);
        } catch (HentOppfoelgingskontraktListeSikkerhetsbegrensning hentOppfoelgingskontraktListeSikkerhetsbegrensning) {
            hentOppfoelgingskontraktListeSikkerhetsbegrensning.printStackTrace();
        }

        return OppfoelgingMapper.tilOppfoelgingskontrakt(response);
    }

    public Oppfolgingsstatus hentOppfolgingsstatus(String identifikator) throws ForbiddenException, NotFoundException, BadRequestException {
        WSHentOppfoelgingsstatusRequest request = new WSHentOppfoelgingsstatusRequest()
                .withPersonidentifikator(identifikator);

        try {
            return tilOppfolgingsstatus(oppfoelgingPortType.hentOppfoelgingsstatus(request));
        } catch (HentOppfoelgingsstatusSikkerhetsbegrensning e) {
            logger.error("Bruker har ikke tilgang til å hente oppfølgingsstatus", e);
            throw new ForbiddenException("Ikke tilgang til ressurs.");
        } catch (HentOppfoelgingsstatusUgyldigInput e) {
            logger.error("Ugyldig input ved henting av oppfølgingsstatus", e);
            throw new BadRequestException("Ugyldig identifikator");
        } catch (HentOppfoelgingsstatusPersonIkkeFunnet e) {
            logger.error(String.format("Fant ikke oppfølgingsstatus for identifikator %s", identifikator), e);
            throw new NotFoundException("Fant ikke person");
        }
    }
}
