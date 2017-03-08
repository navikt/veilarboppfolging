package no.nav.fo.veilarbsituasjon.services;

import no.nav.fo.veilarbsituasjon.domain.Oppfolgingsstatus;
import no.nav.fo.veilarbsituasjon.mappers.OppfolgingMapper;
import no.nav.fo.veilarbsituasjon.rest.domain.OppfolgingskontraktResponse;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.*;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.WSPeriode;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingskontraktListeRequest;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingskontraktListeResponse;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingsstatusRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.xml.datatype.XMLGregorianCalendar;

import static no.nav.fo.veilarbsituasjon.mappers.OppfolgingsstatusMapper.tilOppfolgingsstatus;

public class OppfolgingService {
    private final OppfoelgingPortType oppfoelgingPortType;
    private Logger logger = LoggerFactory.getLogger(OppfolgingService.class);

    public OppfolgingService(OppfoelgingPortType oppfoelgingPortType) {
        this.oppfoelgingPortType = oppfoelgingPortType;
    }

    public OppfolgingskontraktResponse hentOppfolgingskontraktListe(XMLGregorianCalendar fom, XMLGregorianCalendar tom, String fnr) {
        WSHentOppfoelgingskontraktListeRequest request = new WSHentOppfoelgingskontraktListeRequest();
        final WSPeriode periode = new WSPeriode().withFom(fom).withTom(tom);
        request.withPeriode(periode).withPersonidentifikator(fnr);
        WSHentOppfoelgingskontraktListeResponse response = null;
        try {
            response = oppfoelgingPortType.hentOppfoelgingskontraktListe(request);
        } catch (HentOppfoelgingskontraktListeSikkerhetsbegrensning hentOppfoelgingskontraktListeSikkerhetsbegrensning) {
            hentOppfoelgingskontraktListeSikkerhetsbegrensning.printStackTrace();
        }

        return OppfolgingMapper.tilOppfolgingskontrakt(response);
    }

    public Oppfolgingsstatus hentOppfolgingsstatus(String identifikator) {
        WSHentOppfoelgingsstatusRequest request = new WSHentOppfoelgingsstatusRequest()
                .withPersonidentifikator(identifikator);

        try {
            return tilOppfolgingsstatus(oppfoelgingPortType.hentOppfoelgingsstatus(request));
        } catch (HentOppfoelgingsstatusSikkerhetsbegrensning e) {
            throw new ForbiddenException("Ikke tilgang til ressurs.", e);
        } catch (HentOppfoelgingsstatusUgyldigInput e) {
            throw new BadRequestException("Ugyldig identifikator", e);
        } catch (HentOppfoelgingsstatusPersonIkkeFunnet e) {
            throw new NotFoundException("Fant ikke person", e);
        }
    }
}
