package no.nav.fo.veilarbsituasjon.services;



import no.nav.fo.veilarbsituasjon.db.BrukerRepository;
import no.nav.fo.veilarbsituasjon.domain.OppfolgingBruker;
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
    private BrukerRepository brukerRepository;

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

        portefoljeRessurs.postVeilederTilordninger(veilederTilordningList());
        verify(brukerRepository, times(1)).leggTilEllerOppdaterBruker(any(OppfolgingBruker.class));
    }

    @Test
    public void noCallToDAOWhenAktoerIdServiceFails() {
        when(aktoerIdService.findAktoerId(any(String.class))).thenThrow(new IndexOutOfBoundsException());
        portefoljeRessurs.postVeilederTilordninger(veilederTilordningList());
        verify(brukerRepository, never()).leggTilEllerOppdaterBruker(any(OppfolgingBruker.class));
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