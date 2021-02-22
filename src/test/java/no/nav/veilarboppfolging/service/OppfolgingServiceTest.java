package no.nav.veilarboppfolging.service;

import io.vavr.collection.Stream;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.YtelseskontraktV3;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.informasjon.ytelseskontrakt.WSYtelseskontrakt;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.WSHentYtelseskontraktListeRequest;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.WSHentYtelseskontraktListeResponse;
import no.nav.veilarboppfolging.client.dkif.DkifClient;
import no.nav.veilarboppfolging.client.dkif.DkifKontaktinfo;
import no.nav.veilarboppfolging.client.veilarbaktivitet.ArenaAktivitetDTO;
import no.nav.veilarboppfolging.client.veilarbaktivitet.VeilarbaktivitetClient;
import no.nav.veilarboppfolging.client.veilarbarena.ArenaOppfolging;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolging;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClient;
import no.nav.veilarboppfolging.domain.*;
import no.nav.veilarboppfolging.domain.arena.AktivitetStatus;
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository;
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// TODO: Ignorer disse testene til OppfolgingResolver har blitt fjernet
@Ignore
@RunWith(MockitoJUnitRunner.class)
public class OppfolgingServiceTest {

    private static final String FNR = "fnr";
    private static final String AKTOR_ID = "aktorId";
    private static final String ENHET = "enhet";
    private static final String VEILEDER = "veileder";
    private static final String BEGRUNNELSE = "begrunnelse";

    @Mock
    private DkifClient dkifClient;

    @Mock
    private AuthService authService;

    @Mock
    private VeilarbarenaClient veilarbarenaClient;

    @Mock
    private VeilarbaktivitetClient veilarbaktivitetClient;

    @Mock
    private YtelseskontraktV3 ytelseskontraktV3;

    @Mock
    private OppfolgingsStatusRepository oppfolgingsStatusRepository;

    @Mock
    private OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository;

    @Mock
    private WSHentYtelseskontraktListeResponse ytelser;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @InjectMocks
    private OppfolgingService oppfolgingService;

    private Oppfolging oppfolging = new Oppfolging().setAktorId(AKTOR_ID);
    private VeilarbArenaOppfolging veilarbArenaOppfolging = new VeilarbArenaOppfolging();
    private ArenaOppfolging arenaOppfolging;

    @Before
    public void setup() throws Exception {
        arenaOppfolging = new ArenaOppfolging();
//        when(oppfolgingRepositoryServiceMock.hentOppfolging(anyString())).thenReturn(Optional.of(oppfolging));
//
//        doAnswer((a) -> oppfolging.setUnderOppfolging(true)).when(oppfolgingRepositoryServiceMock).startOppfolgingHvisIkkeAlleredeStartet(anyString());

//        when(veilarbarenaClient.hentArenaOppfolging(any(String.class)))
//                .thenReturn(arenaOppfolging);
//        when(aktorServiceMock.getAktorId(FNR)).thenReturn(of(AKTOR_ID));
//        when(unleashService.isEnabled("veilarboppfolging.oppfolgingresolver.bruk_arena_direkte")).thenReturn(true);
//        when(unleashService.isEnabled("veilarboppfolging.hentVeilederTilgang.fra.veilarbarena")).thenReturn(true);
//
//        when(oppfolgingResolverDependencies.getAktorService()).thenReturn(aktorServiceMock);
//        when(oppfolgingResolverDependencies.getOppfolgingRepository()).thenReturn(oppfolgingRepositoryMock);
//        when(oppfolgingResolverDependencies.getArenaOppfolgingService()).thenReturn(veilarbarenaClient);
//        when(oppfolgingResolverDependencies.getVeilarbaktivtetService()).thenReturn(veilarbaktivitetClient);
//        when(oppfolgingResolverDependencies.getYtelseskontraktV3()).thenReturn(ytelseskontraktV3);
//        when(oppfolgingResolverDependencies.getUnleashService()).thenReturn(unleashService);
//        when(oppfolgingResolverDependencies.getDkifService()).thenReturn(dkifClient);
//        when(oppfolgingsbrukerService.hentOppfolgingsbruker(FNR)).thenReturn(Optional.of(veilarbArenaOppfolging));

        when(ytelseskontraktV3.hentYtelseskontraktListe(any())).thenReturn(mock(WSHentYtelseskontraktListeResponse.class));

        gittOppfolgingStatus("", "");
    }

