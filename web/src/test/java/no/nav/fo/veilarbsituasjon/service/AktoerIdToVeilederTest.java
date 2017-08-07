package no.nav.fo.veilarbsituasjon.service;


import no.nav.fo.veilarbsituasjon.db.BrukerRepository;
import no.nav.fo.veilarbsituasjon.domain.OppfolgingBruker;
import no.nav.fo.veilarbsituasjon.rest.PortefoljeRessurs;
import no.nav.fo.veilarbsituasjon.rest.domain.VeilederTilordning;
import no.nav.fo.veilarbsituasjon.services.AktoerIdService;
import no.nav.fo.veilarbsituasjon.services.PepClient;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.jms.core.JmsTemplate;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class AktoerIdToVeilederTest {

    @Mock
    private BrukerRepository brukerRepository;

    @Mock
    private JmsTemplate endreVeilederKo;

    @Mock
    private AktoerIdService aktoerIdService;

    @InjectMocks
    private PortefoljeRessurs portefoljeRessurs;

    @Mock
    HttpServletResponse httpServletResponse;

    @Mock
    private PepClient pepClient;

    @Test
    public void portefoljeRessursMustCallDAOwithAktoerIdToVeileder() throws PepException {
        when(pepClient.isServiceCallAllowed(anyString())).thenReturn(true);
        when(aktoerIdService.findAktoerId(any(String.class))).thenReturn("AKTOERID");
        portefoljeRessurs.postVeilederTilordninger(Collections.singletonList(testData()));
        verify(brukerRepository, times(1)).leggTilEllerOppdaterBruker(any(OppfolgingBruker.class));
    }

    @Test
    public void noCallToDAOWhenAktoerIdServiceFails() {
        when(aktoerIdService.findAktoerId(any(String.class))).thenThrow(new IndexOutOfBoundsException());
        portefoljeRessurs.postVeilederTilordninger(Collections.singletonList(testData()));
        verify(brukerRepository, never()).leggTilEllerOppdaterBruker(any(OppfolgingBruker.class));
    }

    private VeilederTilordning testData() {
        return new VeilederTilordning()
                .setFraVeilederId(null)
                .setTilVeilederId("***REMOVED***")
                .setBrukerFnr("12345678912");
    }
}