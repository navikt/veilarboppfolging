package no.nav.veilarboppfolging.service;

import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.auth.context.UserRole;
import no.nav.common.test.auth.AuthTestUtils;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.BadRequestException;
import no.nav.veilarboppfolging.ForbiddenException;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolgingsBruker;
import no.nav.veilarboppfolging.kafka.KvpPeriode;
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingService;
import no.nav.veilarboppfolging.repository.KvpRepository;
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository;
import no.nav.veilarboppfolging.repository.entity.KvpPeriodeEntity;
import no.nav.veilarboppfolging.repository.entity.OppfolgingEntity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.function.Consumer;

import static no.nav.veilarboppfolging.repository.enums.KodeverkBruker.NAV;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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
    private ArenaOppfolgingService arenaOppfolgingService;

    @Mock
    private MetricsService metricsService;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @Mock
    private TransactionTemplate transactor;

    @InjectMocks
    private KvpService kvpService;

    private static final Fnr FNR = Fnr.of("1234");
    private static final AktorId AKTOR_ID = AktorId.of("12345");
    private static final String ENHET = "1234";
    private static final String START_BEGRUNNELSE = "START_BEGRUNNELSE";
    private static final String STOP_BEGRUNNELSE = "STOP_BEGRUNNELSE";
    private static final String VEILEDER = "1234";

    @Before
    public void initialize() {
        when(oppfolgingsStatusRepository.hentOppfolging(AKTOR_ID)).thenReturn(Optional.of(new OppfolgingEntity().setUnderOppfolging(true)));

        VeilarbArenaOppfolgingsBruker veilarbArenaOppfolgingsBruker = new VeilarbArenaOppfolgingsBruker();
        veilarbArenaOppfolgingsBruker.setNav_kontor(ENHET);
        when(arenaOppfolgingService.hentArenaOppfolgingsEnhetId(FNR)).thenReturn(EnhetId.of(ENHET));

        when(authService.harTilgangTilEnhet(anyString())).thenReturn(true);
        when(authService.getAktorIdOrThrow(FNR)).thenReturn(AKTOR_ID);
        when(authService.getInnloggetVeilederIdent()).thenReturn(VEILEDER);
        doAnswer((mock) -> {
            Consumer consumer = mock.getArgument(0);
            consumer.accept(null);
            return null;
        }).when(transactor).executeWithoutResult(any(Consumer.class));
    }

    @Test
    public void start_kvp_uten_oppfolging_er_ulovlig_handling() {
        when(oppfolgingsStatusRepository.hentOppfolging(AKTOR_ID)).thenReturn(Optional.of(new OppfolgingEntity().setUnderOppfolging(false)));

        try {
            kvpService.startKvp(FNR, START_BEGRUNNELSE);
        } catch (Exception e) {
            assertTrue(e instanceof BadRequestException);
        }
    }

    @Test
    public void start_kvp_uten_bruker_i_oppfolgingtabell_er_ulovlig_handling() {
        when(oppfolgingsStatusRepository.hentOppfolging(AKTOR_ID)).thenReturn(Optional.empty());

        try {
            kvpService.startKvp(FNR, START_BEGRUNNELSE);
        } catch (Exception e) {
            assertTrue(e instanceof BadRequestException);
        }
    }

    @Test
    public void startKvp() {
        kvpService.startKvp(FNR, START_BEGRUNNELSE);
        verify(kvpRepositoryMock, times(1)).startKvp(eq(AKTOR_ID), eq(ENHET), eq(VEILEDER), eq(START_BEGRUNNELSE), any());
    }

    @Test(expected = BadRequestException.class)
    public void startKvp_feiler_dersom_bruker_allerede_er_under_kvp() {

        when(oppfolgingsStatusRepository.hentOppfolging(AKTOR_ID)).thenReturn(Optional.of(new OppfolgingEntity().setUnderOppfolging(true).setGjeldendeKvpId(2)));

        AuthContextHolderThreadLocal.instance().withContext(AuthTestUtils.createAuthContext(UserRole.INTERN, VEILEDER),
                () -> kvpService.startKvp(FNR, START_BEGRUNNELSE)
        );

    }

    @Test
    public void stopKvp() {
        long kvpId = 2;
        var kvpStartTidspunkt = ZonedDateTime.now().minusDays(7);
        when(oppfolgingsStatusRepository.hentOppfolging(AKTOR_ID)).thenReturn(Optional.of(new OppfolgingEntity().setUnderOppfolging(true).setGjeldendeKvpId(kvpId)));
        when(kvpRepositoryMock.hentKvpPeriode(kvpId)).thenReturn(Optional.of(KvpPeriodeEntity.builder()
                        .aktorId(AKTOR_ID.get())
                        .opprettetDato(kvpStartTidspunkt)
                        .build()));

        AuthContextHolderThreadLocal.instance().withContext(AuthTestUtils.createAuthContext(UserRole.INTERN, VEILEDER),
                () -> kvpService.stopKvp(FNR, STOP_BEGRUNNELSE)
        );

        verify(authService, times(1)).sjekkLesetilgangMedAktorId(AKTOR_ID);
        verify(oppfolgingsStatusRepository, times(1)).hentOppfolging(AKTOR_ID);
        verify(kvpRepositoryMock, times(1)).stopKvp(eq(kvpId), eq(AKTOR_ID), eq(VEILEDER), eq(STOP_BEGRUNNELSE), eq(NAV), any());
        verify(authService, times(1)).harTilgangTilEnhet(ENHET);
        verify(kafkaProducerService, times(1)).publiserKvpPeriode(
                KvpPeriode
                        .start(AKTOR_ID, ENHET, VEILEDER, kvpStartTidspunkt, START_BEGRUNNELSE)
                        .avslutt(VEILEDER, any(), STOP_BEGRUNNELSE)
        );
    }

    @Test
    public void stopKvp_UtenAktivPeriode_feiler() {
        try {
            kvpService.stopKvp(FNR, STOP_BEGRUNNELSE);
        } catch(Exception e){
            assertTrue(e instanceof BadRequestException);
        }
    }

    @Test
    public void startKvpInhenEnhetTilgang() {
        when(authService.harTilgangTilEnhet(ENHET)).thenReturn(false);

        try {
            kvpService.startKvp(FNR, START_BEGRUNNELSE);
        } catch(Exception e){
            assertTrue(e instanceof ForbiddenException);
        }
    }

    @Test
    public void stopKvpInhenEnhetTilgang() {
        when(authService.harTilgangTilEnhet(ENHET)).thenReturn(false);

        try {
            kvpService.stopKvp(FNR, STOP_BEGRUNNELSE);
        } catch(Exception e){
            assertTrue(e instanceof ForbiddenException);
        }
    }

}
