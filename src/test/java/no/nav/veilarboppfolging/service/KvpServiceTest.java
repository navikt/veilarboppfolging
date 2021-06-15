package no.nav.veilarboppfolging.service;

import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.auth.context.UserRole;
import no.nav.common.test.auth.AuthTestUtils;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolging;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClient;
import no.nav.veilarboppfolging.domain.OppfolgingTable;
import no.nav.veilarboppfolging.repository.EskaleringsvarselRepository;
import no.nav.veilarboppfolging.repository.KvpRepository;
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static no.nav.veilarboppfolging.domain.KodeverkBruker.NAV;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class KvpServiceTest {

    @Mock
    private KvpRepository kvpRepositoryMock;

    @Mock
    private AuthService authService;

    @Mock
    private OppfolgingsStatusRepository oppfolgingsStatusRepository;

    @Mock
    private EskaleringsvarselRepository eskaleringsvarselRepository;

    @Mock
    private VeilarbarenaClient veilarbarenaClient;

    @Mock
    private MetricsService metricsService;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @InjectMocks
    private KvpService kvpService;

    private static final String FNR = "1234";
    private static final String AKTOR_ID = "12345";
    private static final String ENHET = "1234";
    private static final String START_BEGRUNNELSE = "START_BEGRUNNELSE";
    private static final String STOP_BEGRUNNELSE = "STOP_BEGRUNNELSE";
    private static final String VEILEDER = "1234";

    @Before
    public void initialize() {
        when(oppfolgingsStatusRepository.fetch(AKTOR_ID)).thenReturn(new OppfolgingTable().setUnderOppfolging(true));

        VeilarbArenaOppfolging veilarbArenaOppfolging = new VeilarbArenaOppfolging();
        veilarbArenaOppfolging.setNav_kontor(ENHET);
        when(veilarbarenaClient.hentOppfolgingsbruker(Fnr.of(FNR))).thenReturn(Optional.of(veilarbArenaOppfolging));

        when(authService.harTilgangTilEnhet(anyString())).thenReturn(true);
        when(authService.getAktorIdOrThrow(FNR)).thenReturn(AKTOR_ID);
        when(authService.getInnloggetVeilederIdent()).thenReturn(VEILEDER);
    }

    @Test
    public void start_kvp_uten_oppfolging_er_ulovlig_handling() {
        when(oppfolgingsStatusRepository.fetch(AKTOR_ID)).thenReturn(new OppfolgingTable().setUnderOppfolging(false));

        try {
            kvpService.startKvp(FNR, START_BEGRUNNELSE);
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        }
    }

    @Test
    public void start_kvp_uten_bruker_i_oppfolgingtabell_er_ulovlig_handling() {
        when(oppfolgingsStatusRepository.fetch(AKTOR_ID)).thenReturn(null);

        try {
            kvpService.startKvp(FNR, START_BEGRUNNELSE);
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        }
    }

    @Test
    public void startKvp() {
        kvpService.startKvp(FNR, START_BEGRUNNELSE);
        verify(kvpRepositoryMock, times(1)).startKvp(eq(AKTOR_ID), eq(ENHET), eq(VEILEDER), eq(START_BEGRUNNELSE));
    }

    @Test(expected = ResponseStatusException.class)
    public void startKvp_feiler_dersom_bruker_allerede_er_under_kvp() {

        when(oppfolgingsStatusRepository.fetch(AKTOR_ID)).thenReturn(new OppfolgingTable().setUnderOppfolging(true).setGjeldendeKvpId(2));

        AuthContextHolderThreadLocal.instance().withContext(AuthTestUtils.createAuthContext(UserRole.INTERN, VEILEDER),
                () -> kvpService.startKvp(FNR, START_BEGRUNNELSE)
        );

    }

    @Test
    public void stopKvp() {
        long kvpId = 2;
        when(oppfolgingsStatusRepository.fetch(AKTOR_ID)).thenReturn(new OppfolgingTable().setUnderOppfolging(true).setGjeldendeKvpId(kvpId));

        AuthContextHolderThreadLocal.instance().withContext(AuthTestUtils.createAuthContext(UserRole.INTERN, VEILEDER),
                () -> kvpService.stopKvp(FNR, STOP_BEGRUNNELSE)
        );

        verify(authService, times(1)).sjekkLesetilgangMedAktorId(AKTOR_ID);
        verify(oppfolgingsStatusRepository, times(1)).fetch(AKTOR_ID);
        verify(kvpRepositoryMock, times(1)).stopKvp(eq(kvpId), eq(AKTOR_ID), eq(VEILEDER), eq(STOP_BEGRUNNELSE), eq(NAV));
        verify(authService, times(1)).harTilgangTilEnhet(ENHET);
    }

    @Test
    public void stopKvp_avslutter_eskalering() {
        long kvpId = 2;
        when(oppfolgingsStatusRepository.fetch(AKTOR_ID)).thenReturn(new OppfolgingTable().setUnderOppfolging(true).setGjeldendeEskaleringsvarselId(1).setGjeldendeKvpId(kvpId));

        AuthContextHolderThreadLocal.instance().withContext(AuthTestUtils.createAuthContext(UserRole.INTERN, VEILEDER),
                () -> kvpService.stopKvp(FNR, STOP_BEGRUNNELSE)
        );

        verify(eskaleringsvarselRepository).finish(AKTOR_ID, 1, VEILEDER, KvpService.ESKALERING_AVSLUTTET_FORDI_KVP_BLE_AVSLUTTET);
        verify(kvpRepositoryMock, times(1)).stopKvp(eq(kvpId), eq(AKTOR_ID), eq(VEILEDER), eq(STOP_BEGRUNNELSE), eq(NAV));
    }

    @Test
    public void stopKvp_UtenAktivPeriode_feiler() {
        try {
            kvpService.stopKvp(FNR, STOP_BEGRUNNELSE);
        } catch(ResponseStatusException e){
            assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        }
    }

    @Test
    public void startKvpInhenEnhetTilgang() {
        when(authService.harTilgangTilEnhet(ENHET)).thenReturn(false);

        try {
            kvpService.startKvp(FNR, START_BEGRUNNELSE);
        } catch(ResponseStatusException e){
            assertEquals(HttpStatus.UNAUTHORIZED, e.getStatus());
        }
    }

    @Test
    public void stopKvpInhenEnhetTilgang() {
        when(authService.harTilgangTilEnhet(ENHET)).thenReturn(false);

        try {
            kvpService.stopKvp(FNR, STOP_BEGRUNNELSE);
        } catch(ResponseStatusException e){
            assertEquals(HttpStatus.UNAUTHORIZED, e.getStatus());
        }
    }

}
