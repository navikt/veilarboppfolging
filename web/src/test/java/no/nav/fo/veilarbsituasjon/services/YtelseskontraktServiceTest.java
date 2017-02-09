package no.nav.fo.veilarbsituasjon.services;

import no.nav.fo.veilarbsituasjon.rest.domain.YtelseskontraktResponse;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.YtelseskontraktV3;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.informasjon.ytelseskontrakt.WSVedtak;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.informasjon.ytelseskontrakt.WSYtelseskontrakt;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.WSHentYtelseskontraktListeRequest;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.WSHentYtelseskontraktListeResponse;
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
public class YtelseskontraktServiceTest {

    @InjectMocks
    private YtelseskontraktService ytelseskontraktService;

    @Mock
    private YtelseskontraktV3 ytelseskontraktV3;

    @Test
    public void hentYtelseskontraktListeReturnererEnRespons() throws Exception {

        final XMLGregorianCalendar fom = convertDateToXMLGregorianCalendar(LocalDate.now().minusMonths(2));
        final XMLGregorianCalendar tom = convertDateToXMLGregorianCalendar(LocalDate.now().plusMonths(1));
        when(ytelseskontraktV3.hentYtelseskontraktListe(any(WSHentYtelseskontraktListeRequest.class))).thenReturn(
                new WSHentYtelseskontraktListeResponse().withYtelseskontraktListe(
                        new WSYtelseskontrakt().withIhtVedtak(new WSVedtak()).withDatoKravMottatt(tom),
                        new WSYtelseskontrakt().withDatoKravMottatt(tom)
                )
        );

        final YtelseskontraktResponse response = ytelseskontraktService.hentYtelseskontraktListe(fom, tom, "***REMOVED***");

        assertThat(response.getVedtaksliste().isEmpty(), is(false));
        assertThat(response.getYtelser().isEmpty(), is(false));
    }
}
