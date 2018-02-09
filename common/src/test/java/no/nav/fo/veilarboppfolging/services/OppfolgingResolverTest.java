package no.nav.fo.veilarboppfolging.services;

import lombok.val;
import no.nav.apiapp.feil.UlovligHandling;
import no.nav.brukerdialog.security.context.ThreadLocalSubjectHandler;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.KvpRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingRepository;
import no.nav.fo.veilarboppfolging.domain.Kvp;
import no.nav.fo.veilarboppfolging.domain.Oppfolging;
import no.nav.fo.veilarboppfolging.domain.Oppfolgingsperiode;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingsstatusResponse;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Date;

import static java.lang.System.setProperty;
import static java.util.Optional.of;
import static no.nav.brukerdialog.security.context.SubjectHandler.SUBJECTHANDLER_KEY;
import static no.nav.brukerdialog.security.context.SubjectHandlerUtils.SubjectBuilder;
import static no.nav.brukerdialog.security.context.SubjectHandlerUtils.setSubject;
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
    private OppfoelgingPortType oppfoelgingPortTypeMock;

    private OppfolgingResolver oppfolgingResolver;

    @BeforeClass
    public static void before() {
        setProperty("no.nav.modig.security.systemuser.username", "username");
        setProperty("no.nav.modig.security.systemuser.password", "password");
        setProperty(SUBJECTHANDLER_KEY, ThreadLocalSubjectHandler.class.getName());
        setSubject(new SubjectBuilder("USER", IdentType.InternBruker).withAuthLevel(3).getSubject());
    }

    @Before
    public void setup() {
        when(oppfolgingResolverDependenciesMock.getAktorService()).thenReturn(aktorServiceMock);
        when(oppfolgingResolverDependenciesMock.getOppfolgingRepository()).thenReturn(oppfolgingRepositoryMock);
        when(oppfolgingResolverDependenciesMock.getKvpRepository()).thenReturn(kvpRepositoryMock);
        when(oppfolgingResolverDependenciesMock.getOppfoelgingPortType()).thenReturn(oppfoelgingPortTypeMock);

        when(aktorServiceMock.getAktorId(FNR)).thenReturn(of(AKTOR_ID));
        when(oppfolgingRepositoryMock.hentOppfolging(AKTOR_ID)).thenReturn(of(oppfolging));

        oppfolgingResolver = new OppfolgingResolver(FNR, oppfolgingResolverDependenciesMock);
    }

    @Test
    public void kvp_periode_skal_automatisk_avsluttes_nar_bruker_har_byttet_oppfolgingsEnhet_i_arena() throws Exception {
        when(oppfoelgingPortTypeMock.hentOppfoelgingsstatus(any())).thenReturn(oppfolgingIArena(OTHER_ENHET));
        when(kvpRepositoryMock.gjeldendeKvp(AKTOR_ID)).thenReturn(KVP_ID);
        when(kvpRepositoryMock.fetch(KVP_ID)).thenReturn(Kvp.builder().kvpId(KVP_ID).aktorId(AKTOR_ID).enhet(ENHET).build());

        oppfolgingResolver = new OppfolgingResolver(FNR, oppfolgingResolverDependenciesMock);
        verify(kvpRepositoryMock, times(1)).stopKvp(eq(AKTOR_ID), any(), any());
    }

    @Test
    public void kvp_periode_skal_ikke_avsluttes_sa_lenge_oppfolgingsenhet_i_arena_er_den_samme() throws Exception {
        when(kvpRepositoryMock.gjeldendeKvp(AKTOR_ID)).thenReturn(0L);

        oppfolgingResolver = new OppfolgingResolver(FNR, oppfolgingResolverDependenciesMock);
        verify(kvpRepositoryMock, times(0)).stopKvp(eq(AKTOR_ID), any(), any());
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