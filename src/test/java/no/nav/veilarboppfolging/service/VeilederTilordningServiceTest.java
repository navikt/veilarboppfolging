package no.nav.veilarboppfolging.service;

import no.nav.common.auth.context.AuthContext;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.UserRole;
import no.nav.common.test.auth.AuthTestUtils;
import no.nav.veilarboppfolging.controller.domain.OppfolgingFeedDTO;
import no.nav.veilarboppfolging.controller.domain.TilordneVeilederResponse;
import no.nav.veilarboppfolging.controller.domain.VeilederTilordning;
import no.nav.veilarboppfolging.feed.cjm.producer.FeedProducer;
import no.nav.veilarboppfolging.repository.VeilederHistorikkRepository;
import no.nav.veilarboppfolging.repository.VeilederTilordningerRepository;
import no.nav.veilarboppfolging.test.DbTestUtils;
import no.nav.veilarboppfolging.test.LocalH2Database;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.BadSqlGrammarException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static java.util.Arrays.asList;
import static no.nav.veilarboppfolging.service.VeilederTilordningService.kanTilordneVeileder;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.Silent.class)
public class VeilederTilordningServiceTest {

    @Mock
    private VeilederTilordningerRepository veilederTilordningerRepository;

    @Mock
    private VeilederHistorikkRepository veilederHistorikkRepository;

    @Mock
    private OppfolgingService oppfolgingService;

    @Mock
    private FeedProducer<OppfolgingFeedDTO> feed;

    @Mock
    private AuthService authService;

    @Mock
    private MetricsService metricsService;
    
    private VeilederTilordningService veilederTilordningService;
    
    @Before
    public void setup() {
        when(authService.harVeilederSkriveTilgangTilFnr(anyString(), anyString())).thenReturn(true);

        AuthContextHolder.withContext(AuthTestUtils.createAuthContext(UserRole.INTERN, "uid"), () -> {
            veilederTilordningService = new VeilederTilordningService(
                    metricsService,
                    veilederTilordningerRepository,
                    authService,
                    feed,
                    oppfolgingService,
                    veilederHistorikkRepository,
                    DbTestUtils.getTransactor(LocalH2Database.getDb()),
                    mock(KafkaProducerService.class));
        });
    }

    @Test
    public void skalKunneTildeleDersomOppgittVeilederErLikReellVeileder() {
        VeilederTilordning v = new VeilederTilordning().setBrukerFnr("FNR1").setFraVeilederId("A1").setTilVeilederId("B1");
        assertTrue(kanTilordneVeileder("A1", v));
    }

    @Test
    public void skalTildeleVeilederOmEksisterendeErNull() {
        VeilederTilordning v = new VeilederTilordning().setBrukerFnr("FNR1").setFraVeilederId(null).setTilVeilederId("A1");
        assertTrue(kanTilordneVeileder(null, v));
    }

    @Test
    public void skalIkkeTildeleVeilederOmEksisterendeErUlikFraVeileder() {
        VeilederTilordning v = new VeilederTilordning().setBrukerFnr("FNR1").setFraVeilederId("B1").setTilVeilederId("A1");
        assertFalse(kanTilordneVeileder("C1", v));
    }

    @Test
    public void skalIkkeTildeleVeilederOmEksisterendeErLikTilVeileder() {
        VeilederTilordning v = new VeilederTilordning().setBrukerFnr("FNR1").setFraVeilederId("C1").setTilVeilederId("C1");
        assertFalse(kanTilordneVeileder("C1", v));
    }

