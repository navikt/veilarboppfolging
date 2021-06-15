package no.nav.veilarboppfolging.controller;

import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.auth.context.UserRole;
import no.nav.common.test.auth.AuthTestUtils;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.request.AktiverArbeidssokerData;
import no.nav.veilarboppfolging.controller.request.ReaktiverBrukerRequest;
import no.nav.veilarboppfolging.controller.request.SykmeldtBrukerType;
import no.nav.veilarboppfolging.service.AktiverBrukerService;
import no.nav.veilarboppfolging.service.AuthService;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class SystemOppfolgingControllerTest {

    private AuthService authService = mock(AuthService.class);

    private AktiverBrukerService aktiverBrukerService = mock(AktiverBrukerService.class);

    private SystemOppfolgingController systemOppfolgingController = new SystemOppfolgingController(authService, aktiverBrukerService);

    @Test
    public void aktiverBruker() {
        AktiverArbeidssokerData data = new AktiverArbeidssokerData();
        data.setFnr(new no.nav.veilarboppfolging.controller.request.Fnr("fnr"));
        when(authService.getAktorIdOrThrow("fnr")).thenReturn("aktorId");
        systemOppfolgingController.aktiverBruker(data);
        verify(authService,  times(1)).skalVereSystemBruker();
    }

    @Test
    public void reaktiverBruker() {
        when(authService.getAktorIdOrThrow("fnr")).thenReturn("aktorId");
        systemOppfolgingController.reaktiverBruker(new ReaktiverBrukerRequest(Fnr.of("fnr")));
        verify(authService,  times(1)).skalVereSystemBruker();
    }

    @Test
    public void aktiverSykmeldt() {
        AuthContextHolderThreadLocal.instance().withContext(AuthTestUtils.createAuthContext(UserRole.EKSTERN, "uid"), () -> {
            systemOppfolgingController.aktiverSykmeldt(SykmeldtBrukerType.SKAL_TIL_SAMME_ARBEIDSGIVER, Fnr.of("fnr"));
            verify(authService,  times(1)).skalVereSystemBruker();
        });
    }
}
