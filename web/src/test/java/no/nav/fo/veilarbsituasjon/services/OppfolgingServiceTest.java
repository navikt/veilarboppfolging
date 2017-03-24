package no.nav.fo.veilarbsituasjon.services;

import no.nav.fo.veilarbsituasjon.rest.domain.*;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.*;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.WSBruker;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.WSOppfoelgingskontrakt;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.*;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.ws.rs.*;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;

import static no.nav.fo.veilarbsituasjon.utils.CalendarConverter.convertDateToXMLGregorianCalendar;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OppfolgingServiceTest {

    private static final String MOCK_ENHET_ID = "1331";
    private static final String MOCK_ENHET_NAVN = "NAV Eidsvoll";

    @InjectMocks
    private OppfolgingService oppfolgingService;

    @Mock
    private OppfoelgingPortType oppfoelgingPortType;

    @Mock
    private OrganisasjonsenhetService organisasjonsenhetService;

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
        when(oppfoelgingPortType.hentOppfoelgingsstatus(any())).thenReturn(lagMockResponse());
        when(organisasjonsenhetService.hentEnhet(any())).thenReturn(new Oppfolgingsenhet()
                .withNavn(MOCK_ENHET_NAVN)
                .withEnhetId(MOCK_ENHET_ID));

        Oppfolgingsstatus oppfolgingsstatus = oppfolgingService.hentOppfolgingsstatus("1234");
        Assertions.assertThat(oppfolgingsstatus.getFormidlingsgruppe()).isEqualTo("formidlingsgruppe");
        Assertions.assertThat(oppfolgingsstatus.getOppfolgingsenhet().getEnhetId()).isEqualTo(MOCK_ENHET_ID);
        Assertions.assertThat(oppfolgingsstatus.getRettighetsgruppe()).isEqualTo("rettighetsgruppe");
        Assertions.assertThat(oppfolgingsstatus.getServicegruppe()).isEqualTo("servicegruppe");
    }

    @Test
    public void skalHenteOrganisasjonsenhetDetaljerFraNorg() throws HentOppfoelgingsstatusUgyldigInput, HentOppfoelgingsstatusPersonIkkeFunnet, HentOppfoelgingsstatusSikkerhetsbegrensning {
        when(oppfoelgingPortType.hentOppfoelgingsstatus(any())).thenReturn(lagMockResponse());
        when(organisasjonsenhetService.hentEnhet(any())).thenReturn(new Oppfolgingsenhet().withNavn(MOCK_ENHET_NAVN));

        Oppfolgingsstatus oppfolgingsstatus = oppfolgingService.hentOppfolgingsstatus("1234");

        Assertions.assertThat(oppfolgingsstatus.getOppfolgingsenhet().getNavn()).isEqualTo(MOCK_ENHET_NAVN);
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

    private WSHentOppfoelgingsstatusResponse lagMockResponse() {
        return new WSHentOppfoelgingsstatusResponse()
                .withFormidlingsgruppeKode("formidlingsgruppe")
                .withNavOppfoelgingsenhet(MOCK_ENHET_ID)
                .withRettighetsgruppeKode("rettighetsgruppe")
                .withServicegruppeKode("servicegruppe");
    }

}