package no.nav.fo.veilarboppfolging.rest;

import no.nav.apiapp.security.PepClient;
import no.nav.brukerdialog.security.context.SubjectRule;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.common.auth.SsoToken;
import no.nav.common.auth.Subject;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.domain.AktiverArbeidssokerData;
import no.nav.fo.veilarboppfolging.domain.Fnr;
import no.nav.fo.veilarboppfolging.domain.SykmeldtBrukerType;
import no.nav.fo.veilarboppfolging.services.AktiverBrukerService;
import no.nav.fo.veilarboppfolging.services.OppfolgingService;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;

import static org.mockito.Mockito.*;

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
    AktorService aktorService;

    @Mock
    private PepClient pepClient;

    @Mock
    private FnrParameterUtil fnrParameterUtil;

    @Rule
    public SubjectRule subjectRule = new SubjectRule();

    @Test
    public void aktiverBruker() throws Exception {
        AktiverArbeidssokerData data = new AktiverArbeidssokerData();
        data.setFnr(new Fnr("fnr"));
        when(aktorService.getAktorId("fnr")).thenReturn(Optional.of("aktorId"));
        oppfolgingRessurs.aktiverBruker(data);
        verify(autorisasjonService,  times(1)).skalVereSystemRessurs();
    }

    @Test
    public void reaktiverBruker() throws Exception {
        when(aktorService.getAktorId("fnr")).thenReturn(Optional.of("aktorId"));
        oppfolgingRessurs.reaktiverBruker(new Fnr("fnr"));
        verify(autorisasjonService,  times(1)).skalVereSystemRessurs();
    }

    @Test
    public void aktiverSykmeldt() throws Exception {
        subjectRule.setSubject(new Subject("uid", IdentType.EksternBruker, SsoToken.oidcToken("oidcToken")));
        oppfolgingRessurs.aktiverSykmeldt(SykmeldtBrukerType.SKAL_TIL_SAMME_ARBEIDSGIVER);
        verify(autorisasjonService,  times(1)).skalVereSystemRessurs();
    }
}
