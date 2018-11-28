package no.nav.fo.veilarboppfolging.services;

import no.nav.apiapp.feil.UlovligHandling;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.KvpRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingRepository;
import no.nav.fo.veilarboppfolging.domain.ArenaOppfolging;
import no.nav.fo.veilarboppfolging.domain.Kvp;
import no.nav.fo.veilarboppfolging.domain.Oppfolging;
import no.nav.fo.veilarboppfolging.domain.Oppfolgingsperiode;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Date;

import static java.util.Optional.of;
import static no.nav.fo.veilarboppfolging.domain.KodeverkBruker.SYSTEM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OppfolgingResolverTest {

    private static final String AKTOR_ID = "aktorId";
    private static final String FNR = "fnr";
    private static final String ENHET = "1234";
    private static final String OTHER_ENHET = "4321";
    private static final long KVP_ID = 1L;
    private Oppfolging oppfolging = new Oppfolging();

    @Mock(answer = Answers.RETURNS_MOCKS)
    private OppfolgingResolver.OppfolgingResolverDependencies oppfolgingResolverDependenciesMock;

    @Mock
    private OppfolgingRepository oppfolgingRepositoryMock;

    @Mock
    private AktorService aktorServiceMock;

    @Mock
    private KvpRepository kvpRepositoryMock;

    @Mock
    private KvpService kvpServiceMock;

    @Mock
    private ArenaOppfolgingService arenaOppfolgingServiceMock;

    @Mock
    private PepClient pepClientMock;

    private OppfolgingResolver oppfolgingResolver;

    @Before
    public void setup() throws Exception {
        when(oppfolgingResolverDependenciesMock.getAktorService()).thenReturn(aktorServiceMock);
        when(oppfolgingResolverDependenciesMock.getOppfolgingRepository()).thenReturn(oppfolgingRepositoryMock);
        when(oppfolgingResolverDependenciesMock.getKvpRepository()).thenReturn(kvpRepositoryMock);
        when(oppfolgingResolverDependenciesMock.getArenaOppfolgingService()).thenReturn(arenaOppfolgingServiceMock);
        when(oppfolgingResolverDependenciesMock.getPepClient()).thenReturn(pepClientMock);
        when(oppfolgingResolverDependenciesMock.getKvpService()).thenReturn(kvpServiceMock);

        when(aktorServiceMock.getAktorId(FNR)).thenReturn(of(AKTOR_ID));
        when(oppfolgingRepositoryMock.hentOppfolging(AKTOR_ID)).thenReturn(of(oppfolging));
        when(arenaOppfolgingServiceMock.hentArenaOppfolging(any())).thenReturn(oppfolgingIArena(ENHET));

        oppfolgingResolver = new OppfolgingResolver(FNR, oppfolgingResolverDependenciesMock);
    }

    @Test
    public void veileder_skal_ha_skrivetilgang_til_bruker_som_ikke_er_pa_kvp() {
        when(kvpRepositoryMock.gjeldendeKvp(AKTOR_ID)).thenReturn(0L);

        oppfolgingResolver = new OppfolgingResolver(FNR, oppfolgingResolverDependenciesMock);
        assertThat(oppfolgingResolver.harSkrivetilgangTilBruker()).isEqualTo(true);
    }

    @Test
    public void veileder_skal_ha_skrivetilgang_tilbruker_som_er_pa_kvp_hvis_han_har_tilgang_til_enheten() throws PepException {
        when(kvpRepositoryMock.gjeldendeKvp(AKTOR_ID)).thenReturn(KVP_ID);
        when(kvpRepositoryMock.fetch(KVP_ID)).thenReturn(Kvp.builder().kvpId(KVP_ID).aktorId(AKTOR_ID).enhet(ENHET).build());
        when(pepClientMock.harTilgangTilEnhet(ENHET)).thenReturn(true);

        oppfolgingResolver = new OppfolgingResolver(FNR, oppfolgingResolverDependenciesMock);
        assertThat(oppfolgingResolver.harSkrivetilgangTilBruker()).isEqualTo(true);
    }

    @Test
    public void veilder_skal_ikke_ha_skrivetilgang_til_bruker_som_er_pa_kvp_pa_en_enhet_han_ikke_har_tilgang_til() throws PepException {
        when(kvpRepositoryMock.gjeldendeKvp(AKTOR_ID)).thenReturn(KVP_ID);
        when(kvpRepositoryMock.fetch(KVP_ID)).thenReturn(Kvp.builder().kvpId(KVP_ID).aktorId(AKTOR_ID).enhet(OTHER_ENHET).build());
        when(pepClientMock.harTilgangTilEnhet(OTHER_ENHET)).thenReturn(false);

        oppfolgingResolver = new OppfolgingResolver(FNR, oppfolgingResolverDependenciesMock);
        assertThat(oppfolgingResolver.harSkrivetilgangTilBruker()).isEqualTo(false);
    }

    @Test
    public void kvp_periode_skal_automatisk_avsluttes_nar_bruker_har_byttet_oppfolgingsEnhet_i_arena() throws Exception {
        when(arenaOppfolgingServiceMock.hentArenaOppfolging(any())).thenReturn(oppfolgingIArena(OTHER_ENHET));
        when(kvpServiceMock.gjeldendeKvp(FNR)).thenReturn(Kvp.builder().kvpId(KVP_ID).aktorId(AKTOR_ID).enhet(ENHET).build());

        oppfolgingResolver = new OppfolgingResolver(FNR, oppfolgingResolverDependenciesMock);
        verify(kvpServiceMock, times(1)).stopKvpUtenEnhetSjekk(eq(FNR), any(), eq(SYSTEM), eq(oppfolgingResolver));
    }

    @Test
    public void kvp_periode_skal_ikke_avsluttes_sa_lenge_oppfolgingsenhet_i_arena_er_den_samme() throws Exception {
        when(kvpServiceMock.gjeldendeKvp(FNR)).thenReturn(Kvp.builder().kvpId(KVP_ID).aktorId(AKTOR_ID).enhet(ENHET).build());

        oppfolgingResolver = new OppfolgingResolver(FNR, oppfolgingResolverDependenciesMock);
        verify(kvpServiceMock, times(0)).stopKvpUtenEnhetSjekk(eq(FNR), any(), any(), any());
    }

    private ArenaOppfolging oppfolgingIArena(String enhet) {
        return new ArenaOppfolging().setOppfolgingsenhet(enhet);
    }

    @Test(expected = UlovligHandling.class)
    public void slettMal__under_oppfolging__ulovlig() {
        oppfolging.setUnderOppfolging(true);
        oppfolgingResolver.slettMal();
    }

    @Test
    public void slettMal__ikke_under_oppfolging_og_ingen_oppfolgingsperiode__slett_alle_mal_siden_1970() {
        oppfolgingResolver.slettMal();
        verify(oppfolgingRepositoryMock).slettMalForAktorEtter(AKTOR_ID, new Date(0));
    }

    @Test
    public void slettMal__ikke_under_oppfolging_og_oppfolgingsperioder__slett_alle_mal_etter_siste_avsluttede_periode() {
        Date date1 = new Date(1);
        Date date2 = new Date(2);
        Date date3 = new Date(3);
        oppfolging.setOppfolgingsperioder(Arrays.asList(
                periode(date1),
                periode(date3),
                periode(date2)
        ));

        oppfolgingResolver.slettMal();

        verify(oppfolgingRepositoryMock).slettMalForAktorEtter(eq(AKTOR_ID), eq(date3));
    }

    private Oppfolgingsperiode periode(Date date1) {
        return Oppfolgingsperiode.builder().sluttDato(date1).build();
    }

}