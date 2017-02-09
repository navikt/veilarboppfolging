package no.nav.fo.veilarbsituasjon.service;



import no.nav.fo.veilarbsituasjon.domain.AktoerIdToVeileder;
import no.nav.fo.veilarbsituasjon.repository.AktoerIdToVeilederDAO;
import no.nav.fo.veilarbsituasjon.rest.PortefoljeRessurs;
import no.nav.fo.veilarbsituasjon.rest.domain.VeilederTilordning;
import no.nav.fo.veilarbsituasjon.services.AktoerIdService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.jms.core.JmsTemplate;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class AktoerIdToVeilederTest {

    @Mock
    private AktoerIdToVeilederDAO aktoerIdToVeilederDAO;

    @Mock
    private JmsTemplate endreVeilederKo;

    @Mock
    private AktoerIdService aktoerIdService;

    @InjectMocks
    PortefoljeRessurs portefoljeRessurs;

    @Mock
    HttpServletResponse httpServletResponse;

    @Test
    public void portefoljeRessursMustCallDAOwithAktoerIdToVeileder() {

        portefoljeRessurs.postVeilederTilordninger(veilederTilordningList(), httpServletResponse);
        verify(aktoerIdToVeilederDAO, times(1)).opprettEllerOppdaterAktoerIdToVeileder(any(AktoerIdToVeileder.class));
    }

    @Test
    public void noCallToDAOWhenAktoerIdServiceFails() {
        when(aktoerIdService.findAktoerId(any(String.class))).thenThrow(new IndexOutOfBoundsException());
        portefoljeRessurs.postVeilederTilordninger(veilederTilordningList(), httpServletResponse);
        verify(aktoerIdToVeilederDAO, never()).opprettEllerOppdaterAktoerIdToVeileder(any(AktoerIdToVeileder.class));
    }

    public List<VeilederTilordning> veilederTilordningList() {
        VeilederTilordning veilederTilordning = new VeilederTilordning()
                .setIdentVeileder("***REMOVED***")
                .setFodselsnummerBruker("12345678912");
        List<VeilederTilordning> veilederTilordningList =  new ArrayList<VeilederTilordning>();
        veilederTilordningList.add(veilederTilordning);

        return veilederTilordningList;
    }
}