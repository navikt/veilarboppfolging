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
import org.junit.Test;

import java.util.Collections;

import static org.mockito.Mockito.*;

public class SystemOppfolgingControllerTest {

    private AuthService authService = mock(AuthService.class);

    private AktiverBrukerService aktiverBrukerService = mock(AktiverBrukerService.class);

    private SystemOppfolgingController systemOppfolgingController = new SystemOppfolgingController(authService, aktiverBrukerService);

    @Test
    public void aktiverBruker() {
        AktiverArbeidssokerData data = new AktiverArbeidssokerData();
        data.setFnr(new Fnr("fnr"));
        when(authService.getAktorIdOrThrow("fnr")).thenReturn("aktorId");
        systemOppfolgingController.aktiverBruker(data);
        verify(authService,  times(1)).skalVereSystemBruker();
    }

    @Test
    public void reaktiverBruker() {
        when(authService.getAktorIdOrThrow("fnr")).thenReturn("aktorId");
        systemOppfolgingController.reaktiverBruker(new Fnr("fnr"));
        verify(authService,  times(1)).skalVereSystemBruker();
    }

    @Test
    public void aktiverSykmeldt() {
        SubjectHandler.withSubject(new Subject("uid", IdentType.EksternBruker, SsoToken.oidcToken("oidcToken", Collections.emptyMap())), () -> {
            systemOppfolgingController.aktiverSykmeldt(SykmeldtBrukerType.SKAL_TIL_SAMME_ARBEIDSGIVER, "fnr");
            verify(authService,  times(1)).skalVereSystemBruker();
        });
    }
}
