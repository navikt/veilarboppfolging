package no.nav.fo.veilarboppfolging.services;

import lombok.val;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.feil.UlovligHandling;
import no.nav.apiapp.security.veilarbabac.Bruker;
import no.nav.apiapp.security.veilarbabac.VeilarbAbacPepClient;
import no.nav.common.auth.SsoToken;
import no.nav.common.auth.Subject;
import no.nav.common.auth.SubjectHandler;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.EskaleringsvarselRepository;
import no.nav.fo.veilarboppfolging.db.KvpRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingsStatusRepository;
import no.nav.fo.veilarboppfolging.domain.OppfolgingTable;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingsstatusResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
    private VeilarbAbacPepClient pepClientMock;

    @Mock
    private OppfolgingsStatusRepository oppfolgingsStatusRepository;

    @Mock
    private EskaleringsvarselRepository eskaleringsvarselRepository;

    @InjectMocks
    private KvpService kvpService;

    private static final String FNR = "1234";
    private static final String AKTOR_ID = "12345";
    private static final Bruker BRUKER = Bruker.fraFnr(FNR).medAktoerIdSupplier(()->AKTOR_ID);
    private static final String ENHET = "1234";
    private static final String START_BEGRUNNELSE = "START_BEGRUNNELSE";
    private static final String STOP_BEGRUNNELSE = "STOP_BEGRUNNELSE";

    @Before
    public void initialize() throws Exception {
        when(aktorServiceMock.getAktorId(FNR)).thenReturn(of(AKTOR_ID));
        val res = new HentOppfoelgingsstatusResponse();
        res.setNavOppfoelgingsenhet(ENHET);
        when(oppfoelgingPortTypeMock.hentOppfoelgingsstatus(any())).thenReturn(res);

        when(oppfolgingsStatusRepository.fetch(AKTOR_ID)).thenReturn(new OppfolgingTable().setUnderOppfolging(true));

    }

    @Test(expected = UlovligHandling.class)
    public void start_kvp_uten_oppfolging_er_ulovlig_handling() {
        when(oppfolgingsStatusRepository.fetch(AKTOR_ID)).thenReturn(new OppfolgingTable().setUnderOppfolging(false));
        kvpService.startKvp(FNR, START_BEGRUNNELSE);
    }

    @Test(expected = UlovligHandling.class)
    public void start_kvp_uten_bruker_i_oppfolgingtabell_er_ulovlig_handling() {
        when(oppfolgingsStatusRepository.fetch(AKTOR_ID)).thenReturn(null);
        kvpService.startKvp(FNR, START_BEGRUNNELSE);
    }
    
    @Test
    public void startKvp()  {
        doReturn(true).when(pepClientMock).harTilgangTilEnhet(any());

        SubjectHandler.withSubject(new Subject(VEILEDER, InternBruker, SsoToken.oidcToken("token")),
                () -> kvpService.startKvp(FNR, START_BEGRUNNELSE)
        );

        verify(pepClientMock, times(1)).sjekkLesetilgangTilBruker(BRUKER);
        verify(kvpRepositoryMock, times(1)).startKvp(eq(AKTOR_ID), eq(ENHET), eq(VEILEDER), eq(START_BEGRUNNELSE));
        verify(pepClientMock, times(1)).harTilgangTilEnhet(ENHET);
    }

    @Test
    public void stopKvp()  {
        doReturn(true).when(pepClientMock).harTilgangTilEnhet(any());

        SubjectHandler.withSubject(new Subject(VEILEDER, InternBruker, SsoToken.oidcToken("token")),
                () -> kvpService.stopKvp(FNR, STOP_BEGRUNNELSE)
        );

        verify(pepClientMock, times(1)).sjekkLesetilgangTilBruker(BRUKER);
        verify(kvpRepositoryMock, times(1)).stopKvp(eq(AKTOR_ID), eq(VEILEDER), eq(STOP_BEGRUNNELSE), eq(NAV));
        verify(pepClientMock, times(1)).harTilgangTilEnhet(ENHET);
    }

    @Test
    public void stopKvp_avslutter_eskalering() throws PepException {
        when(oppfolgingsStatusRepository.fetch(AKTOR_ID)).thenReturn(new OppfolgingTable().setUnderOppfolging(true).setGjeldendeEskaleringsvarselId(1));
        SubjectHandler.withSubject(new Subject(VEILEDER, InternBruker, SsoToken.oidcToken("token")),
                () -> kvpService.stopKvp(FNR, STOP_BEGRUNNELSE)
        );

        verify(eskaleringsvarselRepository).finish(AKTOR_ID, 1, VEILEDER, KvpService.ESKALERING_AVSLUTTET_FORDI_KVP_BLE_AVSLUTTET);
        verify(kvpRepositoryMock, times(1)).stopKvp(eq(AKTOR_ID), eq(VEILEDER), eq(STOP_BEGRUNNELSE), eq(NAV));
    }
    
    @Test(expected = IngenTilgang.class)
    public void startKvpIkkeTilgang() {
        doThrow(IngenTilgang.class).when(pepClientMock).sjekkLesetilgangTilBruker(any());

        kvpService.startKvp(FNR, START_BEGRUNNELSE);
    }

    @Test(expected = IngenTilgang.class)
    public void startKvpInhenEnhetTilgang()  {
        doReturn(false).when(pepClientMock).harTilgangTilEnhet(any());

        kvpService.startKvp(FNR, START_BEGRUNNELSE);
    }

    @Test(expected = IngenTilgang.class)
    public void stoppKvpIkkeTilgang() {
        doThrow(IngenTilgang.class).when(pepClientMock).sjekkLesetilgangTilBruker(any());

        kvpService.stopKvp(FNR, STOP_BEGRUNNELSE);
    }

    @Test(expected = IngenTilgang.class)
    public void stopKvpInhenEnhetTilgang() throws PepException {
        doThrow(IngenTilgang.class).when(pepClientMock).harTilgangTilEnhet(any());

        kvpService.stopKvp(FNR, STOP_BEGRUNNELSE);
    }

}
