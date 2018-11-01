package no.nav.fo.veilarboppfolging.rest;

import no.nav.apiapp.security.PepClient;
import no.nav.fo.veilarboppfolging.domain.AktiverArbeidssokerData;
import no.nav.fo.veilarboppfolging.domain.Fnr;
import no.nav.fo.veilarboppfolging.services.AktiverBrukerService;
import no.nav.fo.veilarboppfolging.services.OppfolgingService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class OppfolgingRessursTest {

    @InjectMocks
    private OppfolgingRessurs oppfolgingRessurs;

    @Mock
    private AutorisasjonService autorisasjonService;

    @Mock
    private OppfolgingService oppfolgingService;

    @Mock
    private AktiverBrukerService aktiverBrukerService;

    @Mock
    private PepClient pepClient;

    @Test
    public void aktiverBruker() throws Exception {
        AktiverArbeidssokerData data = new AktiverArbeidssokerData();
        data.setFnr(new Fnr(""));
        oppfolgingRessurs.aktiverBruker(data);
        verify(autorisasjonService,  times(1)).skalVereSystemRessurs();
    }

    @Test
    public void reaktiverBruker() throws Exception {
        oppfolgingRessurs.reaktiverBruker(new Fnr(""));
        verify(autorisasjonService,  times(1)).skalVereSystemRessurs();
    }

    @Test
    public void aktiverSykmeldt() throws Exception {
        oppfolgingRessurs.aktiverSykmeldt();
        verify(autorisasjonService,  times(1)).skalVereSystemRessurs();
    }
}