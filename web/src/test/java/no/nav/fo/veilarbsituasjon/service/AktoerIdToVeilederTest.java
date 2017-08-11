package no.nav.fo.veilarbsituasjon.service;


import no.nav.fo.veilarbsituasjon.db.BrukerRepository;
import no.nav.fo.veilarbsituasjon.rest.PortefoljeRessurs;
import no.nav.fo.veilarbsituasjon.rest.domain.VeilederTilordning;
import no.nav.fo.veilarbsituasjon.services.AktoerIdService;
import no.nav.fo.veilarbsituasjon.services.PepClient;
import no.nav.fo.veilarbsituasjon.services.TilordningService;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletResponse;
import java.util.Collections;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class AktoerIdToVeilederTest {

    @Mock
    private BrukerRepository brukerRepository;

    @Mock
    private TilordningService tilordningService;

    @Mock
    private AktoerIdService aktoerIdService;

    @InjectMocks
    PortefoljeRessurs portefoljeRessurs;

    @Mock
    HttpServletResponse httpServletResponse;

    @Mock
    PepClient pepClient;

    @Test
    public void portefoljeRessursMustCallDAOwithAktoerIdToVeileder() throws PepException {
        when(pepClient.isServiceCallAllowed(anyString())).thenReturn(true);
        when(aktoerIdService.findAktoerId(any(String.class))).thenReturn("AKTOERID");
        portefoljeRessurs.postVeilederTilordninger(Collections.singletonList(testData()));
        verify(tilordningService, times(1)).skrivTilDataBaseOgLeggPaaKo(anyString(), anyString());
    }

    @Test
    public void noCallToDAOWhenAktoerIdServiceFails() {
        when(aktoerIdService.findAktoerId(any(String.class))).thenThrow(new IndexOutOfBoundsException());
        portefoljeRessurs.postVeilederTilordninger(Collections.singletonList(testData()));
        verify(tilordningService, never()).skrivTilDataBaseOgLeggPaaKo(anyString(), anyString());
    }

    private VeilederTilordning testData() {
        return new VeilederTilordning()
                .setFraVeilederId(null)
                .setTilVeilederId("***REMOVED***")
                .setBrukerFnr("12345678912");
    }
}