package no.nav.veilarboppfolging.mock;

import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.health.HealthCheckResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UnleashServiceMock {

    public static UnleashService getMock() {
        UnleashService unleashService = mock(UnleashService.class);
        when(unleashService.isEnabled(any())).thenReturn(true);
        when(unleashService.checkHealth()).thenReturn(HealthCheckResult.healthy());
        return unleashService;
    }
}