    @Test
    public void skal_publisere_paa_kafka_ved_oppdatering_av_manuell_status() {
        when(authService.harTilgangTilEnhet(any())).thenReturn(true);
//        oppfolgingService.oppdaterManuellStatus(FNR, true, "test", SYSTEM, "test");
        verify(kafkaProducerService, times(1)).publiserEndringPaManuellStatus(AKTOR_ID, true);
    }

    @Test
    public void skal_publisere_paa_kafka_ved_start_paa_oppfolging() {
        when(authService.harTilgangTilEnhet(any())).thenReturn(true);
        oppfolgingService.startOppfolging(FNR);
        verify(kafkaProducerService, times(1)).publiserOppfolgingStartet(AKTOR_ID);
    }

    @Test
    public void skal_publisere_paa_kafka_ved_avsluttet_oppfolging() {
        when(authService.harTilgangTilEnhet(any())).thenReturn(true);
        oppfolgingService.avsluttOppfolging(FNR, VEILEDER, "");
        verify(kafkaProducerService, times(1)).publiserOppfolgingAvsluttet(AKTOR_ID);
    }

    @Test(expected = ResponseStatusException.class)
    @SneakyThrows
    public void start_oppfolging_uten_enhet_tilgang() {
        when(authService.harTilgangTilEnhet(any())).thenReturn(false);
        oppfolgingService.startOppfolging(FNR);
    }

    @Test(expected = ResponseStatusException.class)
    @SneakyThrows
    public void avslutt_oppfolging_uten_enhet_tilgang() {
        when(authService.harTilgangTilEnhet(any())).thenReturn(false);
        oppfolgingService.avsluttOppfolging(FNR, VEILEDER, BEGRUNNELSE);
    }

    @Test(expected = ResponseStatusException.class)
    @SneakyThrows
    public void sett_manuell_uten_enhet_tilgang() {
        when(authService.harTilgangTilEnhet(any())).thenReturn(false);
//        oppfolgingService.oppdaterManuellStatus(FNR, true, BEGRUNNELSE, NAV, VEILEDER);
    }

    @Test(expected = ResponseStatusException.class)
    @SneakyThrows
    public void settDigital_uten_enhet_tilgang() {
        when(authService.harTilgangTilEnhet(any())).thenReturn(false);
//        oppfolgingService.settDigitalBruker(FNR);
    }


    @Test
    public void medEnhetTilgang() {
        when(authService.harTilgangTilEnhet(any())).thenReturn(true);

        gittEnhet(ENHET);

        VeilederTilgang veilederTilgang = oppfolgingService.hentVeilederTilgang(FNR);
        assertTrue(veilederTilgang.isTilgangTilBrukersKontor());
    }

    @Test
    public void utenEnhetTilgang() {
        when(authService.harTilgangTilEnhet(any())).thenReturn(false);

        gittEnhet(ENHET);

        VeilederTilgang veilederTilgang = oppfolgingService.hentVeilederTilgang(FNR);
        assertThat(veilederTilgang.isTilgangTilBrukersKontor(), equalTo(false));
    }

    @Test
    public void ukjentAktor() {
        doThrow(new RuntimeException()).when(authService).getAktorIdOrThrow(FNR);
//        doReturn(Optional.empty()).when(aktorServiceMock).getAktorId(FNR);
        assertThrows(IllegalArgumentException.class, this::hentOppfolgingStatus);
    }

    @Test
    public void riktigFnr() {
        gittOppfolging(oppfolging);

        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();
        assertEquals(FNR, oppfolgingStatusData.fnr);
    }

    @Test
    public void riktigServicegruppe() {
        String servicegruppe = "BATT";
        gittServicegruppe(servicegruppe);

        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();
        assertEquals(servicegruppe, oppfolgingStatusData.servicegruppe);
    }

