package no.nav.fo.veilarboppfolging.services;

import lombok.val;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.feil.UlovligHandling;
import no.nav.apiapp.security.PepClient;
import no.nav.common.auth.SsoToken;
import no.nav.common.auth.Subject;
import no.nav.common.auth.SubjectHandler;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.KvpRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingRepository;
import no.nav.fo.veilarboppfolging.domain.Oppfolging;
import no.nav.fo.veilarboppfolging.services.OppfolgingResolver.OppfolgingResolverDependencies;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingsstatusResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static java.util.Optional.of;
import static no.nav.brukerdialog.security.domain.IdentType.InternBruker;
import static no.nav.fo.veilarboppfolging.domain.KodeverkBruker.NAV;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class KvpServiceTest {

    public static final String VEILEDER = "1234";
    @Mock
    private KvpRepository kvpRepositoryMock;

    @Mock
    private AktorService aktorServiceMock;

    @Mock
    private OppfoelgingPortType oppfoelgingPortTypeMock;

    @Mock
    private PepClient pepClientMock;

    @Mock(answer = Answers.RETURNS_MOCKS)
    private OppfolgingResolverDependencies oppfolgingResolverDependenciesMock;

    @InjectMocks
    private KvpService kvpService;

    private static final String FNR = "1234";
    private static final String AKTOR_ID = "12345";
    private static final String ENHET = "1234";
    private static final String START_BEGRUNNELSE = "START_BEGRUNNELSE";
    private static final String STOP_BEGRUNNELSE = "STOP_BEGRUNNELSE";

    private OppfolgingRepository oppfolgingRepositoryMock;

    @Before
    public void initialize() throws Exception {
        when(aktorServiceMock.getAktorId(FNR)).thenReturn(of(AKTOR_ID));
        val res = new HentOppfoelgingsstatusResponse();
        res.setNavOppfoelgingsenhet(ENHET);
        when(oppfoelgingPortTypeMock.hentOppfoelgingsstatus(any())).thenReturn(res);

        oppfolgingRepositoryMock = mock(OppfolgingRepository.class);
        when(oppfolgingRepositoryMock.hentOppfolging(AKTOR_ID)).thenReturn(of(new Oppfolging().setUnderOppfolging(true)));
        when(oppfolgingResolverDependenciesMock.getOppfolgingRepository()).thenReturn(oppfolgingRepositoryMock);
        when(oppfolgingResolverDependenciesMock.getAktorService()).thenReturn(aktorServiceMock);
    }

    @Test(expected = UlovligHandling.class)
    public void start_kvp_uten_oppfolging_er_ulovlig_handling() {
        when(oppfolgingRepositoryMock.hentOppfolging(AKTOR_ID)).thenReturn(of(new Oppfolging().setUnderOppfolging(false)));
        kvpService.startKvp(FNR, START_BEGRUNNELSE);
    }

    @Test
    public void startKvp() throws PepException {
        SubjectHandler.withSubject(new Subject(VEILEDER, InternBruker, SsoToken.oidcToken("token")),
                () -> kvpService.startKvp(FNR, START_BEGRUNNELSE)
        );

        verify(pepClientMock, times(1)).sjekkLeseTilgangTilFnr(FNR);
        verify(kvpRepositoryMock, times(1)).startKvp(eq(AKTOR_ID), eq(ENHET), eq(VEILEDER), eq(START_BEGRUNNELSE));
        verify(pepClientMock, times(1)).sjekkTilgangTilEnhet(ENHET);
    }

    @Test
    public void stopKvp() throws PepException {
        SubjectHandler.withSubject(new Subject(VEILEDER, InternBruker, SsoToken.oidcToken("token")),
                () -> kvpService.stopKvp(FNR, STOP_BEGRUNNELSE)
        );

        verify(pepClientMock, times(1)).sjekkLeseTilgangTilFnr(FNR);
        verify(kvpRepositoryMock, times(1)).stopKvp(eq(AKTOR_ID), eq(VEILEDER), eq(STOP_BEGRUNNELSE), eq(NAV));
        verify(pepClientMock, times(1)).sjekkTilgangTilEnhet(ENHET);
    }

    @Test(expected = IngenTilgang.class)
    public void startKvpIkkeTilgang() {
        when(pepClientMock.sjekkLeseTilgangTilFnr(any())).thenThrow(IngenTilgang.class);

        kvpService.startKvp(FNR, START_BEGRUNNELSE);
    }

    @Test(expected = IngenTilgang.class)
    public void startKvpInhenEnhetTilgang() throws PepException {
        doThrow(IngenTilgang.class).when(pepClientMock).sjekkTilgangTilEnhet(any());

        kvpService.startKvp(FNR, START_BEGRUNNELSE);
    }

}
