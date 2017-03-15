package no.nav.fo.veilarbsituasjon.rest;

import no.nav.fo.veilarbsituasjon.rest.domain.OppfolgingskontraktData;
import no.nav.fo.veilarbsituasjon.rest.domain.OppfolgingskontraktResponse;
import no.nav.fo.veilarbsituasjon.services.OppfolgingService;
import no.nav.fo.veilarbsituasjon.services.PepClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OppfolgingRessursTest {

    @InjectMocks
    private OppfolgingRessurs oppfoelgingRessurs;

    @Mock
    private OppfolgingService oppfolgingService;

    @Mock
    private PepClient pepClient;

    @Test
    public void getOppfoelgingSkalReturnereEnRespons() throws Exception {

        when(oppfolgingService.hentOppfolgingskontraktListe(any(), any(), anyString())).thenReturn(
                new OppfolgingskontraktResponse(Collections.singletonList(new OppfolgingskontraktData()))
        );

        when(pepClient.isServiceCallAllowed(anyString())).thenReturn(true);

        final OppfolgingskontraktResponse oppfoelging = oppfoelgingRessurs.getOppfoelging("***REMOVED***");

        assertThat(oppfoelging.getOppfoelgingskontrakter().isEmpty(), is(false));
    }
}
