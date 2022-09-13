package no.nav.veilarboppfolging.controller;

import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.auth.context.UserRole;
import no.nav.common.test.auth.AuthTestUtils;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.request.AktiverArbeidssokerData;
import no.nav.veilarboppfolging.controller.request.ReaktiverBrukerRequest;
import no.nav.veilarboppfolging.controller.request.SykmeldtBrukerType;
import no.nav.veilarboppfolging.service.AktiverBrukerService;
import no.nav.veilarboppfolging.service.AuthService;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class SystemOppfolgingControllerTest {

    private final static Fnr FNR = Fnr.of("123");

    private final static AktorId AKTOR_ID = AktorId.of("3409823");

    private AuthService authService = mock(AuthService.class);

    private AktiverBrukerService aktiverBrukerService = mock(AktiverBrukerService.class);

    private SystemOppfolgingController systemOppfolgingController = new SystemOppfolgingController(
            authService,
            aktiverBrukerService
    );

    @Test
    public void aktiverBruker() {
        AktiverArbeidssokerData data = new AktiverArbeidssokerData();
        data.setFnr(new AktiverArbeidssokerData.Fnr(FNR.get()));
        when(authService.getAktorIdOrThrow(FNR)).thenReturn(AKTOR_ID);
        systemOppfolgingController.aktiverBruker(data);
        verify(authService,  times(1)).skalVereSystemBruker();
    }

    @Test
    public void reaktiverBruker() {
        when(authService.getAktorIdOrThrow(FNR)).thenReturn(AKTOR_ID);
        systemOppfolgingController.reaktiverBruker(new ReaktiverBrukerRequest(FNR));
        verify(authService,  times(1)).skalVereSystemBruker();
    }

    @Test
    public void aktiverSykmeldt() {
        AuthContextHolderThreadLocal.instance().withContext(AuthTestUtils.createAuthContext(UserRole.EKSTERN, "uid"), () -> {
            systemOppfolgingController.aktiverSykmeldt(SykmeldtBrukerType.SKAL_TIL_SAMME_ARBEIDSGIVER, FNR);
            verify(authService,  times(1)).skalVereSystemBruker();
        });
    }
}
