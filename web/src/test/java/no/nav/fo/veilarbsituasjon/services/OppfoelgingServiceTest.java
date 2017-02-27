package no.nav.fo.veilarbsituasjon.services;

import no.nav.fo.veilarbsituasjon.rest.domain.OppfoelgingskontraktResponse;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.Bruker;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.Oppfoelgingskontrakt;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingskontraktListeRequest;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingskontraktListeResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;

import static no.nav.fo.veilarbsituasjon.utils.CalendarConverter.convertDateToXMLGregorianCalendar;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OppfoelgingServiceTest {

    @InjectMocks
    private OppfoelgingService oppfoelgingService;

    @Mock
    private OppfoelgingPortType oppfoelgingPortType;

    @Test
    public void hentOppfoelgingskontraktListeReturnererEnRespons() throws Exception {
        final XMLGregorianCalendar fom = convertDateToXMLGregorianCalendar(LocalDate.now().minusMonths(2));
        final XMLGregorianCalendar tom = convertDateToXMLGregorianCalendar(LocalDate.now().plusMonths(1));
        HentOppfoelgingskontraktListeResponse withOppfoelgingskontraktListe = new HentOppfoelgingskontraktListeResponse();
        Oppfoelgingskontrakt oppfoelgingskontrakt = new Oppfoelgingskontrakt();
        oppfoelgingskontrakt.setGjelderBruker(new Bruker());
        withOppfoelgingskontraktListe.getOppfoelgingskontraktListe().add(oppfoelgingskontrakt);
        when(oppfoelgingPortType.hentOppfoelgingskontraktListe(any(HentOppfoelgingskontraktListeRequest.class))).thenReturn(withOppfoelgingskontraktListe);

        final OppfoelgingskontraktResponse response = oppfoelgingService.hentOppfoelgingskontraktListe(fom, tom, "***REMOVED***");

        assertThat(response.getOppfoelgingskontrakter().isEmpty(), is(false));
    }

}