package no.nav.fo.veilarbsituasjon.services;

import no.nav.fo.veilarbsituasjon.domain.Oppfolgingsstatus;
import no.nav.fo.veilarbsituasjon.rest.domain.OppfolgingskontraktResponse;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.HentOppfoelgingsstatusPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.HentOppfoelgingsstatusSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.HentOppfoelgingsstatusUgyldigInput;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.WSBruker;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.WSOppfoelgingskontrakt;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingskontraktListeRequest;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingskontraktListeResponse;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingsstatusResponse;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;

import static no.nav.fo.veilarbsituasjon.utils.CalendarConverter.convertDateToXMLGregorianCalendar;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OppfolgingServiceTest {

    @InjectMocks
    private OppfolgingService oppfolgingService;

    @Mock
    private OppfoelgingPortType oppfoelgingPortType;

    @Test
    public void hentOppfoelgingskontraktListeReturnererEnRespons() throws Exception {
        final XMLGregorianCalendar fom = convertDateToXMLGregorianCalendar(LocalDate.now().minusMonths(2));
        final XMLGregorianCalendar tom = convertDateToXMLGregorianCalendar(LocalDate.now().plusMonths(1));
        WSHentOppfoelgingskontraktListeResponse withOppfoelgingskontraktListe = new WSHentOppfoelgingskontraktListeResponse();
        WSOppfoelgingskontrakt oppfoelgingskontrakt = new WSOppfoelgingskontrakt();
        oppfoelgingskontrakt.setGjelderBruker(new WSBruker());
        withOppfoelgingskontraktListe.getOppfoelgingskontraktListe().add(oppfoelgingskontrakt);
        when(oppfoelgingPortType.hentOppfoelgingskontraktListe(any(WSHentOppfoelgingskontraktListeRequest.class))).thenReturn(withOppfoelgingskontraktListe);

        final OppfolgingskontraktResponse response = oppfolgingService.hentOppfolgingskontraktListe(fom, tom, "***REMOVED***");

        assertThat(response.getOppfoelgingskontrakter().isEmpty(), is(false));
    }

    @Test
    public void skalMappeTilOppfolgingsstatus() throws Exception {
        when(oppfoelgingPortType.hentOppfoelgingsstatus(any())).thenReturn(
                new WSHentOppfoelgingsstatusResponse()
                        .withFormidlingsgruppeKode("formidlingsgruppe")
                        .withNavOppfoelgingsenhet("oppfolgingsenhet")
                        .withRettighetsgruppeKode("rettighetsgruppe")
                        .withServicegruppeKode("servicegruppe")
        );

        Oppfolgingsstatus oppfolgingsstatus = oppfolgingService.hentOppfolgingsstatus("1234");
        Assertions.assertThat(oppfolgingsstatus.getFormidlingsgruppe()).isEqualTo("formidlingsgruppe");
        Assertions.assertThat(oppfolgingsstatus.getOppfolgingsenhet()).isEqualTo("oppfolgingsenhet");
        Assertions.assertThat(oppfolgingsstatus.getRettighetsgruppe()).isEqualTo("rettighetsgruppe");
        Assertions.assertThat(oppfolgingsstatus.getServicegruppe()).isEqualTo("servicegruppe");
    }

    @Test(expected = NotFoundException.class)
    public void skalKasteNotFoundOmPersonIkkeFunnet() throws Exception {
        when(oppfoelgingPortType.hentOppfoelgingsstatus(any())).thenThrow(new HentOppfoelgingsstatusPersonIkkeFunnet());
        oppfolgingService.hentOppfolgingsstatus("1234");
    }

    @Test(expected = ForbiddenException.class)
    public void skalKasteForbiddenOmManIkkeHarTilgang() throws Exception {
        when(oppfoelgingPortType.hentOppfoelgingsstatus(any())).thenThrow(new HentOppfoelgingsstatusSikkerhetsbegrensning());
        oppfolgingService.hentOppfolgingsstatus("1234");
    }

    @Test(expected = BadRequestException.class)
    public void skalKasteBadRequestOmUgyldigIdentifikator() throws Exception {
        when(oppfoelgingPortType.hentOppfoelgingsstatus(any())).thenThrow(new HentOppfoelgingsstatusUgyldigInput());
        oppfolgingService.hentOppfolgingsstatus("1234");
    }

}