    @Test
    public void responsSkalInneholdeBrukereHvorVeilederIkkeHarTilgangEllerAbacFeiler() {
        List<VeilederTilordning> tilordninger = new ArrayList<>();

        VeilederTilordning harTilgang1 = new VeilederTilordning().setBrukerFnr("FNR1").setFraVeilederId("FRAVEILEDER1").setTilVeilederId("TILVEILEDER1");
        VeilederTilordning harIkkeTilgang1 = new VeilederTilordning().setBrukerFnr("FNR2").setFraVeilederId("FRAVEILEDER2").setTilVeilederId("TILVEILEDER2");
        VeilederTilordning harTilgang2 = new VeilederTilordning().setBrukerFnr("FNR3").setFraVeilederId("FRAVEILEDER3").setTilVeilederId("TILVEILEDER3");
        VeilederTilordning harIkkeTilgang2 = new VeilederTilordning().setBrukerFnr("FNR4").setFraVeilederId("FRAVEILEDER4").setTilVeilederId("TILVEILEDER4");

        tilordninger.add(harTilgang1);
        tilordninger.add(harIkkeTilgang1);
        tilordninger.add(harTilgang2);
        tilordninger.add(harIkkeTilgang2);

        doThrow(new RuntimeException()).when(authService).sjekkSkrivetilgangMedAktorId("AKTOERID2");
        doThrow(new RuntimeException()).when(authService).sjekkSkrivetilgangMedAktorId("AKTOERID4");

        when(authService.getAktorIdOrThrow("FNR1")).thenReturn("AKTOERID1");
        when(authService.getAktorIdOrThrow("FNR2")).thenReturn("AKTOERID2");
        when(authService.getAktorIdOrThrow("FNR3")).thenReturn("AKTOERID3");
        when(authService.getAktorIdOrThrow("FNR4")).thenReturn("AKTOERID4");

        TilordneVeilederResponse response = veilederTilordningService.tilordneVeiledere(tilordninger);
        List<VeilederTilordning> feilendeTilordninger = response.getFeilendeTilordninger();

        assertThat(feilendeTilordninger).contains(harIkkeTilgang1);
        assertThat(feilendeTilordninger).contains(harIkkeTilgang2);
        assertThat(feilendeTilordninger).doesNotContain(harTilgang1);
        assertThat(feilendeTilordninger).doesNotContain(harTilgang2);
    }

    @Test
    public void responsSkalInneholdeBrukereSomHarFeilFraVeileder() {
        List<VeilederTilordning> tilordninger = new ArrayList<>();

        VeilederTilordning kanTilordne1 = new VeilederTilordning().setBrukerFnr("FNR1").setFraVeilederId("FRAVEILEDER1").setTilVeilederId("TILVEILEDER1");
        VeilederTilordning kanIkkeTilordne1 = new VeilederTilordning().setBrukerFnr("FNR2").setFraVeilederId("FRAVEILEDER2").setTilVeilederId("TILVEILEDER2");
        VeilederTilordning kanTilordne2 = new VeilederTilordning().setBrukerFnr("FNR3").setFraVeilederId("FRAVEILEDER3").setTilVeilederId("TILVEILEDER3");
        VeilederTilordning kanIkkeTilordne2 = new VeilederTilordning().setBrukerFnr("FNR4").setFraVeilederId("FRAVEILEDER4").setTilVeilederId("TILVEILEDER4");

        tilordninger.add(kanTilordne1);
        tilordninger.add(kanIkkeTilordne1);
        tilordninger.add(kanTilordne2);
        tilordninger.add(kanIkkeTilordne2);


        when(authService.getAktorIdOrThrow("FNR1")).thenReturn("AKTOERID1");
        when(veilederTilordningerRepository.hentTilordningForAktoer("AKTOERID1"))
                .thenReturn("FRAVEILEDER1");

        when(authService.getAktorIdOrThrow("FNR2")).thenReturn("AKTOERID2");
        when(veilederTilordningerRepository.hentTilordningForAktoer("AKTOERID2"))
                .thenReturn("IKKE_FRAVEILEDER2");

        when(authService.getAktorIdOrThrow("FNR3")).thenReturn("AKTOERID3");
        when(veilederTilordningerRepository.hentTilordningForAktoer("AKTOERID3"))
                .thenReturn("FRAVEILEDER3");

        when(authService.getAktorIdOrThrow("FNR4")).thenReturn("AKTOERID4");
        when(veilederTilordningerRepository.hentTilordningForAktoer("AKTOERID4"))
                .thenReturn("IKKE_FRAVEILEDER4");

        TilordneVeilederResponse response = veilederTilordningService.tilordneVeiledere(tilordninger);
        List<VeilederTilordning> feilendeTilordninger = response.getFeilendeTilordninger();

        assertThat(feilendeTilordninger).contains(kanIkkeTilordne1);
        assertThat(feilendeTilordninger).contains(kanIkkeTilordne2);
        assertThat(feilendeTilordninger).doesNotContain(kanTilordne1);
        assertThat(feilendeTilordninger).doesNotContain(kanTilordne2);
    }

