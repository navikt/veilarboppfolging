package no.nav.fo.veilarbsituasjon.rest;

import no.nav.fo.veilarbsituasjon.rest.domain.Oppfoelgingskontrakt;
import no.nav.fo.veilarbsituasjon.rest.domain.OppfoelgingskontraktResponse;
import no.nav.fo.veilarbsituasjon.services.OppfoelgingService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OppfoelgingRessursTest {

    @InjectMocks
    private OppfoelgingRessurs oppfoelgingRessurs;

    @Mock
    private OppfoelgingService oppfoelgingService;

    @Test
    public void getOppfoelgingSkalReturnereEnRespons() throws Exception {

        when(oppfoelgingService.hentOppfoelgingskontraktListe(any(), any(), anyString())).thenReturn(
                new OppfoelgingskontraktResponse(Collections.singletonList(new Oppfoelgingskontrakt()))
        );

        final OppfoelgingskontraktResponse oppfoelging = oppfoelgingRessurs.getOppfoelging("***REMOVED***");

        assertThat(oppfoelging.getOppfoelgingskontrakter().isEmpty(), is(false));
    }
}
