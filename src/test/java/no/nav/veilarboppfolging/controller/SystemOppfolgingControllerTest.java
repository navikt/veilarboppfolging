package no.nav.veilarboppfolging.controller;

import no.nav.common.auth.subject.IdentType;
import no.nav.common.auth.subject.SsoToken;
import no.nav.common.auth.subject.Subject;
import no.nav.common.auth.subject.SubjectHandler;
import no.nav.veilarboppfolging.domain.AktiverArbeidssokerData;
import no.nav.veilarboppfolging.domain.Fnr;
import no.nav.veilarboppfolging.domain.SykmeldtBrukerType;
import no.nav.veilarboppfolging.service.AktiverBrukerService;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.OppfolgingService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SystemOppfolgingControllerTest {

    @InjectMocks
    private SystemOppfolgingController systemOppfolgingController;

    @Mock
    private AuthService authService;

    @Mock
    private OppfolgingService oppfolgingService;

    @Mock
    private AktiverBrukerService aktiverBrukerService;

    @Test
    public void aktiverBruker() throws Exception {
        AktiverArbeidssokerData data = new AktiverArbeidssokerData();
        data.setFnr(new Fnr("fnr"));
        when(authService.getAktorIdOrThrow("fnr")).thenReturn("aktorId");
        systemOppfolgingController.aktiverBruker(data);
        verify(authService,  times(1)).skalVereSystemBruker();
    }

    @Test
    public void reaktiverBruker() throws Exception {
        when(authService.getAktorIdOrThrow("fnr")).thenReturn("aktorId");
        systemOppfolgingController.reaktiverBruker(new Fnr("fnr"));
        verify(authService,  times(1)).skalVereSystemBruker();
    }

    @Test
    public void aktiverSykmeldt() throws Exception {
        SubjectHandler.withSubject(new Subject("uid", IdentType.EksternBruker, SsoToken.oidcToken("oidcToken", Collections.emptyMap())), () -> {
            systemOppfolgingController.aktiverSykmeldt(SykmeldtBrukerType.SKAL_TIL_SAMME_ARBEIDSGIVER, "fnr");
            verify(authService,  times(1)).skalVereSystemBruker();
        });
    }
}