    @Test
    public void responsSkalInneholdeFeilendeTildelingNaarHentingAvVeilederFeiler() {
        List<VeilederTilordning> tilordninger = new ArrayList<>();

        VeilederTilordning tilordningOK1 = new VeilederTilordning().setBrukerFnr("FNR1").setFraVeilederId("FRAVEILEDER1").setTilVeilederId("TILVEILEDER1");
        VeilederTilordning tilordningOK2 = new VeilederTilordning().setBrukerFnr("FNR4").setFraVeilederId("FRAVEILEDER4").setTilVeilederId("TILVEILEDER4");
        VeilederTilordning tilordningERROR1 = new VeilederTilordning().setBrukerFnr("FNR2").setFraVeilederId("FRAVEILEDER2").setTilVeilederId("TILVEILEDER2");
        VeilederTilordning tilordningERROR2 = new VeilederTilordning().setBrukerFnr("FNR3").setFraVeilederId("FRAVEILEDER3").setTilVeilederId("TILVEILEDER3");

        tilordninger.add(tilordningOK1);
        tilordninger.add(tilordningERROR1);
        tilordninger.add(tilordningOK2);
        tilordninger.add(tilordningERROR2);

        when(authService.getAktorIdOrThrow("FNR1")).thenReturn("AKTOERID1");
        when(authService.getAktorIdOrThrow("FNR2")).thenReturn("AKTOERID2");
        when(authService.getAktorIdOrThrow("FNR3")).thenReturn("AKTOERID3");
        when(authService.getAktorIdOrThrow("FNR4")).thenReturn("AKTOERID4");

        when(veilederTilordningerRepository.hentTilordningForAktoer("AKTOERID2")).thenThrow(new BadSqlGrammarException("AKTOER", "Dette er bare en test", new SQLException()));

        when(veilederTilordningerRepository.hentTilordningForAktoer("AKTOERID3")).thenThrow(new BadSqlGrammarException("AKTOER", "Dette er bare en test", new SQLException()));

        TilordneVeilederResponse response = veilederTilordningService.tilordneVeiledere(tilordninger);
        List<VeilederTilordning> feilendeTilordninger = response.getFeilendeTilordninger();

        assertThat(feilendeTilordninger).contains(tilordningERROR1);
        assertThat(feilendeTilordninger).contains(tilordningERROR2);
        assertThat(feilendeTilordninger).doesNotContain(tilordningOK1);
        assertThat(feilendeTilordninger).doesNotContain(tilordningOK2);
    }

    @Test
    public void responsSkalInneholderFeilendeTildelingNaarOppdateringAvDBFeiler() {
        List<VeilederTilordning> tilordninger = new ArrayList<>();

        VeilederTilordning tilordningOK1 = new VeilederTilordning().setBrukerFnr("FNR1").setFraVeilederId("FRAVEILEDER1").setTilVeilederId("TILVEILEDER1");
        VeilederTilordning tilordningERROR1 = new VeilederTilordning().setBrukerFnr("FNR2").setFraVeilederId("FRAVEILEDER2").setTilVeilederId("TILVEILEDER2");
        VeilederTilordning tilordningOK2 = new VeilederTilordning().setBrukerFnr("FNR3").setFraVeilederId("FRAVEILEDER3").setTilVeilederId("TILVEILEDER3");
        VeilederTilordning tilordningERROR2 = new VeilederTilordning().setBrukerFnr("FNR4").setFraVeilederId("FRAVEILEDER4").setTilVeilederId("TILVEILEDER4");

        tilordninger.add(tilordningOK1);
        tilordninger.add(tilordningERROR1);
        tilordninger.add(tilordningOK2);
        tilordninger.add(tilordningERROR2);

        when(authService.getAktorIdOrThrow("FNR1")).thenReturn("AKTOERID1");
        when(authService.getAktorIdOrThrow("FNR2")).thenReturn("AKTOERID2");
        when(authService.getAktorIdOrThrow("FNR3")).thenReturn("AKTOERID3");
        when(authService.getAktorIdOrThrow("FNR4")).thenReturn("AKTOERID4");


        doThrow(new BadSqlGrammarException("AKTOER", "Dette er bare en test", new SQLException()))
                .when(veilederTilordningerRepository).upsertVeilederTilordning(eq("AKTOERID2"), anyString());

        doThrow(new BadSqlGrammarException("AKTOER", "Dette er bare en test", new SQLException()))
                .when(veilederTilordningerRepository).upsertVeilederTilordning(eq("AKTOERID4"), anyString());

        TilordneVeilederResponse response = veilederTilordningService.tilordneVeiledere(tilordninger);
        List<VeilederTilordning> feilendeTilordninger = response.getFeilendeTilordninger();

        assertThat(feilendeTilordninger).contains(tilordningERROR1);
        assertThat(feilendeTilordninger).contains(tilordningERROR2);
        assertThat(feilendeTilordninger).doesNotContain(tilordningOK1);
        assertThat(feilendeTilordninger).doesNotContain(tilordningOK2);
    }

