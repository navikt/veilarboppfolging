package no.nav.fo.veilarboppfolging.services;

import lombok.val;
import no.nav.apiapp.feil.UlovligHandling;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.OppfolgingRepository;
import no.nav.fo.veilarboppfolging.domain.Kvp;
import no.nav.fo.veilarboppfolging.domain.Oppfolging;
import no.nav.fo.veilarboppfolging.domain.Oppfolgingsperiode;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingsstatusResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Date;

import static java.util.Optional.of;
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
    private KvpService kvpServiceMock;

    @Mock
    private OppfoelgingPortType oppfoelgingPortTypeMock;

    private OppfolgingResolver oppfolgingResolver;

    @Before
    public void setup() {
        when(oppfolgingResolverDependenciesMock.getAktorService()).thenReturn(aktorServiceMock);
        when(oppfolgingResolverDependenciesMock.getOppfolgingRepository()).thenReturn(oppfolgingRepositoryMock);
        when(oppfolgingResolverDependenciesMock.getKvpService()).thenReturn(kvpServiceMock);
        when(oppfolgingResolverDependenciesMock.getOppfoelgingPortType()).thenReturn(oppfoelgingPortTypeMock);

        when(aktorServiceMock.getAktorId(FNR)).thenReturn(of(AKTOR_ID));
        when(oppfolgingRepositoryMock.hentOppfolging(AKTOR_ID)).thenReturn(of(oppfolging));

        oppfolgingResolver = new OppfolgingResolver(FNR, oppfolgingResolverDependenciesMock);
    }

    @Test
    public void avslutt_kvp_ved_bytte_av_enhet() throws Exception {
        oppfolging.setGjeldendeKvp(Kvp.builder().kvpId(KVP_ID).aktorId(AKTOR_ID).enhet(ENHET).build());
        when(oppfoelgingPortTypeMock.hentOppfoelgingsstatus(any())).thenReturn(oppfolgingIArena(OTHER_ENHET));

        oppfolgingResolver = new OppfolgingResolver(FNR, oppfolgingResolverDependenciesMock);
        verify(kvpServiceMock, times(1)).stopKvp(eq(FNR), any());
    }

    @Test
    public void ikke_avslutt_kvp_nar_enhet_ikke_byttet() throws Exception {
        oppfolging.setGjeldendeKvp(Kvp.builder().kvpId(KVP_ID).aktorId(AKTOR_ID).enhet(ENHET).build());
        when(oppfoelgingPortTypeMock.hentOppfoelgingsstatus(any())).thenReturn(oppfolgingIArena(ENHET));

        oppfolgingResolver = new OppfolgingResolver(FNR, oppfolgingResolverDependenciesMock);
        verify(kvpServiceMock, times(0)).stopKvp(eq(FNR), any());
    }

    private HentOppfoelgingsstatusResponse oppfolgingIArena(String enhet) {
        val res = new HentOppfoelgingsstatusResponse();
        res.setNavOppfoelgingsenhet(enhet);
        return res;
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