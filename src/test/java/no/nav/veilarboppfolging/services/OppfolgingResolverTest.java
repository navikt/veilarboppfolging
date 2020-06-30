package no.nav.veilarboppfolging.services;

import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.veilarboppfolging.db.KvpRepository;
import no.nav.veilarboppfolging.db.OppfolgingRepository;
import no.nav.veilarboppfolging.domain.ArenaOppfolging;
import no.nav.veilarboppfolging.domain.Kvp;
import no.nav.veilarboppfolging.domain.Oppfolging;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolging;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;

import static java.util.Optional.of;
import static no.nav.veilarboppfolging.domain.KodeverkBruker.SYSTEM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
abstract class OppfolgingResolverTest {

    private boolean brukArenaDirekte;

    OppfolgingResolverTest(boolean brukArenaDirekte) {
        this.brukArenaDirekte = brukArenaDirekte;
    }

    protected static final String AKTOR_ID = "aktorId";
    protected static final String FNR = "fnr";
    protected static final String ENHET = "1234";
    protected static final String OTHER_ENHET = "4321";
    protected static final long KVP_ID = 1L;
    protected Oppfolging oppfolging = new Oppfolging();

    @Mock(answer = Answers.RETURNS_MOCKS)
    protected OppfolgingResolver.OppfolgingResolverDependencies oppfolgingResolverDependenciesMock;

    @Mock
    protected OppfolgingRepository oppfolgingRepositoryMock;

    @Mock
    private AktorService aktorServiceMock;

    @Mock
    private KvpRepository kvpRepositoryMock;

    @Mock
    private KvpService kvpServiceMock;

    @Mock
    protected ArenaOppfolgingService arenaOppfolgingServiceMock;

    @Mock
    protected OppfolgingsbrukerService oppfolgingsbrukerServiceMock;

    @Mock
    private PepClient pepClientMock;

    @Mock
    private UnleashService unleashServiceMock;

    protected OppfolgingResolver oppfolgingResolver;

    @Before
    public void setup() throws Exception {

        MockitoAnnotations.initMocks(this);

        when(oppfolgingResolverDependenciesMock.getAktorService()).thenReturn(aktorServiceMock);
        when(oppfolgingResolverDependenciesMock.getOppfolgingRepository()).thenReturn(oppfolgingRepositoryMock);
        when(oppfolgingResolverDependenciesMock.getKvpRepository()).thenReturn(kvpRepositoryMock);

        when(oppfolgingResolverDependenciesMock.getPepClient()).thenReturn(pepClientMock);
        when(oppfolgingResolverDependenciesMock.getKvpService()).thenReturn(kvpServiceMock);
        when(oppfolgingResolverDependenciesMock.getUnleashService()).thenReturn(unleashServiceMock);

        if (brukArenaDirekte) {
            setupArenaService();
        } else {
            setupVeilarbArenaService();
        }

        when(aktorServiceMock.getAktorId(FNR)).thenReturn(of(AKTOR_ID));
        when(oppfolgingRepositoryMock.hentOppfolging(AKTOR_ID)).thenReturn(of(oppfolging));
        when(unleashServiceMock.isEnabled("veilarboppfolging.oppfolgingresolver.bruk_arena_direkte")).thenReturn(brukArenaDirekte);

        oppfolgingResolver = OppfolgingResolver.lagOppfolgingResolver(FNR, oppfolgingResolverDependenciesMock);
    }

    @Test
    public void veileder_skal_ha_skrivetilgang_til_bruker_som_ikke_er_pa_kvp() {
        when(kvpRepositoryMock.gjeldendeKvp(AKTOR_ID)).thenReturn(0L);

        oppfolgingResolver = OppfolgingResolver.lagOppfolgingResolver(FNR, oppfolgingResolverDependenciesMock);
        assertThat(oppfolgingResolver.harSkrivetilgangTilBruker()).isEqualTo(true);
    }

