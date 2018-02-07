package no.nav.fo.veilarboppfolging.services;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.OppfolgingRepository;
import no.nav.fo.veilarboppfolging.domain.*;
import no.nav.fo.veilarboppfolging.vilkar.VilkarService;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.DigitalKontaktinformasjonV1;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.HentDigitalKontaktinformasjonKontaktinformasjonIkkeFunnet;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.HentDigitalKontaktinformasjonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.WSKontaktinformasjon;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.WSHentDigitalKontaktinformasjonRequest;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.WSHentDigitalKontaktinformasjonResponse;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingsstatusRequest;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingsstatusResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.sql.Timestamp;
import java.util.Optional;

import static java.lang.System.currentTimeMillis;
import static java.util.Optional.of;
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
    private OppfoelgingPortType oppfoelgingPortTypeMock;

    @Mock
    private PepClient pepClientMock;

    @Mock
    private OppfolgingResolver.OppfolgingResolverDependencies oppfolgingResolverDependencies;

    private static final String FNR = "fnr";
    private static final String AKTOR_ID = "aktorId";
    private static final String ENHET = "0100";
    private static final String VEILEDER = "Z990000";
    private static final String BEGRUNNELSE = "begrunnelse";

    @InjectMocks
    private OppfolgingService oppfolgingService;

    private Oppfolging oppfolging = new Oppfolging().setAktorId(AKTOR_ID);
    private HentOppfoelgingsstatusResponse hentOppfolgingstatusResponse;
    private WSKontaktinformasjon wsKontaktinformasjon = new WSKontaktinformasjon();

    @Before
    public void setup() throws Exception {
        hentOppfolgingstatusResponse = new HentOppfoelgingsstatusResponse();
        when(oppfolgingRepositoryMock.opprettOppfolging(anyString())).thenReturn(oppfolging);

        doAnswer((a) -> oppfolging.setUnderOppfolging(true)).when(oppfolgingRepositoryMock).startOppfolgingHvisIkkeAlleredeStartet(anyString());

        when(oppfoelgingPortTypeMock.hentOppfoelgingsstatus(any(HentOppfoelgingsstatusRequest.class)))
                .thenReturn(hentOppfolgingstatusResponse);
        when(digitalKontaktinformasjonV1Mock.hentDigitalKontaktinformasjon(any(WSHentDigitalKontaktinformasjonRequest.class)))
                .thenReturn(new WSHentDigitalKontaktinformasjonResponse()
                        .withDigitalKontaktinformasjon(wsKontaktinformasjon));
        when(vilkarServiceMock.getVilkar(any(VilkarService.VilkarType.class), any())).thenReturn("Gjeldene Vilkar");
        when(aktorServiceMock.getAktorId(FNR)).thenReturn(of(AKTOR_ID));

        when(oppfolgingResolverDependencies.getAktorService()).thenReturn(aktorServiceMock);
        when(oppfolgingResolverDependencies.getOppfolgingRepository()).thenReturn(oppfolgingRepositoryMock);
        when(oppfolgingResolverDependencies.getOppfoelgingPortType()).thenReturn(oppfoelgingPortTypeMock);
        when(oppfolgingResolverDependencies.getDigitalKontaktinformasjonV1()).thenReturn(digitalKontaktinformasjonV1Mock);
        when(oppfolgingResolverDependencies.getVilkarService()).thenReturn(vilkarServiceMock);
        when(oppfolgingResolverDependencies.getPepClient()).thenReturn(pepClientMock);
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
    public void oppfolgingMedOppfolgingsFlaggIDatabasen() throws Exception {
        gittAktor();
        gittOppfolging(oppfolging.setUnderOppfolging(true));

        hentOppfolgingStatus();

        verifyZeroInteractions(oppfoelgingPortTypeMock);
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
        hentOppfolgingstatusResponse.setFormidlingsgruppeKode(formidlingskode);
        hentOppfolgingstatusResponse.setServicegruppeKode(kvalifiseringsgruppekode);
    }

    private void gittEnhet(String enhet) {
        hentOppfolgingstatusResponse.setNavOppfoelgingsenhet(enhet);
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

}