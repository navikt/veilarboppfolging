package no.nav.fo.veilarboppfolging.services;

import io.vavr.collection.Stream;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.security.veilarbabac.Bruker;
import no.nav.apiapp.security.veilarbabac.VeilarbAbacPepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.OppfolgingRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingsStatusRepository;
import no.nav.fo.veilarboppfolging.domain.*;
import no.nav.fo.veilarboppfolging.domain.arena.AktivitetStatus;
import no.nav.fo.veilarboppfolging.domain.arena.ArenaAktivitetDTO;
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

import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static no.nav.fo.veilarboppfolging.domain.KodeverkBruker.NAV;
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
    private ArenaOppfolgingService arenaOppfolgingService;

    @Mock
    private VeilarbAbacPepClient pepClientMock;

    @Mock
    private VeilarbaktivtetService veilarbaktivtetService;

    @Mock
    private YtelseskontraktV3 ytelseskontraktV3;

    @Mock
    private OppfolgingsStatusRepository oppfolgingsStatusRepository;

    @Mock(answer = Answers.RETURNS_MOCKS)
    private OppfolgingResolver.OppfolgingResolverDependencies oppfolgingResolverDependencies;

    private static final String FNR = "fnr";
    private static final String AKTOR_ID = "aktorId";
    private static final String ENHET = "enhet";
    private static final String VEILEDER = "veileder";
    private static final String BEGRUNNELSE = "begrunnelse";

    @InjectMocks
    private OppfolgingService oppfolgingService;

    private Oppfolging oppfolging = new Oppfolging().setAktorId(AKTOR_ID);
    private ArenaOppfolging arenaOppfolging;
    private WSKontaktinformasjon wsKontaktinformasjon = new WSKontaktinformasjon();

    @Before
    public void setup() throws Exception {
        arenaOppfolging = new ArenaOppfolging();
        when(oppfolgingRepositoryMock.hentOppfolging(anyString())).thenReturn(Optional.of(oppfolging));

        doAnswer((a) -> oppfolging.setUnderOppfolging(true)).when(oppfolgingRepositoryMock).startOppfolgingHvisIkkeAlleredeStartet(anyString());

        when(arenaOppfolgingService.hentArenaOppfolging(any(String.class)))
                .thenReturn(arenaOppfolging);
        when(digitalKontaktinformasjonV1Mock.hentDigitalKontaktinformasjon(any(WSHentDigitalKontaktinformasjonRequest.class)))
                .thenReturn(new WSHentDigitalKontaktinformasjonResponse()
                        .withDigitalKontaktinformasjon(wsKontaktinformasjon));
        when(aktorServiceMock.getAktorId(FNR)).thenReturn(of(AKTOR_ID));

        when(oppfolgingResolverDependencies.getAktorService()).thenReturn(aktorServiceMock);
        when(oppfolgingResolverDependencies.getOppfolgingRepository()).thenReturn(oppfolgingRepositoryMock);
        when(oppfolgingResolverDependencies.getArenaOppfolgingService()).thenReturn(arenaOppfolgingService);
        when(oppfolgingResolverDependencies.getDigitalKontaktinformasjonV1()).thenReturn(digitalKontaktinformasjonV1Mock);
        when(oppfolgingResolverDependencies.getPepClient()).thenReturn(pepClientMock);
        when(oppfolgingResolverDependencies.getVeilarbaktivtetService()).thenReturn(veilarbaktivtetService);
        when(oppfolgingResolverDependencies.getYtelseskontraktV3()).thenReturn(ytelseskontraktV3);
        gittOppfolgingStatus("", "");
    }

    @Test(expected = IngenTilgang.class)
    @SneakyThrows
    public void start_oppfolging_uten_enhet_tilgang() {
        doReturn(false).when(pepClientMock).harTilgangTilEnhet(any());
        oppfolgingService.startOppfolging(FNR);
    }

    @Test(expected = IngenTilgang.class)
    @SneakyThrows
    public void avslutt_oppfolging_uten_enhet_tilgang() {
        doReturn(false).when(pepClientMock).harTilgangTilEnhet(any());
        oppfolgingService.avsluttOppfolging(FNR, VEILEDER, BEGRUNNELSE);
    }

    @Test(expected = IngenTilgang.class)
    @SneakyThrows
    public void sett_manuell_uten_enhet_tilgang() {
        doReturn(false).when(pepClientMock).harTilgangTilEnhet(any());
        oppfolgingService.oppdaterManuellStatus(FNR, true, BEGRUNNELSE, NAV, VEILEDER);
    }

    @Test(expected = IngenTilgang.class)
    @SneakyThrows
    public void settDigital_uten_enhet_tilgang() {
        doReturn(false).when(pepClientMock).harTilgangTilEnhet(any());
        oppfolgingService.settDigitalBruker(FNR);
    }

    @Test(expected = IngenTilgang.class)
    @SneakyThrows
    public void start_eskalering_uten_enhet_tilgang() {
        doReturn(false).when(pepClientMock).harTilgangTilEnhet(any());
        oppfolgingService.startEskalering(FNR, BEGRUNNELSE, 1L);
    }

    @Test(expected = IngenTilgang.class)
    @SneakyThrows
    public void stopp_eskalering_uten_enhet_tilgang() {
        doReturn(false).when(pepClientMock).harTilgangTilEnhet(any());
        oppfolgingService.stoppEskalering(FNR, BEGRUNNELSE);
    }

    @Test
    public void medEnhetTilgang() throws Exception {
        when(pepClientMock.harTilgangTilEnhet(ENHET)).thenReturn(true);

        gittEnhet(ENHET);

        VeilederTilgang veilederTilgang = oppfolgingService.hentVeilederTilgang(FNR);
        assertThat(veilederTilgang.isTilgangTilBrukersKontor(), equalTo(true));
    }

    @Test
    public void utenEnhetTilgang() throws Exception {
        when(pepClientMock.harTilgangTilEnhet(anyString())).thenReturn(false);

        gittEnhet(ENHET);

        VeilederTilgang veilederTilgang = oppfolgingService.hentVeilederTilgang(FNR);
        assertThat(veilederTilgang.isTilgangTilBrukersKontor(), equalTo(false));
    }

    @Test
    public void ukjentAktor() throws Exception {
        doReturn(Optional.empty()).when(aktorServiceMock).getAktorId(FNR);
        assertThrows(IllegalArgumentException.class, this::hentOppfolgingStatus);
    }

    @Test
    public void riktigFnr() throws Exception {
        gittOppfolging(oppfolging);

        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();
        assertThat(oppfolgingStatusData.fnr, equalTo(FNR));
    }

    @Test
    public void hentOppfolgingStatus_brukerSomIkkeErUnderOppfolgingOppdateresIkkeDersomIkkeUnderOppfolgingIArena() throws Exception {
        gittOppfolging(oppfolging);
        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();

        verify(oppfolgingRepositoryMock, never()).startOppfolgingHvisIkkeAlleredeStartet(anyString());
        assertThat(oppfolgingStatusData.underOppfolging, is(false));
    }

    @Test
    public void hentOppfolgingStatus_brukerSomIkkeErUnderOppfolgingSettesUnderOppfolgingDersomArenaHarRiktigStatus() throws Exception {
        gittOppfolging(oppfolging);
        gittOppfolgingStatus("ARBS", "");

        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();

        verify(oppfolgingRepositoryMock).startOppfolgingHvisIkkeAlleredeStartet(AKTOR_ID);
        assertThat(oppfolgingStatusData.underOppfolging, is(true));
    }

    private void gittInaktivOppfolgingStatus(Boolean kanEnkeltReaktiveres) {
        arenaOppfolging.setFormidlingsgruppe("ISERV");
        arenaOppfolging.setKanEnkeltReaktiveres(kanEnkeltReaktiveres);
    }

    @Test
    public void hentOppfolgingStatus_brukerSomErUnderOppfolgingOgISERVMeldesUtDersomArenaSierReaktiveringIkkeErMulig() throws Exception {
        oppfolging.setUnderOppfolging(true);
        gittOppfolging(oppfolging);
        gittInaktivOppfolgingStatus(false);

        hentOppfolgingStatus();

        verify(oppfolgingRepositoryMock).avsluttOppfolging(eq(AKTOR_ID), eq(null), any(String.class));
    }

    @Test
    public void hentOppfolgingStatus_brukerSomErUnderOppfolgingOgISERVSkalReaktiveresDersomArenaSierReaktiveringErMulig() throws Exception {
        oppfolging.setUnderOppfolging(true);
        gittOppfolging(oppfolging);
        gittInaktivOppfolgingStatus(true);

        OppfolgingStatusData status = hentOppfolgingStatus();

        assertThat(status.kanReaktiveres, is(true));
        assertThat(status.inaktivIArena, is(true));
    }

    @Test
    public void hentOppfolgingStatus_brukerSomErKRRSkalVareManuell() throws Exception {
        oppfolging.setUnderOppfolging(true);
        gittReservasjon("true");
        OppfolgingStatusData status = hentOppfolgingStatus();

        assertThat(status.reservasjonKRR, is(true));
        assertThat(status.manuell, is(true));
    }

    @Test
    public void utenReservasjon() throws Exception {

        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();

        assertThat(oppfolgingStatusData.reservasjonKRR, is(false));
    }

    @Test
    public void utenKontaktInformasjon() throws Exception {
        gittKRRFeil(HentDigitalKontaktinformasjonKontaktinformasjonIkkeFunnet.class);
        gittOppfolgingStatus("ARBS", "");

        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();

        assertThat(oppfolgingStatusData.reservasjonKRR, is(true));
    }

    @Test
    public void personIkkeFunnet() throws Exception {
        gittKRRFeil(HentDigitalKontaktinformasjonPersonIkkeFunnet.class);
        gittOppfolgingStatus("ARBS", "");

        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();

        assertThat(oppfolgingStatusData.reservasjonKRR, is(true));
    }

    @Test
    public void medReservasjonOgUnderOppfolging() throws Exception {
        gittReservasjon("true");
        gittOppfolgingStatus("ARBS", "");

        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();

        assertThat(oppfolgingStatusData.reservasjonKRR, is(true));
    }

    @Test
    public void underOppfolging() throws Exception {
        gittOppfolging(oppfolging);
        gittOppfolgingStatus("ARBS", "");

        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();

        assertThat(oppfolgingStatusData.underOppfolging, is(true));
    }

    @Test
    public void ikkeArbeidssokerUnderOppfolging() throws Exception {
        gittOppfolging(oppfolging);
        gittOppfolgingStatus("IARBS", "BATT");

        val oppfolgingOgVilkarStatus = hentOppfolgingStatus();

        assertThat(oppfolgingOgVilkarStatus.underOppfolging, is(true));
    }

    @Test
    public void ikkeArbeidssokerIkkeUnderOppfolging() throws Exception {
        gittOppfolging(oppfolging);
        gittOppfolgingStatus("IARBS", "");

        val oppfolgingOgVilkarStatus = hentOppfolgingStatus();

        assertThat(oppfolgingOgVilkarStatus.underOppfolging, is(false));
    }

    @Test
    public void kanIkkeAvslutteNarManIkkeErUnderOppfolging() throws Exception {
        gittOppfolging(oppfolging.setUnderOppfolging(false));
        gittYtelserMedStatus();

        OppfolgingStatusData oppfolgingStatusData = oppfolgingService.hentAvslutningStatus(FNR);
        AvslutningStatusData avslutningStatusData = oppfolgingStatusData.avslutningStatusData;

        assertThat(avslutningStatusData.kanAvslutte, is(false));
    }

    @Test
    public void kanIkkeAvslutteNarManIkkeErUnderOppfolgingIArena() throws Exception {
        gittOppfolging(oppfolging.setUnderOppfolging(true));
        gittOppfolgingStatus("ARBS", null);
        gittYtelserMedStatus();

        OppfolgingStatusData oppfolgingStatusData = oppfolgingService.hentAvslutningStatus(FNR);
        AvslutningStatusData avslutningStatusData = oppfolgingStatusData.avslutningStatusData;

        assertThat(avslutningStatusData.kanAvslutte, is(false));
    }

    @Test
    public void kanAvslutteMedAktiveTiltak() throws Exception {
        gittOppfolging(oppfolging.setUnderOppfolging(true));
        gittOppfolgingStatus("ISERV", "");
        gittAktiveTiltak();
        gittYtelserMedStatus();

        OppfolgingStatusData oppfolgingStatusData = oppfolgingService.hentAvslutningStatus(FNR);
        AvslutningStatusData avslutningStatusData = oppfolgingStatusData.avslutningStatusData;

        assertThat(avslutningStatusData.harTiltak, is(true));
        assertThat(avslutningStatusData.kanAvslutte, is(true));
    }

    @Test
    public void kanAvslutteMedVarselOmAktiveYtelser() throws Exception {
        gittOppfolging(oppfolging.setUnderOppfolging(true));
        gittOppfolgingStatus("ISERV", "");
        gittIngenAktiveTiltak();
        gittYtelserMedStatus("Inaktiv", "Aktiv");

        OppfolgingStatusData oppfolgingStatusData = oppfolgingService.hentAvslutningStatus(FNR);
        AvslutningStatusData avslutningStatusData = oppfolgingStatusData.avslutningStatusData;

        assertThat(avslutningStatusData.kanAvslutte, is(true));
        assertThat(avslutningStatusData.harYtelser, is(true));
    }

    @Test(expected = IngenTilgang.class)
    public void underOppfolgingNiva3_skalFeileHvisIkkeTilgang() {
        VeilarbAbacPepClient endretVeilarbAbacPepClientMock = underOppfolgingNiva3_setup(of(AKTOR_ID));

        doThrow(IngenTilgang.class).when(endretVeilarbAbacPepClientMock).sjekkLesetilgangTilBruker(any(Bruker.class));

        oppfolgingService.underOppfolgingNiva3(FNR);
    }

    @Test(expected=IllegalArgumentException.class)
    public void underOppfolgingNiva3_skalFeileHvisAktoerIdIkkeFinnes() {
        underOppfolgingNiva3_setup(Optional.empty());

        oppfolgingService.underOppfolgingNiva3(FNR);
    }

    @Test
    public void underOppfolgingNiva3_skalReturnereFalseHvisIngenDataOmBruker() {
        underOppfolgingNiva3_setup(of(AKTOR_ID));

        assertThat(oppfolgingService.underOppfolgingNiva3(FNR), is(false));
    }

    @Test
    public void underOppfolgingNiva3_skalReturnereTrueHvisBrukerHarOppfolgingsflagg() {
        when(oppfolgingsStatusRepository.fetch(AKTOR_ID)).thenReturn(new OppfolgingTable().setUnderOppfolging(true));
        underOppfolgingNiva3_setup(of(AKTOR_ID));

        assertThat(oppfolgingService.underOppfolgingNiva3(FNR), is(true));
    }

    private VeilarbAbacPepClient underOppfolgingNiva3_setup(Optional<String> aktorId) {
        when(aktorServiceMock.getAktorId(FNR)).thenReturn(aktorId);
        VeilarbAbacPepClient endretVeilarbAbacPepClientMock = mock(VeilarbAbacPepClient.class);
        VeilarbAbacPepClient.Builder builderMock = mock(VeilarbAbacPepClient.Builder.class);
        when(pepClientMock.endre()).thenReturn(builderMock);
        when(builderMock.medResourceTypeUnderOppfolging()).thenReturn(builderMock);
        when(builderMock.bygg()).thenReturn(endretVeilarbAbacPepClientMock);
        return endretVeilarbAbacPepClientMock;
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