    @Test
    public void skalInneholdeFeilendeTildeligNaarKallTilAktoerFeiler() {
        List<VeilederTilordning> tilordninger = new ArrayList<>();

        VeilederTilordning tilordningOK1 = new VeilederTilordning().setBrukerFnr("FNR1").setFraVeilederId("FRAVEILEDER1").setTilVeilederId("TILVEILEDER1");
        VeilederTilordning tilordningOK2 = new VeilederTilordning().setBrukerFnr("FNR4").setFraVeilederId("FRAVEILEDER4").setTilVeilederId("TILVEILEDER4");
        VeilederTilordning tilordningERROR1 = new VeilederTilordning().setBrukerFnr("FNR2").setFraVeilederId("FRAVEILEDER2").setTilVeilederId("TILVEILEDER2");
        VeilederTilordning tilordningERROR2 = new VeilederTilordning().setBrukerFnr("FNR3").setFraVeilederId("FRAVEILEDER3").setTilVeilederId("TILVEILEDER3");

        tilordninger.add(tilordningOK1);
        tilordninger.add(tilordningERROR1);
        tilordninger.add(tilordningOK2);
        tilordninger.add(tilordningERROR2);

        when(authService.getAktorIdOrThrow("FNR1")).thenReturn("AKTOERID1");
        when(authService.getAktorIdOrThrow("FNR2")).thenThrow(new RuntimeException());
        when(authService.getAktorIdOrThrow("FNR3")).thenThrow(new RuntimeException());
        when(authService.getAktorIdOrThrow("FNR4")).thenReturn("AKTOERID4");

        TilordneVeilederResponse response = veilederTilordningService.tilordneVeiledere(tilordninger);
        List<VeilederTilordning> feilendeTilordninger = response.getFeilendeTilordninger();

        assertThat(feilendeTilordninger).contains(tilordningERROR1);
        assertThat(feilendeTilordninger).contains(tilordningERROR2);
        assertThat(feilendeTilordninger).doesNotContain(tilordningOK1);
        assertThat(feilendeTilordninger).doesNotContain(tilordningOK2);
    }

    @Test
    public void skalGiFeilmeldingTilBrukerDersomUkjentFeilOppstaar() {
        List<VeilederTilordning> tilordninger = new ArrayList<>();

        VeilederTilordning tilordningERROR1 = new VeilederTilordning().setBrukerFnr("FNR1").setFraVeilederId("FRAVEILEDER1").setTilVeilederId("TILVEILEDER1");
        VeilederTilordning tilordningERROR2 = new VeilederTilordning().setBrukerFnr("FNR2").setFraVeilederId("FRAVEILEDER2").setTilVeilederId("TILVEILEDER2");

        tilordninger.add(tilordningERROR1);
        tilordninger.add(tilordningERROR2);

        when(authService.getAktorIdOrThrow("FNR1")).thenReturn("AKTOERID1");
        when(authService.getAktorIdOrThrow("FNR2")).thenReturn("AKTOERID2");
        doThrow(new RuntimeException()).when(authService).sjekkSkrivetilgangMedAktorId(anyString());

        TilordneVeilederResponse response = veilederTilordningService.tilordneVeiledere(tilordninger);
        List<VeilederTilordning> feilendeTilordninger = response.getFeilendeTilordninger();

        assertThat(feilendeTilordninger).contains(tilordningERROR1);
        assertThat(feilendeTilordninger).contains(tilordningERROR2);
    }