    @Test
    public void hentOppfolgingStatus_brukerSomIkkeErUnderOppfolgingOppdateresIkkeDersomIkkeUnderOppfolgingIArena() {
        gittOppfolging(oppfolging);
        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();

//        verify(oppfolgingRepositoryServiceMock, never()).startOppfolgingHvisIkkeAlleredeStartet(anyString());
        assertFalse(oppfolgingStatusData.underOppfolging);
    }

    @Test
    public void hentOppfolgingStatus_brukerSomIkkeErUnderOppfolgingSettesUnderOppfolgingDersomArenaHarRiktigStatus() {
        gittOppfolging(oppfolging);
        gittOppfolgingStatus("ARBS", "");

        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();

//        verify(oppfolgingRepositoryServiceMock).startOppfolgingHvisIkkeAlleredeStartet(AKTOR_ID);
        assertTrue(oppfolgingStatusData.underOppfolging);
    }

    private void gittInaktivOppfolgingStatus(Boolean kanEnkeltReaktiveres) {
        arenaOppfolging.setFormidlingsgruppe("ISERV");
        arenaOppfolging.setKanEnkeltReaktiveres(kanEnkeltReaktiveres);
    }

    private void gittServicegruppe(String servicegruppe) {
        arenaOppfolging.setServicegruppe(servicegruppe);
    }

    @Test
    public void hentOppfolgingStatus_brukerSomErUnderOppfolgingOgISERVMeldesUtDersomArenaSierReaktiveringIkkeErMulig() {
        oppfolging.setUnderOppfolging(true);
        gittOppfolging(oppfolging);
        gittInaktivOppfolgingStatus(false);

        hentOppfolgingStatus();

        verify(oppfolgingsPeriodeRepository).avslutt(eq(AKTOR_ID), eq(null), any(String.class));
    }

    @Test
    public void hentOppfolgingStatus_brukerSomErUnderOppfolgingOgISERVSkalReaktiveresDersomArenaSierReaktiveringErMulig() {
        oppfolging.setUnderOppfolging(true);
        gittOppfolging(oppfolging);
        gittInaktivOppfolgingStatus(true);

        OppfolgingStatusData status = hentOppfolgingStatus();

        assertTrue(status.kanReaktiveres);
        assertTrue(status.inaktivIArena);
    }

    @Test
    public void hentOppfolgingStatus_brukerSomErKRRSkalVareManuell() {
        oppfolging.setUnderOppfolging(true);
        gittReservasjonIKrr();
        OppfolgingStatusData status = hentOppfolgingStatus();

        assertTrue(status.reservasjonKRR);
        assertTrue(status.manuell);
    }

    @Test
    public void utenReservasjon() {
        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();
        assertFalse(oppfolgingStatusData.reservasjonKRR);
    }

    @Test
    public void utenKontaktInformasjon() {
        gittKRRFeil();
        gittOppfolgingStatus("ARBS", "");

        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();

        assertTrue(oppfolgingStatusData.reservasjonKRR);
    }

    @Test
    public void medReservasjonOgUnderOppfolging() {
        gittReservasjonIKrr();
        gittOppfolgingStatus("ARBS", "");

        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();

        assertTrue(oppfolgingStatusData.reservasjonKRR);
    }

    @Test
    public void underOppfolging() {
        gittOppfolging(oppfolging);
        gittOppfolgingStatus("ARBS", "");

        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();

        assertTrue(oppfolgingStatusData.underOppfolging);
    }

    @Test
    public void ikkeArbeidssokerUnderOppfolging() {
        gittOppfolging(oppfolging);
        gittOppfolgingStatus("IARBS", "BATT");

        val oppfolgingOgVilkarStatus = hentOppfolgingStatus();

        assertTrue(oppfolgingOgVilkarStatus.underOppfolging);
    }

    @Test
    public void ikkeArbeidssokerIkkeUnderOppfolging() {
        gittOppfolging(oppfolging);
        gittOppfolgingStatus("IARBS", "");

        val oppfolgingOgVilkarStatus = hentOppfolgingStatus();

        assertFalse(oppfolgingOgVilkarStatus.underOppfolging);
    }