    @Test
    public void veileder_skal_ha_skrivetilgang_tilbruker_som_er_pa_kvp_hvis_han_har_tilgang_til_enheten() throws PepException {
        when(kvpRepositoryMock.gjeldendeKvp(AKTOR_ID)).thenReturn(KVP_ID);
        when(kvpRepositoryMock.fetch(KVP_ID)).thenReturn(Kvp.builder().kvpId(KVP_ID).aktorId(AKTOR_ID).enhet(ENHET).build());
        when(pepClientMock.harTilgangTilEnhet(ENHET)).thenReturn(true);

        oppfolgingResolver = OppfolgingResolver.lagOppfolgingResolver(FNR, oppfolgingResolverDependenciesMock);
        assertThat(oppfolgingResolver.harSkrivetilgangTilBruker()).isEqualTo(true);
    }

    @Test
    public void veilder_skal_ikke_ha_skrivetilgang_til_bruker_som_er_pa_kvp_pa_en_enhet_han_ikke_har_tilgang_til() throws PepException {
        when(kvpRepositoryMock.gjeldendeKvp(AKTOR_ID)).thenReturn(KVP_ID);
        when(kvpRepositoryMock.fetch(KVP_ID)).thenReturn(Kvp.builder().kvpId(KVP_ID).aktorId(AKTOR_ID).enhet(OTHER_ENHET).build());
        when(pepClientMock.harTilgangTilEnhet(OTHER_ENHET)).thenReturn(false);

        oppfolgingResolver = OppfolgingResolver.lagOppfolgingResolver(FNR, oppfolgingResolverDependenciesMock);
        assertThat(oppfolgingResolver.harSkrivetilgangTilBruker()).isEqualTo(false);
    }

    @Test
    public void kvp_periode_skal_automatisk_avsluttes_nar_bruker_har_byttet_oppfolgingsEnhet_i_arena() throws Exception {
        mockSvarFraArena(OTHER_ENHET);
        when(kvpServiceMock.gjeldendeKvp(AKTOR_ID)).thenReturn(Kvp.builder().kvpId(KVP_ID).aktorId(AKTOR_ID).enhet(ENHET).build());

        oppfolgingResolver = OppfolgingResolver.lagOppfolgingResolver(FNR, oppfolgingResolverDependenciesMock);
        verify(kvpServiceMock, times(1)).stopKvpUtenEnhetSjekk(eq(AKTOR_ID), any(), eq(SYSTEM));
    }

    @Test
    public void kvp_periode_skal_ikke_avsluttes_sa_lenge_oppfolgingsenhet_i_arena_er_den_samme() throws Exception {
        when(kvpServiceMock.gjeldendeKvp(AKTOR_ID)).thenReturn(Kvp.builder().kvpId(KVP_ID).aktorId(AKTOR_ID).enhet(ENHET).build());

        oppfolgingResolver = OppfolgingResolver.lagOppfolgingResolver(FNR, oppfolgingResolverDependenciesMock);
        verify(kvpServiceMock, times(0)).stopKvpUtenEnhetSjekk(eq(AKTOR_ID), any(), any());
    }

    protected void mockSvarFraArena(String enhet) {
        if (brukArenaDirekte) {
            when(arenaOppfolgingServiceMock.hentArenaOppfolging(any())).thenReturn(new ArenaOppfolging().setOppfolgingsenhet(enhet));
        } else {
            when(oppfolgingsbrukerServiceMock.hentOppfolgingsbruker(any())).thenReturn(Optional.of(new VeilarbArenaOppfolging().setNav_kontor(enhet)));
        }
    }

    protected void setupVeilarbArenaService() {
        when(oppfolgingResolverDependenciesMock.getOppfolgingsbrukerService()).thenReturn(oppfolgingsbrukerServiceMock);
    }

    protected void setupArenaService() {
        when(oppfolgingResolverDependenciesMock.getArenaOppfolgingService()).thenReturn(arenaOppfolgingServiceMock);
    }

}