    @Test
    public void toOppdateringerSkalIkkeGaaIBeinaPaaHverandre() throws ExecutionException, InterruptedException {
        VeilederTilordning tilordningOKBruker1 = new VeilederTilordning().setBrukerFnr("FNR1").setFraVeilederId("FRAVEILEDER1").setTilVeilederId("TILVEILEDER1");
        VeilederTilordning tilordningERRORBruker2 = new VeilederTilordning().setBrukerFnr("FNR2").setFraVeilederId("FRAVEILEDER2").setTilVeilederId("TILVEILEDER2");

        doAnswer(invocation -> {
            if ("FNR1".equals(invocation.getArguments()[0])) {
                Thread.sleep(20);
                return null;
            }
            return null;
        }).when(authService).sjekkSkrivetilgangMedFnr(anyString());

        when(authService.getAktorIdOrThrow("FNR1")).thenReturn("AKTOERID1");

        doThrow(new RuntimeException()).when(authService).getAktorIdOrThrow("FNR2");

        //Starter to tråder som gjør to separate tilordninger gjennom samme portefoljeressurs. Dette simulerer
        //at to brukere kaller rest-operasjonen samtidig. Den første tilordningen tar lenger tid siden pep-kallet tar lenger tid.
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<TilordneVeilederResponse> response1 = pool.submit(portefoljeRessursCallable(veilederTilordningService, asList(tilordningOKBruker1)));
        Future<TilordneVeilederResponse> response2 = pool.submit(portefoljeRessursCallable(veilederTilordningService, asList(tilordningERRORBruker2)));

        List<VeilederTilordning> feilendeTilordninger1 = response1.get().getFeilendeTilordninger();
        List<VeilederTilordning> feilendeTilordninger2 = response2.get().getFeilendeTilordninger();

        assertThat(feilendeTilordninger1).isEmpty();
        assertThat(feilendeTilordninger2).contains(tilordningERRORBruker2);
    }

    private Callable<TilordneVeilederResponse> portefoljeRessursCallable(VeilederTilordningService veilederTilordningService, List<VeilederTilordning> tilordninger) {
        var context = new AuthContext(
                UserRole.INTERN,
                null
        );

        return () -> AuthContextHolder.withContext(context, () -> veilederTilordningService.tilordneVeiledere(tilordninger));
    }

    @Test
    public void feilIWebhookSkalIgnoreres() {
        List<VeilederTilordning> tilordninger = new ArrayList<>();

        VeilederTilordning tilordningOK1 = new VeilederTilordning().setBrukerFnr("FNR1").setFraVeilederId("FRAVEILEDER1").setTilVeilederId("TILVEILEDER1");

        tilordninger.add(tilordningOK1);

        when(authService.getAktorIdOrThrow("FNR1")).thenReturn("AKTOERID1");
        doThrow(new RuntimeException("Test")).when(feed).activateWebhook();

        TilordneVeilederResponse response = veilederTilordningService.tilordneVeiledere(tilordninger);
        List<VeilederTilordning> feilendeTilordninger = response.getFeilendeTilordninger();

        assertThat(feilendeTilordninger).isEmpty();
    }

    @Test
    public void noCallToDAOWhenAktoerIdServiceFails() {
        when(authService.getAktorIdOrThrow(anyString())).thenThrow(new RuntimeException("MOCK INGEN AKTOR ID"));
        veilederTilordningService.tilordneVeiledere(Collections.singletonList(testData()));
        verify(veilederTilordningerRepository, never()).upsertVeilederTilordning(anyString(), anyString());
    }

    private VeilederTilordning testData() {
        return new VeilederTilordning()
                .setFraVeilederId(null)
                .setTilVeilederId("4321")
                .setBrukerFnr("1234");
    }
}