    @Test
    public void kanIkkeAvslutteNarManIkkeErUnderOppfolging() throws Exception {
        gittOppfolging(oppfolging.setUnderOppfolging(false));
        gittYtelserMedStatus();

        AvslutningStatusData avslutningStatusData = oppfolgingService.hentAvslutningStatus(FNR);

        assertFalse(avslutningStatusData.kanAvslutte);
    }

    @Test
    public void kanIkkeAvslutteNarManIkkeErUnderOppfolgingIArena() throws Exception {
        gittOppfolging(oppfolging.setUnderOppfolging(true));
        gittOppfolgingStatus("ARBS", null);
        gittYtelserMedStatus();

        AvslutningStatusData avslutningStatusData = oppfolgingService.hentAvslutningStatus(FNR);

        assertFalse(avslutningStatusData.kanAvslutte);
    }

    @Test
    public void kanAvslutteMedVarselOmAktiveYtelser() throws Exception {
        gittOppfolging(oppfolging.setUnderOppfolging(true));
        gittOppfolgingStatus("ISERV", "");
        gittIngenAktiveTiltak();
        gittYtelserMedStatus("Inaktiv", "Aktiv");

        AvslutningStatusData avslutningStatusData = oppfolgingService.hentAvslutningStatus(FNR);

        assertTrue(avslutningStatusData.kanAvslutte);
        assertTrue(avslutningStatusData.harYtelser);
    }

    @Test(expected = ResponseStatusException.class)
    public void underOppfolgingNiva3_skalFeileHvisIkkeTilgang() {
//        doThrow(IngenTilgang.class).when(pepClientMock)
//                .sjekkTilgangTilPerson(any(AbacPersonId.class), any(Action.ActionId.class), eq(ResourceType.VeilArbUnderOppfolging));

        oppfolgingService.underOppfolgingNiva3(FNR);
    }

    @Test
    public void underOppfolgingNiva3_skalReturnereFalseHvisIngenDataOmBruker() {
        assertTrue(oppfolgingService.underOppfolgingNiva3(FNR));
    }

    @Test
    public void underOppfolgingNiva3_skalReturnereTrueHvisBrukerHarOppfolgingsflagg() {
        gittOppfolging(oppfolging.setUnderOppfolging(true));

        assertTrue(oppfolgingService.underOppfolgingNiva3(FNR));
    }

    private void gittOppfolgingStatus(String formidlingskode, String kvalifiseringsgruppekode) {
        arenaOppfolging.setFormidlingsgruppe(formidlingskode);
        arenaOppfolging.setServicegruppe(kvalifiseringsgruppekode);
    }

    private void gittEnhet(String enhet) {
        arenaOppfolging.setOppfolgingsenhet(enhet);
        veilarbArenaOppfolging.setNav_kontor(enhet);
    }

    private OppfolgingStatusData hentOppfolgingStatus() {
        return oppfolgingService.hentOppfolgingsStatus(FNR);
    }

    private void gittOppfolging(Oppfolging oppfolging) {
//        when(oppfolgingRepositoryServiceMock.hentOppfolging(AKTOR_ID)).thenReturn(Optional.of(oppfolging));
    }

    private void gittReservasjonIKrr() {
        DkifKontaktinfo kontaktinfo = new DkifKontaktinfo();
        kontaktinfo.setPersonident("fnr");
        kontaktinfo.setKanVarsles(false);
        kontaktinfo.setReservert(true);

        when(dkifClient.hentKontaktInfo(FNR)).thenReturn(kontaktinfo);
    }

    private void gittKRRFeil() {
//        when(dkifClient.sjekkDkifRest(FNR)).thenReturn("{\n" +
//                "  \"melding\": \"Tilgang nektet\"\n" +
//                "}");
    }

    private void gittAktiveTiltak() {
        when(veilarbaktivitetClient.hentArenaAktiviteter(FNR)).thenReturn(
                asList(
                        new ArenaAktivitetDTO().setStatus(AktivitetStatus.GJENNOMFORES),
                        new ArenaAktivitetDTO().setStatus(AktivitetStatus.PLANLAGT)
                )
        );
    }

    private void gittIngenAktiveTiltak() {
        when(veilarbaktivitetClient.hentArenaAktiviteter(FNR)).thenReturn(
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

    private static Fnr fnr() {
        return new Fnr(FNR);
    }

}
