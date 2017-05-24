package no.nav.fo.veilarbsituasjon.rest;

import no.nav.brukerdialog.security.context.ThreadLocalSubjectHandler;
import no.nav.fo.veilarbsituasjon.db.BrukerRepository;
import no.nav.fo.veilarbsituasjon.domain.OppfolgingBruker;
import no.nav.fo.veilarbsituasjon.rest.domain.TilordneVeilederResponse;
import no.nav.fo.veilarbsituasjon.rest.domain.VeilederTilordning;
import no.nav.fo.veilarbsituasjon.services.AktoerIdService;
import no.nav.fo.veilarbsituasjon.services.PepClient;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.BadSqlGrammarException;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.core.Response;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PortefoljeRessursTest {


    @Mock
    private PepClient pepClient;

    @Mock
    private AktoerIdService aktoerIdService;

    @Mock
    private BrukerRepository brukerRepository;

    @InjectMocks
    private PortefoljeRessurs portefoljeRessurs;

    @Before
    public void beforeAll() {
        System.setProperty("no.nav.modig.core.context.subjectHandlerImplementationClass", ThreadLocalSubjectHandler.class.getName());
    }

    @Test
    public void skalTildeleVeileder() throws Exception {
        String fraVeileder = "AAAAAAA";
        String tilVeileder = "BBBBBBB";
        String eksisterendeVeileder = "AAAAAAA";
        boolean result = PortefoljeRessurs.kanSetteNyVeileder(fraVeileder, tilVeileder, eksisterendeVeileder);
        assertTrue(result);
    }

    @Test
    public void skalTildeleVeilederOmEksisterendeErNull() throws Exception {
        String fraVeileder = "AAAAAAA";
        String tilVeileder = "BBBBBBB";
        String eksisterendeVeileder = null;
        boolean result = PortefoljeRessurs.kanSetteNyVeileder(fraVeileder, tilVeileder, eksisterendeVeileder);
        assertTrue(result);
    }

    @Test
    public void skalIkkeTildeleVeilederOmEksisterendeErUlikFraVeileder() throws Exception {
        String fraVeileder = "AAAAAAA";
        String tilVeileder = "BBBBBBB";
        String eksisterendeVeileder = "CCCCCC";
        boolean result = PortefoljeRessurs.kanSetteNyVeileder(fraVeileder, tilVeileder, eksisterendeVeileder);
        assertFalse(result);
    }

    @Test
    public void skalIkkeTildeleVeilederOmTilVeilederErNull() throws Exception {
        String fraVeileder = "AAAAAAA";
        String tilVeileder = null;
        String eksisterendeVeileder = "CCCCCC";
        boolean result = PortefoljeRessurs.kanSetteNyVeileder(fraVeileder, tilVeileder, eksisterendeVeileder);
        assertFalse(result);
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

        when(aktoerIdService.findAktoerId("FNR1")).thenReturn("AKTOERID1");
        when(aktoerIdService.findAktoerId("FNR2")).thenReturn("AKTOERID2");
        when(aktoerIdService.findAktoerId("FNR3")).thenReturn("AKTOERID3");
        when(aktoerIdService.findAktoerId("FNR4")).thenReturn("AKTOERID4");

        when(pepClient.isServiceCallAllowed("FNR1")).thenReturn(true);
        when(pepClient.isServiceCallAllowed("FNR2")).thenThrow(NotAuthorizedException.class);
        when(pepClient.isServiceCallAllowed("FNR3")).thenReturn(true);
        when(pepClient.isServiceCallAllowed("FNR4")).thenThrow(PepException.class);


        Response response = portefoljeRessurs.postVeilederTilordninger(tilordninger);
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

        when(pepClient.isServiceCallAllowed(any(String.class))).thenReturn(true);

        when(aktoerIdService.findAktoerId("FNR1")).thenReturn("AKTOERID1");
        when(brukerRepository.hentVeilederForAktoer("AKTOERID1")).thenReturn("FRAVEILEDER1");

        when(aktoerIdService.findAktoerId("FNR2")).thenReturn("AKTOERID2");
        when(brukerRepository.hentVeilederForAktoer("AKTOERID2")).thenReturn("IKKE_FRAVEILEDER2");

        when(aktoerIdService.findAktoerId("FNR3")).thenReturn("AKTOERID3");
        when(brukerRepository.hentVeilederForAktoer("AKTOERID3")).thenReturn("FRAVEILEDER3");

        when(aktoerIdService.findAktoerId("FNR4")).thenReturn("AKTOERID4");
        when(brukerRepository.hentVeilederForAktoer("AKTOERID4")).thenReturn("IKKE_FRAVEILEDER4");


        Response response = portefoljeRessurs.postVeilederTilordninger(tilordninger);
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

        when(pepClient.isServiceCallAllowed(any(String.class))).thenReturn(true);

        when(aktoerIdService.findAktoerId("FNR1")).thenReturn("AKTOERID1");
        when(aktoerIdService.findAktoerId("FNR4")).thenReturn("AKTOERID4");

        when(aktoerIdService.findAktoerId("FNR2")).thenReturn("AKTOERID2");
        when(brukerRepository.hentVeilederForAktoer("AKTOERID2")).thenThrow(new BadSqlGrammarException("AKTOER","Dette er bare en test", new SQLException()));

        when(aktoerIdService.findAktoerId("FNR3")).thenReturn("AKTOERID3");
        when(brukerRepository.hentVeilederForAktoer("AKTOERID3")).thenThrow(new BadSqlGrammarException("AKTOER","Dette er bare en test", new SQLException()));

        Response response = portefoljeRessurs.postVeilederTilordninger(tilordninger);
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

        when(pepClient.isServiceCallAllowed(any(String.class))).thenReturn(true);

        when(aktoerIdService.findAktoerId("FNR1")).thenReturn("AKTOERID1");
        when(aktoerIdService.findAktoerId("FNR2")).thenReturn("AKTOERID2");
        when(aktoerIdService.findAktoerId("FNR3")).thenReturn("AKTOERID3");
        when(aktoerIdService.findAktoerId("FNR4")).thenReturn("AKTOERID4");


        doThrow(new BadSqlGrammarException("AKTOER","Dette er bare en test", new SQLException()))
                .when(brukerRepository).leggTilEllerOppdaterBruker(argThat(new IsOppfolgingsbrukerWithAktoerId("AKTOERID2")));

        doThrow(new BadSqlGrammarException("AKTOER","Dette er bare en test", new SQLException()))
                .when(brukerRepository).leggTilEllerOppdaterBruker(argThat(new IsOppfolgingsbrukerWithAktoerId("AKTOERID4")));

        Response response = portefoljeRessurs.postVeilederTilordninger(tilordninger);
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

        when(pepClient.isServiceCallAllowed(any(String.class))).thenReturn(true);
        when(aktoerIdService.findAktoerId("FNR3")).thenReturn(null);
        when(aktoerIdService.findAktoerId("FNR2")).thenReturn(null);
        when(aktoerIdService.findAktoerId("FNR1")).thenReturn("AKTOERID1");
        when(aktoerIdService.findAktoerId("FNR4")).thenReturn("AKTOERID4");

        Response response = portefoljeRessurs.postVeilederTilordninger(tilordninger);
        List<VeilederTilordning> feilendeTilordninger = ((TilordneVeilederResponse) response.getEntity()).getFeilendeTilordninger();

        assertThat(feilendeTilordninger).contains(tilordningERROR1);
        assertThat(feilendeTilordninger).contains(tilordningERROR2);
        assertThat(feilendeTilordninger).doesNotContain(tilordningOK1);
        assertThat(feilendeTilordninger).doesNotContain(tilordningOK2);
    }

    @Test
    public void skalInneholdeFeilendeTildeligNaarLeggePaaKoFeiler() throws Exception {
        List<VeilederTilordning> tilordninger = new ArrayList<>();

        VeilederTilordning tilordningERROR1 = new VeilederTilordning().setBrukerFnr("FNR1").setFraVeilederId("FRAVEILEDER1").setTilVeilederId("TILVEILEDER1");
        VeilederTilordning tilordningERROR2 = new VeilederTilordning().setBrukerFnr("FNR2").setFraVeilederId("FRAVEILEDER2").setTilVeilederId("TILVEILEDER2");

        tilordninger.add(tilordningERROR1);
        tilordninger.add(tilordningERROR2);

        when(pepClient.isServiceCallAllowed(any(String.class))).thenReturn(true);
        when(aktoerIdService.findAktoerId("FNR1")).thenReturn("AKTOERID1");
        when(aktoerIdService.findAktoerId("FNR2")).thenReturn("AKTOERID2");


        Response response = portefoljeRessurs.postVeilederTilordninger(tilordninger);
        List<VeilederTilordning> feilendeTilordninger = ((TilordneVeilederResponse) response.getEntity()).getFeilendeTilordninger();

        assertThat(feilendeTilordninger).contains(tilordningERROR1);
        assertThat(feilendeTilordninger).contains(tilordningERROR2);
    }

    @Test
    public void skalGiFeilmeldingTilBrukerDersomUkjentFeilOppstaar() throws Exception {
        List<VeilederTilordning> tilordninger = new ArrayList<>();

        VeilederTilordning tilordningERROR1 = new VeilederTilordning().setBrukerFnr("FNR1").setFraVeilederId("FRAVEILEDER1").setTilVeilederId("TILVEILEDER1");
        VeilederTilordning tilordningERROR2 = new VeilederTilordning().setBrukerFnr("FNR2").setFraVeilederId("FRAVEILEDER2").setTilVeilederId("TILVEILEDER2");

        tilordninger.add(tilordningERROR1);
        tilordninger.add(tilordningERROR2);

        when(pepClient.isServiceCallAllowed(any(String.class))).thenThrow(Exception.class);

        Response response = portefoljeRessurs.postVeilederTilordninger(tilordninger);
        List<VeilederTilordning> feilendeTilordninger = ((TilordneVeilederResponse) response.getEntity()).getFeilendeTilordninger();

        assertThat(feilendeTilordninger).contains(tilordningERROR1);
        assertThat(feilendeTilordninger).contains(tilordningERROR2);
    }

    class IsOppfolgingsbrukerWithAktoerId implements ArgumentMatcher<OppfolgingBruker> {
        private String aktoeridToMatch;

        IsOppfolgingsbrukerWithAktoerId(String aktoeridToMatch) {
            this.aktoeridToMatch = aktoeridToMatch;
        }
        public boolean matches(OppfolgingBruker oppfolgingBruker) {
            return aktoeridToMatch.equals(oppfolgingBruker.getAktoerid());
        }
    }
}