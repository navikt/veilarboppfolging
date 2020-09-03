package no.nav.veilarboppfolging.controller;

import no.nav.common.abac.VeilarbPep;
import no.nav.common.auth.subject.IdentType;
import no.nav.common.auth.subject.SsoToken;
import no.nav.common.auth.subject.Subject;
import no.nav.common.auth.subject.SubjectHandler;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.utils.Credentials;
import no.nav.veilarboppfolging.controller.domain.KvpDTO;
import no.nav.veilarboppfolging.domain.Kvp;
import no.nav.veilarboppfolging.repository.KvpRepository;
import no.nav.veilarboppfolging.service.ArenaOppfolgingService;
import no.nav.veilarboppfolging.service.AuthService;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
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

    private static final Subject AUTHORIZED_SUBJECT = new Subject("srvveilarbdialog", IdentType.Systemressurs, SsoToken.oidcToken("token", Collections.EMPTY_MAP));
    private static final Subject UNAUTHORIZED_SUBJECT = new Subject("user", IdentType.EksternBruker, SsoToken.oidcToken("token", Collections.EMPTY_MAP));

    private KvpRepository kvpRepositoryMock = mock(KvpRepository.class);

    private AuthService authService = new AuthService(mock(ArenaOppfolgingService.class), mock(VeilarbPep.class), mock(AktorregisterClient.class), new Credentials("srvtest", ""));

    private KvpController kvpController = new KvpController(kvpRepositoryMock, authService);

    @Test
    public void unauthorized_user() {
        try {
            getKvpStatus(UNAUTHORIZED_SUBJECT);
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.UNAUTHORIZED, e.getStatus());
        }
    }

    @Test
    public void no_active_kvp() {
        when(kvpRepositoryMock.gjeldendeKvp(anyString())).thenReturn(0L);
        KvpDTO kvp = getKvpStatus(AUTHORIZED_SUBJECT);
        assertThat(kvp).isNull();
    }

    @Test
    public void with_active_kvp() {
        when(kvpRepositoryMock.gjeldendeKvp(AKTOR_ID)).thenReturn(KVP_ID);
        when(kvpRepositoryMock.fetch(KVP_ID)).thenReturn(kvp());

        KvpDTO kvp = getKvpStatus(AUTHORIZED_SUBJECT);
        assertEquals(kvp.getEnhet(), ENHET_ID);
    }

    @Test
    public void with_active_kvp_but_unable_to_fetch_kvp_object() {
        when(kvpRepositoryMock.gjeldendeKvp(AKTOR_ID)).thenReturn(KVP_ID);
        when(kvpRepositoryMock.fetch(KVP_ID)).thenReturn(null);
        try {
            getKvpStatus(AUTHORIZED_SUBJECT);
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatus());
        }
    }

    private KvpDTO getKvpStatus(Subject subject) {
        return SubjectHandler.withSubject(subject, () -> kvpController.getKvpStatus(AKTOR_ID).getBody());
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
