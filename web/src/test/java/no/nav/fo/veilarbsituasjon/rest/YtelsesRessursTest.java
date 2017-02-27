package no.nav.fo.veilarbsituasjon.rest;

import no.nav.fo.veilarbsituasjon.rest.domain.*;
import no.nav.fo.veilarbsituasjon.services.OppfoelgingService;
import no.nav.fo.veilarbsituasjon.services.YtelseskontraktService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class YtelsesRessursTest {

    @InjectMocks
    private YtelseRessurs ytelseRessurs;

    @Mock
    private YtelseskontraktService ytelseskontraktService;

    @Mock
    private OppfoelgingService oppfoelgingService;

    @Test
    public void getOppfoelgingSkalReturnereEnRespons() throws Exception {

        when(ytelseskontraktService.hentYtelseskontraktListe(any(), any(), anyString())).thenReturn(
                new YtelseskontraktResponse(singletonList(new Vedtak()), singletonList(new Ytelseskontrakt()))
        );
        when(oppfoelgingService.hentOppfoelgingskontraktListe(any(), any(), anyString())).thenReturn(
                new OppfoelgingskontraktResponse(singletonList(new OppfoelgingskontraktData()))
        );

        final YtelserResponse ytelser = ytelseRessurs.getYtelser("***REMOVED***");

        assertThat(ytelser.getOppfoelgingskontrakter().isEmpty(), is(false));
        assertThat(ytelser.getVedtaksliste().isEmpty(), is(false));
        assertThat(ytelser.getYtelser().isEmpty(), is(false));
    }
}
