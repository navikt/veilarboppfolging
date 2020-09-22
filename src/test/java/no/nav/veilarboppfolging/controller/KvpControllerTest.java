package no.nav.veilarboppfolging.controller;

import no.nav.common.abac.VeilarbPep;
import no.nav.common.auth.context.AuthContext;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.UserRole;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.test.auth.AuthTestUtils;
import no.nav.common.utils.Credentials;
import no.nav.veilarboppfolging.controller.domain.KvpDTO;
import no.nav.veilarboppfolging.domain.Kvp;
import no.nav.veilarboppfolging.repository.KvpRepository;
import no.nav.veilarboppfolging.service.ArenaOppfolgingService;
import no.nav.veilarboppfolging.service.AuthService;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KvpControllerTest {

    private static final String AKTOR_ID = "1234";
    private static final long KVP_ID = 1L;
    private static final String ENHET_ID = "0001";

    private static final AuthContext AUTHORIZED_CONTEXT = AuthTestUtils.createAuthContext(UserRole.SYSTEM, "srvveilarbdialog");
    private static final AuthContext UNAUTHORIZED_CONTEXT = AuthTestUtils.createAuthContext(UserRole.EKSTERN, "user");


    private KvpRepository kvpRepositoryMock = mock(KvpRepository.class);

    private AuthService authService = new AuthService(mock(ArenaOppfolgingService.class), mock(VeilarbPep.class), mock(AktorregisterClient.class), new Credentials("srvtest", ""));

    private KvpController kvpController = new KvpController(kvpRepositoryMock, authService);

    @Test
    public void unauthorized_user() {
        try {
            getKvpStatus(UNAUTHORIZED_CONTEXT);
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.UNAUTHORIZED, e.getStatus());
        }
    }

    @Test
    public void no_active_kvp() {
        when(kvpRepositoryMock.gjeldendeKvp(anyString())).thenReturn(0L);
        KvpDTO kvp = getKvpStatus(AUTHORIZED_CONTEXT);
        assertThat(kvp).isNull();
    }

    @Test
    public void with_active_kvp() {
        when(kvpRepositoryMock.gjeldendeKvp(AKTOR_ID)).thenReturn(KVP_ID);
        when(kvpRepositoryMock.fetch(KVP_ID)).thenReturn(kvp());

        KvpDTO kvp = getKvpStatus(AUTHORIZED_CONTEXT);
        assertEquals(kvp.getEnhet(), ENHET_ID);
    }

    @Test
    public void with_active_kvp_but_unable_to_fetch_kvp_object() {
        when(kvpRepositoryMock.gjeldendeKvp(AKTOR_ID)).thenReturn(KVP_ID);
        when(kvpRepositoryMock.fetch(KVP_ID)).thenReturn(null);
        try {
            getKvpStatus(AUTHORIZED_CONTEXT);
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatus());
        }
    }

    private KvpDTO getKvpStatus(AuthContext context) {
        return AuthContextHolder.withContext(context, () -> kvpController.getKvpStatus(AKTOR_ID).getBody());
    }

    private Kvp kvp() {
        return Kvp.builder()
                .kvpId(KVP_ID)
                .aktorId(AKTOR_ID)
                .opprettetDato(new Date())
                .enhet(ENHET_ID)
                .build();
    }

}
