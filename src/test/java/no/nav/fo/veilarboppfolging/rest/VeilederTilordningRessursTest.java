package no.nav.fo.veilarboppfolging.rest;

import lombok.val;
import no.nav.apiapp.security.veilarbabac.Bruker;
import no.nav.apiapp.security.veilarbabac.VeilarbAbacPepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.feed.producer.FeedProducer;
import no.nav.fo.veilarboppfolging.db.VeilederTilordningerRepository;
import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingFeedDTO;
import no.nav.fo.veilarboppfolging.rest.domain.TilordneVeilederResponse;
import no.nav.fo.veilarboppfolging.rest.domain.VeilederTilordning;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.BadSqlGrammarException;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.core.Response;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static no.nav.fo.veilarboppfolging.rest.VeilederTilordningRessurs.kanTilordneFraVeileder;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class VeilederTilordningRessursTest {

    @Mock
    private VeilarbAbacPepClient pepClient;

    @Mock
    private AktorService aktorServiceMock;

    @Mock
    private VeilederTilordningerRepository veilederTilordningerRepository;

    @Mock
    private FeedProducer<OppfolgingFeedDTO> feed;

    @Mock
    private AutorisasjonService autorisasjonService;

    @InjectMocks
    private VeilederTilordningRessurs veilederTilordningRessurs;

    @Before
    public void setup() {
        when(autorisasjonService.harVeilederSkriveTilgangTilFnr(anyString(), anyString())).thenReturn(true);
    }

    @Test
    public void skalKunneTildeleDersomOppgittVeilederErLikReellVeileder() throws Exception {
        assertTrue(kanTilordneFraVeileder("AAAAAAA", "AAAAAAA"));
    }

    @Test
    public void skalTildeleVeilederOmEksisterendeErNull() throws Exception {
        assertTrue(kanTilordneFraVeileder(null, "AAAAAAA"));
    }

    @Test
    public void skalIkkeTildeleVeilederOmEksisterendeErUlikFraVeileder() throws Exception {
        assertFalse(kanTilordneFraVeileder("AAAAAAA", "CCCCCC"));
    }

    @Test
    public void responsSkalInneholdeBrukereHvorVeilederIkkeHarTilgangEllerAbacFeiler() throws Exception {
        List<VeilederTilordning> tilordninger = new ArrayList<>();

        VeilederTilordning harTilgang1 = new VeilederTilordning().setBrukerFnr("FNR1").setFraVeilederId("FRAVEILEDER1").setTilVeilederId("TILVEILEDER1");
        VeilederTilordning harIkkeTilgang1 = new VeilederTilordning().setBrukerFnr("FNR2").setFraVeilederId("FRAVEILEDER2").setTilVeilederId("TILVEILEDER2");
        VeilederTilordning harTilgang2 = new VeilederTilordning().setBrukerFnr("FNR3").setFraVeilederId("FRAVEILEDER3").setTilVeilederId("TILVEILEDER3");
        VeilederTilordning harIkkeTilgang2 = new VeilederTilordning().setBrukerFnr("FNR4").setFraVeilederId("FRAVEILEDER4").setTilVeilederId("TILVEILEDER4");

        tilordninger.add(harTilgang1);
        tilordninger.add(harIkkeTilgang1);
        tilordninger.add(harTilgang2);
        tilordninger.add(harIkkeTilgang2);

        doThrow(NotAuthorizedException.class).when (pepClient).sjekkLesetilgangTilBruker(bruker("FNR2"));
        doThrow(PepException.class).when(pepClient).sjekkLesetilgangTilBruker(bruker("FNR4"));

        when(aktorServiceMock.getAktorId("FNR1")).thenReturn(of("AKTOERID1"));
        when(aktorServiceMock.getAktorId("FNR3")).thenReturn(of("AKTOERID3"));


        Response response = veilederTilordningRessurs.postVeilederTilordninger(tilordninger);
        List<VeilederTilordning> feilendeTilordninger = ((TilordneVeilederResponse) response.getEntity()).getFeilendeTilordninger();

        assertThat(feilendeTilordninger).contains(harIkkeTilgang1);
        assertThat(feilendeTilordninger).contains(harIkkeTilgang2);
        assertThat(feilendeTilordninger).doesNotContain(harTilgang1);
        assertThat(feilendeTilordninger).doesNotContain(harTilgang2);
    }

    @Test
    public void responsSkalInneholdeBrukereSomHarFeilFraVeileder() throws Exception {
        List<VeilederTilordning> tilordninger = new ArrayList<>();

        VeilederTilordning kanTilordne1 = new VeilederTilordning().setBrukerFnr("FNR1").setFraVeilederId("FRAVEILEDER1").setTilVeilederId("TILVEILEDER1");
        VeilederTilordning kanIkkeTilordne1 = new VeilederTilordning().setBrukerFnr("FNR2").setFraVeilederId("FRAVEILEDER2").setTilVeilederId("TILVEILEDER2");
        VeilederTilordning kanTilordne2 = new VeilederTilordning().setBrukerFnr("FNR3").setFraVeilederId("FRAVEILEDER3").setTilVeilederId("TILVEILEDER3");
        VeilederTilordning kanIkkeTilordne2 = new VeilederTilordning().setBrukerFnr("FNR4").setFraVeilederId("FRAVEILEDER4").setTilVeilederId("TILVEILEDER4");

        tilordninger.add(kanTilordne1);
        tilordninger.add(kanIkkeTilordne1);
        tilordninger.add(kanTilordne2);
        tilordninger.add(kanIkkeTilordne2);

        val oppfolgingsBruker = OppfolgingFeedDTO
                .builder()
                .oppfolging(false);


        when(aktorServiceMock.getAktorId("FNR1")).thenReturn(of("AKTOERID1"));
        when(veilederTilordningerRepository.hentTilordningForAktoer("AKTOERID1"))
                .thenReturn("FRAVEILEDER1");

        when(aktorServiceMock.getAktorId("FNR2")).thenReturn(of("AKTOERID2"));
        when(veilederTilordningerRepository.hentTilordningForAktoer("AKTOERID2"))
                .thenReturn("IKKE_FRAVEILEDER2");

        when(aktorServiceMock.getAktorId("FNR3")).thenReturn(of("AKTOERID3"));
        when(veilederTilordningerRepository.hentTilordningForAktoer("AKTOERID3"))
                .thenReturn("FRAVEILEDER3");

        when(aktorServiceMock.getAktorId("FNR4")).thenReturn(of("AKTOERID4"));
        when(veilederTilordningerRepository.hentTilordningForAktoer("AKTOERID4"))
                .thenReturn("IKKE_FRAVEILEDER4");

        Response response = veilederTilordningRessurs.postVeilederTilordninger(tilordninger);
        List<VeilederTilordning> feilendeTilordninger = ((TilordneVeilederResponse) response.getEntity()).getFeilendeTilordninger();

        assertThat(feilendeTilordninger).contains(kanIkkeTilordne1);
        assertThat(feilendeTilordninger).contains(kanIkkeTilordne2);
        assertThat(feilendeTilordninger).doesNotContain(kanTilordne1);
        assertThat(feilendeTilordninger).doesNotContain(kanTilordne2);
    }

    @Test
    public void responsSkalInneholdeFeilendeTildelingNaarHentingAvVeilederFeiler() throws Exception {
        List<VeilederTilordning> tilordninger = new ArrayList<>();

        VeilederTilordning tilordningOK1 = new VeilederTilordning().setBrukerFnr("FNR1").setFraVeilederId("FRAVEILEDER1").setTilVeilederId("TILVEILEDER1");
        VeilederTilordning tilordningOK2 = new VeilederTilordning().setBrukerFnr("FNR4").setFraVeilederId("FRAVEILEDER4").setTilVeilederId("TILVEILEDER4");
        VeilederTilordning tilordningERROR1 = new VeilederTilordning().setBrukerFnr("FNR2").setFraVeilederId("FRAVEILEDER2").setTilVeilederId("TILVEILEDER2");
        VeilederTilordning tilordningERROR2 = new VeilederTilordning().setBrukerFnr("FNR3").setFraVeilederId("FRAVEILEDER3").setTilVeilederId("TILVEILEDER3");

        tilordninger.add(tilordningOK1);
        tilordninger.add(tilordningERROR1);
        tilordninger.add(tilordningOK2);
        tilordninger.add(tilordningERROR2);

        when(aktorServiceMock.getAktorId("FNR1")).thenReturn(of("AKTOERID1"));
        when(aktorServiceMock.getAktorId("FNR4")).thenReturn(of("AKTOERID4"));

        when(aktorServiceMock.getAktorId("FNR2")).thenReturn(of("AKTOERID2"));
        when(veilederTilordningerRepository.hentTilordningForAktoer("AKTOERID2")).thenThrow(new BadSqlGrammarException("AKTOER", "Dette er bare en test", new SQLException()));

        when(aktorServiceMock.getAktorId("FNR3")).thenReturn(of("AKTOERID3"));
        when(veilederTilordningerRepository.hentTilordningForAktoer("AKTOERID3")).thenThrow(new BadSqlGrammarException("AKTOER", "Dette er bare en test", new SQLException()));

        Response response = veilederTilordningRessurs.postVeilederTilordninger(tilordninger);
        List<VeilederTilordning> feilendeTilordninger = ((TilordneVeilederResponse) response.getEntity()).getFeilendeTilordninger();

        assertThat(feilendeTilordninger).contains(tilordningERROR1);
        assertThat(feilendeTilordninger).contains(tilordningERROR2);
        assertThat(feilendeTilordninger).doesNotContain(tilordningOK1);
        assertThat(feilendeTilordninger).doesNotContain(tilordningOK2);
    }

    @Test
    public void responsSkalInneholderFeilendeTildelingNaarOppdateringAvDBFeiler() throws Exception {
        List<VeilederTilordning> tilordninger = new ArrayList<>();

        VeilederTilordning tilordningOK1 = new VeilederTilordning().setBrukerFnr("FNR1").setFraVeilederId("FRAVEILEDER1").setTilVeilederId("TILVEILEDER1");
        VeilederTilordning tilordningERROR1 = new VeilederTilordning().setBrukerFnr("FNR2").setFraVeilederId("FRAVEILEDER2").setTilVeilederId("TILVEILEDER2");
        VeilederTilordning tilordningOK2 = new VeilederTilordning().setBrukerFnr("FNR3").setFraVeilederId("FRAVEILEDER3").setTilVeilederId("TILVEILEDER3");
        VeilederTilordning tilordningERROR2 = new VeilederTilordning().setBrukerFnr("FNR4").setFraVeilederId("FRAVEILEDER4").setTilVeilederId("TILVEILEDER4");

        tilordninger.add(tilordningOK1);
        tilordninger.add(tilordningERROR1);
        tilordninger.add(tilordningOK2);
        tilordninger.add(tilordningERROR2);

        when(aktorServiceMock.getAktorId("FNR1")).thenReturn(of("AKTOERID1"));
        when(aktorServiceMock.getAktorId("FNR2")).thenReturn(of("AKTOERID2"));
        when(aktorServiceMock.getAktorId("FNR3")).thenReturn(of("AKTOERID3"));
        when(aktorServiceMock.getAktorId("FNR4")).thenReturn(of("AKTOERID4"));


        doThrow(new BadSqlGrammarException("AKTOER", "Dette er bare en test", new SQLException()))
                .when(veilederTilordningerRepository).upsertVeilederTilordning(eq("AKTOERID2"), anyString());

        doThrow(new BadSqlGrammarException("AKTOER", "Dette er bare en test", new SQLException()))
                .when(veilederTilordningerRepository).upsertVeilederTilordning(eq("AKTOERID4"), anyString());

        Response response = veilederTilordningRessurs.postVeilederTilordninger(tilordninger);
        List<VeilederTilordning> feilendeTilordninger = ((TilordneVeilederResponse) response.getEntity()).getFeilendeTilordninger();

        assertThat(feilendeTilordninger).contains(tilordningERROR1);
        assertThat(feilendeTilordninger).contains(tilordningERROR2);
        assertThat(feilendeTilordninger).doesNotContain(tilordningOK1);
        assertThat(feilendeTilordninger).doesNotContain(tilordningOK2);
    }

    @Test
    public void skalInneholdeFeilendeTildeligNaarKallTilAktoerFeiler() throws Exception {
        List<VeilederTilordning> tilordninger = new ArrayList<>();

        VeilederTilordning tilordningOK1 = new VeilederTilordning().setBrukerFnr("FNR1").setFraVeilederId("FRAVEILEDER1").setTilVeilederId("TILVEILEDER1");
        VeilederTilordning tilordningOK2 = new VeilederTilordning().setBrukerFnr("FNR4").setFraVeilederId("FRAVEILEDER4").setTilVeilederId("TILVEILEDER4");
        VeilederTilordning tilordningERROR1 = new VeilederTilordning().setBrukerFnr("FNR2").setFraVeilederId("FRAVEILEDER2").setTilVeilederId("TILVEILEDER2");
        VeilederTilordning tilordningERROR2 = new VeilederTilordning().setBrukerFnr("FNR3").setFraVeilederId("FRAVEILEDER3").setTilVeilederId("TILVEILEDER3");

        tilordninger.add(tilordningOK1);
        tilordninger.add(tilordningERROR1);
        tilordninger.add(tilordningOK2);
        tilordninger.add(tilordningERROR2);

        when(aktorServiceMock.getAktorId("FNR3")).thenReturn(empty());
        when(aktorServiceMock.getAktorId("FNR2")).thenReturn(empty());
        when(aktorServiceMock.getAktorId("FNR1")).thenReturn(of("AKTOERID1"));
        when(aktorServiceMock.getAktorId("FNR4")).thenReturn(of("AKTOERID4"));

        Response response = veilederTilordningRessurs.postVeilederTilordninger(tilordninger);
        List<VeilederTilordning> feilendeTilordninger = ((TilordneVeilederResponse) response.getEntity()).getFeilendeTilordninger();

        assertThat(feilendeTilordninger).contains(tilordningERROR1);
        assertThat(feilendeTilordninger).contains(tilordningERROR2);
        assertThat(feilendeTilordninger).doesNotContain(tilordningOK1);
        assertThat(feilendeTilordninger).doesNotContain(tilordningOK2);
    }

    @Test
    public void skalGiFeilmeldingTilBrukerDersomUkjentFeilOppstaar() throws Exception {
        List<VeilederTilordning> tilordninger = new ArrayList<>();

        VeilederTilordning tilordningERROR1 = new VeilederTilordning().setBrukerFnr("FNR1").setFraVeilederId("FRAVEILEDER1").setTilVeilederId("TILVEILEDER1");
        VeilederTilordning tilordningERROR2 = new VeilederTilordning().setBrukerFnr("FNR2").setFraVeilederId("FRAVEILEDER2").setTilVeilederId("TILVEILEDER2");

        tilordninger.add(tilordningERROR1);
        tilordninger.add(tilordningERROR2);

        doThrow(Exception.class).when(pepClient).sjekkLesetilgangTilBruker(any(Bruker.class));

        Response response = veilederTilordningRessurs.postVeilederTilordninger(tilordninger);
        List<VeilederTilordning> feilendeTilordninger = ((TilordneVeilederResponse) response.getEntity()).getFeilendeTilordninger();

        assertThat(feilendeTilordninger).contains(tilordningERROR1);
        assertThat(feilendeTilordninger).contains(tilordningERROR2);
    }

    @Test
    public void toOppdateringerSkalIkkeGaaIBeinaPaaHverandre() throws Exception {

        VeilederTilordning tilordningOKBruker1 = new VeilederTilordning().setBrukerFnr("FNR1").setFraVeilederId("FRAVEILEDER1").setTilVeilederId("TILVEILEDER1");
        VeilederTilordning tilordningERRORBruker2 = new VeilederTilordning().setBrukerFnr("FNR2").setFraVeilederId("FRAVEILEDER2").setTilVeilederId("TILVEILEDER2");

        doAnswer(invocation -> {
            //Simulerer at pep-kallet tar noe tid for fnr1
            if ("FNR1".equals(invocation.getArguments()[0])) {
                Thread.sleep(20);
                return null;
            }
            return null;
        }).when(pepClient).sjekkLesetilgangTilBruker(any(Bruker.class));

        when(aktorServiceMock.getAktorId("FNR1")).thenReturn(of("AKTOERID1"));

        //Starter to tråder som gjør to separate tilordninger gjennom samme portefoljeressurs. Dette simulerer
        //at to brukere kaller rest-operasjonen samtidig. Den første tilordningen tar lenger tid siden pep-kallet tar lenger tid.
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<Response> response1 = pool.submit(portefoljeRessursCallable(veilederTilordningRessurs, asList(tilordningOKBruker1)));
        Future<Response> response2 = pool.submit(portefoljeRessursCallable(veilederTilordningRessurs, asList(tilordningERRORBruker2)));

        List<VeilederTilordning> feilendeTilordninger1 = ((TilordneVeilederResponse) response1.get().getEntity()).getFeilendeTilordninger();
        List<VeilederTilordning> feilendeTilordninger2 = ((TilordneVeilederResponse) response2.get().getEntity()).getFeilendeTilordninger();

        assertThat(feilendeTilordninger1).isEmpty();
        assertThat(feilendeTilordninger2).contains(tilordningERRORBruker2);
    }

    private Callable<Response> portefoljeRessursCallable(VeilederTilordningRessurs veilederTilordningRessurs, List<VeilederTilordning> tilordninger) {
        return new Callable<Response>() {

            @Override
            public Response call() throws Exception {
                return veilederTilordningRessurs.postVeilederTilordninger(tilordninger);
            }
        };
    }

    @Test
    public void feilIWebhookSkalIgnoreres() throws Exception {

        List<VeilederTilordning> tilordninger = new ArrayList<>();

        VeilederTilordning tilordningOK1 = new VeilederTilordning().setBrukerFnr("FNR1").setFraVeilederId("FRAVEILEDER1").setTilVeilederId("TILVEILEDER1");

        tilordninger.add(tilordningOK1);

        when(aktorServiceMock.getAktorId("FNR1")).thenReturn(of("AKTOERID1"));
        doThrow(new RuntimeException("Test")).when(feed).activateWebhook();

        Response response = veilederTilordningRessurs.postVeilederTilordninger(tilordninger);
        List<VeilederTilordning> feilendeTilordninger = ((TilordneVeilederResponse) response.getEntity()).getFeilendeTilordninger();

        assertThat(feilendeTilordninger).isEmpty();
    }

    @Test
    public void portefoljeRessursMustCallDAOwithAktoerIdToVeileder() throws PepException {
        when(aktorServiceMock.getAktorId(any(String.class))).thenReturn(of("AKTOERID"));
        veilederTilordningRessurs.postVeilederTilordninger(Collections.singletonList(testData()));
        verify(veilederTilordningerRepository, times(1)).upsertVeilederTilordning(anyString(), anyString());
    }

    @Test
    public void noCallToDAOWhenAktoerIdServiceFails() {
        when(aktorServiceMock.getAktorId(any(String.class))).thenReturn(empty());
        veilederTilordningRessurs.postVeilederTilordninger(Collections.singletonList(testData()));
        verify(veilederTilordningerRepository, never()).upsertVeilederTilordning(anyString(), anyString());
    }

    private VeilederTilordning testData() {
        return new VeilederTilordning()
                .setFraVeilederId(null)
                .setTilVeilederId("4321")
                .setBrukerFnr("1234");
    }

    private Bruker bruker(String fnr) {
        return Bruker.fraFnr(fnr).medAktoerIdSupplier(()->null);
    }
}