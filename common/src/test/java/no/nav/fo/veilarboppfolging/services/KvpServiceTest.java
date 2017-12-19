package no.nav.fo.veilarboppfolging.services;

import lombok.val;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.security.PepClient;
import no.nav.brukerdialog.security.context.ThreadLocalSubjectHandler;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.KvpRepository;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingsstatusResponse;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static java.util.Optional.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class KvpServiceTest {

    public static final String ENHET = "1234";
    public static final String START_BEGRUNNELSE = "START_BEGRUNNELSE";
    public static final String STOP_BEGRUNNELSE = "STOP_BEGRUNNELSE";
    @Mock
    private KvpRepository kvpRepositoryMock;

    @Mock
    private AktorService aktorServiceMock;

    @Mock
    private OppfoelgingPortType oppfoelgingPortTypeMock;

    @Mock
    private PepClient pepClientMock;

    @InjectMocks
    private KvpService kvpService;

    public static final String FNR = "12345678912";
    public static final String AKTOR_ID = "12345";

    @BeforeClass
    public static void setUp() {
        System.setProperty("no.nav.brukerdialog.security.context.subjectHandlerImplementationClass",
                ThreadLocalSubjectHandler.class.getName());
        ThreadLocalSubjectHandler sh = new ThreadLocalSubjectHandler();

    }

    @Before
    public void initialize() throws Exception {
        when(aktorServiceMock.getAktorId(FNR)).thenReturn(of(AKTOR_ID));
        val res = new HentOppfoelgingsstatusResponse();
        res.setNavOppfoelgingsenhet(ENHET);
        when(oppfoelgingPortTypeMock.hentOppfoelgingsstatus(any())).thenReturn(res);
    }

    @Test
    public void startKvp() {
        kvpService.startKvp(FNR, START_BEGRUNNELSE);

        verify(pepClientMock, times(1)).sjekkLeseTilgangTilFnr(FNR);
        verify(kvpRepositoryMock, times(1)).startKvp(eq(AKTOR_ID), eq(ENHET), isNull(), eq(START_BEGRUNNELSE));
    }

    @Test
    public void stopKvp() {
        kvpService.stopKvp(FNR, STOP_BEGRUNNELSE);

        verify(pepClientMock, times(1)).sjekkLeseTilgangTilFnr(FNR);
        verify(kvpRepositoryMock, times(1)).stopKvp(eq(AKTOR_ID), isNull(), eq(STOP_BEGRUNNELSE));
    }

    @Test(expected = IngenTilgang.class)
    public void startKvpIkkeTilgang() {
        when(pepClientMock.sjekkLeseTilgangTilFnr(any())).thenThrow(IngenTilgang.class);

        kvpService.startKvp(FNR, START_BEGRUNNELSE);
    }

}