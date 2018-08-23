package no.nav.fo.veilarboppfolging.services;

import io.vavr.collection.Stream;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarbaktivitet.domain.AktivitetStatus;
import no.nav.fo.veilarbaktivitet.domain.arena.ArenaAktivitetDTO;
import no.nav.fo.veilarboppfolging.db.OppfolgingRepository;
import no.nav.fo.veilarboppfolging.domain.*;
import no.nav.fo.veilarboppfolging.vilkar.VilkarService;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.DigitalKontaktinformasjonV1;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.HentDigitalKontaktinformasjonKontaktinformasjonIkkeFunnet;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.HentDigitalKontaktinformasjonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.WSKontaktinformasjon;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.WSHentDigitalKontaktinformasjonRequest;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.WSHentDigitalKontaktinformasjonResponse;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.YtelseskontraktV3;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.informasjon.ytelseskontrakt.WSYtelseskontrakt;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.WSHentYtelseskontraktListeRequest;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.WSHentYtelseskontraktListeResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static no.nav.fo.veilarboppfolging.domain.KodeverkBruker.NAV;
import static no.nav.fo.veilarboppfolging.domain.VilkarStatus.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OppfolgingServiceTest {

    @Mock
    private DigitalKontaktinformasjonV1 digitalKontaktinformasjonV1Mock;

    @Mock
    private OppfolgingRepository oppfolgingRepositoryMock;

    @Mock
    private AktorService aktorServiceMock;

    @Mock
    private VilkarService vilkarServiceMock;

    @Mock
    private ArenaOppfolgingService arenaOppfolgingService;

    @Mock
    private PepClient pepClientMock;

    @Mock
    private VeilarbaktivtetService veilarbaktivtetService;

    @Mock
    private YtelseskontraktV3 ytelseskontraktV3;

    @Mock(answer = Answers.RETURNS_MOCKS)
    private OppfolgingResolver.OppfolgingResolverDependencies oppfolgingResolverDependencies;

    private static final String FNR = "fnr";
    private static final String AKTOR_ID = "aktorId";
    private static final String ENHET = "0100";
    private static final String VEILEDER = "Z990000";
    private static final String BEGRUNNELSE = "begrunnelse";

    @InjectMocks
    private OppfolgingService oppfolgingService;

    private Oppfolging oppfolging = new Oppfolging().setAktorId(AKTOR_ID);
    private ArenaOppfolging arenaOppfolging;
    private WSKontaktinformasjon wsKontaktinformasjon = new WSKontaktinformasjon();

    @Before
    public void setup() throws Exception {
        arenaOppfolging = new ArenaOppfolging();
        when(oppfolgingRepositoryMock.opprettOppfolging(anyString())).thenReturn(oppfolging);

        doAnswer((a) -> oppfolging.setUnderOppfolging(true)).when(oppfolgingRepositoryMock).startOppfolgingHvisIkkeAlleredeStartet(anyString());

        when(arenaOppfolgingService.hentArenaOppfolging(any(String.class)))
                .thenReturn(arenaOppfolging);
        when(digitalKontaktinformasjonV1Mock.hentDigitalKontaktinformasjon(any(WSHentDigitalKontaktinformasjonRequest.class)))
                .thenReturn(new WSHentDigitalKontaktinformasjonResponse()
                        .withDigitalKontaktinformasjon(wsKontaktinformasjon));
        when(vilkarServiceMock.getVilkar(any(VilkarService.VilkarType.class), any())).thenReturn("Gjeldene Vilkar");
        when(aktorServiceMock.getAktorId(FNR)).thenReturn(of(AKTOR_ID));

        when(oppfolgingResolverDependencies.getAktorService()).thenReturn(aktorServiceMock);
        when(oppfolgingResolverDependencies.getOppfolgingRepository()).thenReturn(oppfolgingRepositoryMock);
        when(oppfolgingResolverDependencies.getArenaOppfolgingService()).thenReturn(arenaOppfolgingService);
        when(oppfolgingResolverDependencies.getDigitalKontaktinformasjonV1()).thenReturn(digitalKontaktinformasjonV1Mock);
        when(oppfolgingResolverDependencies.getVilkarService()).thenReturn(vilkarServiceMock);
        when(oppfolgingResolverDependencies.getPepClient()).thenReturn(pepClientMock);
        when(oppfolgingResolverDependencies.getVeilarbaktivtetService()).thenReturn(veilarbaktivtetService);
        when(oppfolgingResolverDependencies.getYtelseskontraktV3()).thenReturn(ytelseskontraktV3);
        gittOppfolgingStatus("", "");
    }

    @Test(expected = IngenTilgang.class)
    @SneakyThrows
    public void start_oppfolging_uten_enhet_tilgang() {
        doThrow(IngenTilgang.class).when(pepClientMock).sjekkTilgangTilEnhet(any());
        oppfolgingService.startOppfolging(FNR);
    }

    @Test(expected = IngenTilgang.class)
    @SneakyThrows
    public void avslutt_oppfolging_uten_enhet_tilgang() {
        doThrow(IngenTilgang.class).when(pepClientMock).sjekkTilgangTilEnhet(any());
        oppfolgingService.avsluttOppfolging(FNR, VEILEDER, BEGRUNNELSE);
    }

    @Test(expected = IngenTilgang.class)
    @SneakyThrows
    public void sett_manuell_uten_enhet_tilgang() {
        doThrow(IngenTilgang.class).when(pepClientMock).sjekkTilgangTilEnhet(any());
        oppfolgingService.oppdaterManuellStatus(FNR, true, BEGRUNNELSE, NAV, VEILEDER);
    }

    @Test(expected = IngenTilgang.class)
    @SneakyThrows
    public void settDigital_uten_enhet_tilgang() {
        doThrow(IngenTilgang.class).when(pepClientMock).sjekkTilgangTilEnhet(any());
        oppfolgingService.settDigitalBruker(FNR);
    }

    @Test(expected = IngenTilgang.class)
    @SneakyThrows
    public void start_eskalering_uten_enhet_tilgang() {
        doThrow(IngenTilgang.class).when(pepClientMock).sjekkTilgangTilEnhet(any());
        oppfolgingService.startEskalering(FNR, BEGRUNNELSE, 1L);
    }

    @Test(expected = IngenTilgang.class)
    @SneakyThrows
    public void stopp_eskalering_uten_enhet_tilgang() {
        doThrow(IngenTilgang.class).when(pepClientMock).sjekkTilgangTilEnhet(any());
        oppfolgingService.stoppEskalering(FNR, BEGRUNNELSE);
    }

    @Test
    public void medEnhetTilgang() throws Exception {
        when(pepClientMock.harTilgangTilEnhet(ENHET)).thenReturn(true);

        gittAktor();
        gittEnhet(ENHET);

        VeilederTilgang veilederTilgang = oppfolgingService.hentVeilederTilgang(FNR);
        assertThat(veilederTilgang.isTilgangTilBrukersKontor(), equalTo(true));
    }

    @Test
    public void utenEnhetTilgang() throws Exception {
        when(pepClientMock.harTilgangTilEnhet(anyString())).thenReturn(false);

        gittAktor();
        gittEnhet(ENHET);

        VeilederTilgang veilederTilgang = oppfolgingService.hentVeilederTilgang(FNR);
        assertThat(veilederTilgang.isTilgangTilBrukersKontor(), equalTo(false));
    }

    @Test
    public void ukjentAktor() throws Exception {
        doThrow(IllegalArgumentException.class).when(aktorServiceMock).getAktorId(FNR);
        assertThrows(IllegalArgumentException.class, this::hentOppfolgingStatus);
    }

    @Test
    public void riktigFnr() throws Exception {
        gittAktor();
        gittOppfolging(oppfolging);

        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();
        assertThat(oppfolgingStatusData.fnr, equalTo(FNR));
    }

    @Test
    public void hentOppfolgingStatus_brukerSomIkkeErUnderOppfolgingOppdateresIkkeDersomIkkeUnderOppfolgingIArena() throws Exception {
        gittAktor();
        gittOppfolging(oppfolging);
        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();

        verify(oppfolgingRepositoryMock, never()).startOppfolgingHvisIkkeAlleredeStartet(anyString());
        assertThat(oppfolgingStatusData.underOppfolging, is(false));
    }

    @Test
    public void hentOppfolgingStatus_brukerSomIkkeErUnderOppfolgingSettesUnderOppfolgingDersomArenaHarRiktigStatus() throws Exception {
        gittAktor();
        gittOppfolging(oppfolging);
        gittOppfolgingStatus("ARBS", "");

        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();

        verify(oppfolgingRepositoryMock).startOppfolgingHvisIkkeAlleredeStartet(AKTOR_ID);
        assertThat(oppfolgingStatusData.underOppfolging, is(true));
    }

    @Test
    public void hentOppfolgingStatus_brukerSomErUnderOppfolgingMeldesUtDersomIservMerEnn28Dager() throws Exception {
        gittAktor();
        oppfolging.setUnderOppfolging(true);
        gittOppfolging(oppfolging);
        gittInaktivOppfolgingStatus(LocalDate.now().minusDays(29), null);

        hentOppfolgingStatus();

        verify(oppfolgingRepositoryMock).avsluttOppfolging(eq(AKTOR_ID), eq(null), any(String.class));
    }
    
    private void gittInaktivOppfolgingStatus(LocalDate iservDato, Boolean kanEnkeltReaktiveres) {
        arenaOppfolging.setFormidlingsgruppe("ISERV");
        arenaOppfolging.setInaktiveringsdato(iservDato);
        arenaOppfolging.setKanEnkeltReaktiveres(kanEnkeltReaktiveres);
    }

    @Test
    public void hentOppfolgingStatus_brukerSomErUnderOppfolgingSkalReaktiveresDersomIservMindreEnn28Dager() throws Exception {
        gittAktor();
        oppfolging.setUnderOppfolging(true);
        gittOppfolging(oppfolging);
        gittInaktivOppfolgingStatus(LocalDate.now().minusDays(27), null);

        OppfolgingStatusData status = hentOppfolgingStatus();

        assertThat(status.kanReaktiveres, is(true));
        assertThat(status.inaktivIArena, is(true));
    }

    @Test
    public void hentOppfolgingStatus_brukerSomErUnderOppfolgingMeldesUtDersomArenaSierReaktiveringIkkeErMulig() throws Exception {
        gittAktor();
        oppfolging.setUnderOppfolging(true);
        gittOppfolging(oppfolging);
        gittInaktivOppfolgingStatus(LocalDate.now().minusDays(27), false);

        hentOppfolgingStatus();

        verify(oppfolgingRepositoryMock).avsluttOppfolging(eq(AKTOR_ID), eq(null), any(String.class));
    }
     
    @Test
    public void hentOppfolgingStatus_brukerSomErUnderOppfolgingSkalReaktiveresDersomArenaSierReaktiveringErMulig() throws Exception {
        gittAktor();
        oppfolging.setUnderOppfolging(true);
        gittOppfolging(oppfolging);
        gittInaktivOppfolgingStatus(LocalDate.now().minusDays(29), true);

        OppfolgingStatusData status = hentOppfolgingStatus();

        assertThat(status.kanReaktiveres, is(true));
        assertThat(status.inaktivIArena, is(true));
    }
    
    @Test
    public void utenReservasjon() throws Exception {
        gittAktor();

        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();

        assertThat(oppfolgingStatusData.reservasjonKRR, is(false));
    }

    @Test
    public void utenKontaktInformasjon() throws Exception {
        gittAktor();
        gittKRRFeil(HentDigitalKontaktinformasjonKontaktinformasjonIkkeFunnet.class);
        gittOppfolgingStatus("ARBS", "");

        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();

        assertThat(oppfolgingStatusData.reservasjonKRR, is(true));
    }

    @Test
    public void personIkkeFunnet() throws Exception {
        gittAktor();
        gittKRRFeil(HentDigitalKontaktinformasjonPersonIkkeFunnet.class);
        gittOppfolgingStatus("ARBS", "");

        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();

        assertThat(oppfolgingStatusData.reservasjonKRR, is(true));
    }

    @Test
    public void medReservasjonOgUnderOppfolging() throws Exception {
        gittAktor();
        gittReservasjon("true");
        gittOppfolgingStatus("ARBS", "");

        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();

        assertThat(oppfolgingStatusData.reservasjonKRR, is(true));
    }

    @Test
    public void underOppfolging() throws Exception {
        gittAktor();
        gittOppfolging(oppfolging);
        gittOppfolgingStatus("ARBS", "");

        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();

        assertThat(oppfolgingStatusData.underOppfolging, is(true));
    }

    @Test
    public void aksepterVilkar() throws Exception {
        gittAktor();
        gittOppfolging(oppfolging);

        assertThat(hentOppfolgingStatus().vilkarMaBesvares, is(true));

        besvarVilkar(GODKJENT, hentGjeldendeVilkar());

        assertThat(hentOppfolgingStatus().vilkarMaBesvares, is(false));
    }

    @Test
    public void avslaaVilkar() throws Exception {
        gittAktor();
        gittOppfolging(oppfolging);

        assertThat(hentOppfolgingStatus().vilkarMaBesvares, is(true));

        besvarVilkar(AVSLATT, hentGjeldendeVilkar());

        assertThat(hentOppfolgingStatus().vilkarMaBesvares, is(true));
    }

    @Test
    public void akseptererFeilVilkar() throws Exception {
        gittAktor();
        Brukervilkar feilVilkar = new Brukervilkar().setTekst("feilVilkar").setHash("HASH");
        besvarVilkar(GODKJENT, feilVilkar);

        assertThat(hentOppfolgingStatus().vilkarMaBesvares, is(true));
    }

    @Test
    public void vilkarIkkeBesvart() throws Exception {
        gittAktor();

        besvarVilkar(IKKE_BESVART, hentGjeldendeVilkar());

        assertThat(hentOppfolgingStatus().vilkarMaBesvares, is(true));
    }

    @Test
    public void ikkeArbeidssokerUnderOppfolging() throws Exception {
        gittAktor();
        gittOppfolging(oppfolging);
        gittOppfolgingStatus("IARBS", "BATT");

        val oppfolgingOgVilkarStatus = hentOppfolgingStatus();

        assertThat(oppfolgingOgVilkarStatus.underOppfolging, is(true));
    }

    @Test
    public void ikkeArbeidssokerIkkeUnderOppfolging() throws Exception {
        gittAktor();
        gittOppfolging(oppfolging);
        gittOppfolgingStatus("IARBS", "");

        val oppfolgingOgVilkarStatus = hentOppfolgingStatus();

        assertThat(oppfolgingOgVilkarStatus.underOppfolging, is(false));
    }

    @Test
    public void kanIkkeAvslutteNarManIkkeErUnderOppfolging() throws Exception {
        gittAktor();
        gittOppfolging(oppfolging.setUnderOppfolging(false));
        gittYtelserMedStatus();

        OppfolgingStatusData oppfolgingStatusData = oppfolgingService.hentAvslutningStatus(FNR);
        AvslutningStatusData avslutningStatusData = oppfolgingStatusData.avslutningStatusData;

        assertThat(avslutningStatusData.kanAvslutte, is(false));
    }

    @Test
    public void kanIkkeAvslutteNarManIkkeErUnderOppfolgingIArena() throws Exception {
        gittAktor();
        gittOppfolging(oppfolging.setUnderOppfolging(true));
        gittOppfolgingStatus("ARBS", null);
        gittYtelserMedStatus();

        OppfolgingStatusData oppfolgingStatusData = oppfolgingService.hentAvslutningStatus(FNR);
        AvslutningStatusData avslutningStatusData = oppfolgingStatusData.avslutningStatusData;

        assertThat(avslutningStatusData.kanAvslutte, is(false));
    }

    @Test
    public void kanIkkeAvslutteMedAktiveTiltak() throws Exception {
        gittAktor();
        gittOppfolging(oppfolging.setUnderOppfolging(true));
        gittOppfolgingStatus("IARBS", "VURDI");
        gittAktiveTiltak();
        gittYtelserMedStatus();

        OppfolgingStatusData oppfolgingStatusData = oppfolgingService.hentAvslutningStatus(FNR);
        AvslutningStatusData avslutningStatusData = oppfolgingStatusData.avslutningStatusData;

        assertThat(avslutningStatusData.kanAvslutte, is(false));
    }

    @Test
    public void kanAvslutteMedVarselOmAktiveYtelser() throws Exception {
        gittAktor();
        gittOppfolging(oppfolging.setUnderOppfolging(true));
        gittOppfolgingStatus("IARBS", "VURDI");
        gittIngenAktiveTiltak();
        gittYtelserMedStatus("Inaktiv", "Aktiv");

        OppfolgingStatusData oppfolgingStatusData = oppfolgingService.hentAvslutningStatus(FNR);
        AvslutningStatusData avslutningStatusData = oppfolgingStatusData.avslutningStatusData;

        assertThat(avslutningStatusData.kanAvslutte, is(true));
        assertThat(avslutningStatusData.harYtelser, is(true));
    }

    private void besvarVilkar(VilkarStatus vilkarStatus, Brukervilkar vilkar) {
        gittOppfolging(oppfolging.setGjeldendeBrukervilkar(
                new Brukervilkar(
                        oppfolging.getAktorId(),
                        new Timestamp(currentTimeMillis()),
                        vilkarStatus,
                        vilkar.getTekst(),
                        vilkar.getHash()
                ))
        );
    }

    private Brukervilkar hentGjeldendeVilkar() throws Exception {
        return oppfolgingService.hentVilkar(FNR);
    }

    private void gittOppfolgingStatus(String formidlingskode, String kvalifiseringsgruppekode) {
        arenaOppfolging.setFormidlingsgruppe(formidlingskode);
        arenaOppfolging.setServicegruppe(kvalifiseringsgruppekode);
    }

    private void gittEnhet(String enhet) {
        arenaOppfolging.setOppfolgingsenhet(enhet);
    }

    private OppfolgingStatusData hentOppfolgingStatus() throws Exception {
        return oppfolgingService.hentOppfolgingsStatus(FNR);
    }

    private void gittOppfolging(Oppfolging oppfolging) {
        when(oppfolgingRepositoryMock.hentOppfolging(AKTOR_ID)).thenReturn(Optional.of(oppfolging));
    }

    private void gittAktor() {
        when(aktorServiceMock.getAktorId(FNR)).thenReturn(of(AKTOR_ID));
    }

    private void gittReservasjon(String reservasjon) {
        wsKontaktinformasjon.setReservasjon(reservasjon);
    }

    private void gittKRRFeil(Class<? extends Exception> aClass) throws Exception {
        when(digitalKontaktinformasjonV1Mock.hentDigitalKontaktinformasjon(any(WSHentDigitalKontaktinformasjonRequest.class))).thenThrow(aClass);
    }

    private void gittAktiveTiltak() {
        when(veilarbaktivtetService.hentArenaAktiviteter(FNR)).thenReturn(
                asList(
                        new ArenaAktivitetDTO().setStatus(AktivitetStatus.GJENNOMFORES),
                        new ArenaAktivitetDTO().setStatus(AktivitetStatus.PLANLAGT)
                )
        );
    }

    private void gittIngenAktiveTiltak() {
        when(veilarbaktivtetService.hentArenaAktiviteter(FNR)).thenReturn(
                asList(
                        new ArenaAktivitetDTO().setStatus(AktivitetStatus.AVBRUTT),
                        new ArenaAktivitetDTO().setStatus(AktivitetStatus.FULLFORT)
                )
        );
    }

    private void gittYtelserMedStatus(String... statuser) throws Exception {
        WSHentYtelseskontraktListeRequest request = new WSHentYtelseskontraktListeRequest();
        request.setPersonidentifikator(FNR);
        WSHentYtelseskontraktListeResponse response = new WSHentYtelseskontraktListeResponse();

        List<WSYtelseskontrakt> ytelser = Stream.of(statuser)
                .map((status) -> {
                    WSYtelseskontrakt ytelse = new WSYtelseskontrakt();
                    ytelse.setStatus(status);
                    return ytelse;
                })
                .collect(toList());

        response.getYtelseskontraktListe().addAll(ytelser);

        when(ytelseskontraktV3.hentYtelseskontraktListe(request)).thenReturn(response);
    }
}