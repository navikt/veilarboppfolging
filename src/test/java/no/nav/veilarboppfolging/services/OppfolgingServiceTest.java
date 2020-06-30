package no.nav.veilarboppfolging.services;

import io.vavr.collection.Stream;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.veilarboppfolging.client.veilarbarena.ArenaOppfolgingService;
import no.nav.veilarboppfolging.client.veilarbarena.OppfolgingsbrukerService;
import no.nav.veilarboppfolging.db.OppfolgingRepository;
import no.nav.veilarboppfolging.db.OppfolgingsStatusRepository;
import no.nav.veilarboppfolging.domain.*;
import no.nav.veilarboppfolging.domain.arena.AktivitetStatus;
import no.nav.veilarboppfolging.client.veilarbaktivitet.ArenaAktivitetDTO;
import no.nav.veilarboppfolging.kafka.OppfolgingStatusKafkaProducer;
import no.nav.veilarboppfolging.utils.mappers.VeilarbArenaOppfolging;
import no.nav.sbl.dialogarena.common.abac.pep.AbacPersonId;
import no.nav.sbl.dialogarena.common.abac.pep.domain.ResourceType;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.Action;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
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
import static no.nav.veilarboppfolging.domain.KodeverkBruker.NAV;
import static no.nav.veilarboppfolging.domain.KodeverkBruker.SYSTEM;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OppfolgingServiceTest {

    @Mock
    private DkifService dkifService;

    @Mock
    private OppfolgingRepository oppfolgingRepositoryMock;

    @Mock
    private AktorService aktorServiceMock;

    @Mock
    private AutorisasjonService autorisasjonService;

    @Mock
    private ArenaOppfolgingService arenaOppfolgingService;

    @Mock
    private PepClient pepClientMock;

    @Mock
    private VeilarbaktivtetService veilarbaktivtetService;

    @Mock
    private YtelseskontraktV3 ytelseskontraktV3;

    @Mock
    private OppfolgingsStatusRepository oppfolgingsStatusRepository;

    @Mock
    private OppfolgingsbrukerService oppfolgingsbrukerService;

    @Mock(answer = Answers.RETURNS_MOCKS)
    private OppfolgingResolver.OppfolgingResolverDependencies oppfolgingResolverDependencies;

    @Mock
    private UnleashService unleashService;

    @Mock
    private OppfolgingStatusKafkaProducer kafkaProducer;

    @Mock
    private WSHentYtelseskontraktListeResponse ytelser;

    private static final String FNR = "fnr";
    private static final String AKTOR_ID = "aktorId";
    private static final String ENHET = "enhet";
    private static final String VEILEDER = "veileder";
    private static final String BEGRUNNELSE = "begrunnelse";

    @InjectMocks
    private OppfolgingService oppfolgingService;

    private Oppfolging oppfolging = new Oppfolging().setAktorId(AKTOR_ID);
    private VeilarbArenaOppfolging veilarbArenaOppfolging = new VeilarbArenaOppfolging();
    private ArenaOppfolging arenaOppfolging;

    @Before
    public void setup() throws Exception {
        arenaOppfolging = new ArenaOppfolging();
        when(oppfolgingRepositoryMock.hentOppfolging(anyString())).thenReturn(Optional.of(oppfolging));

        doAnswer((a) -> oppfolging.setUnderOppfolging(true)).when(oppfolgingRepositoryMock).startOppfolgingHvisIkkeAlleredeStartet(anyString());

        when(arenaOppfolgingService.hentArenaOppfolging(any(String.class)))
                .thenReturn(arenaOppfolging);
        when(aktorServiceMock.getAktorId(FNR)).thenReturn(of(AKTOR_ID));
        when(unleashService.isEnabled("veilarboppfolging.oppfolgingresolver.bruk_arena_direkte")).thenReturn(true);
        when(unleashService.isEnabled("veilarboppfolging.hentVeilederTilgang.fra.veilarbarena")).thenReturn(true);

        when(oppfolgingResolverDependencies.getAktorService()).thenReturn(aktorServiceMock);
        when(oppfolgingResolverDependencies.getOppfolgingRepository()).thenReturn(oppfolgingRepositoryMock);
        when(oppfolgingResolverDependencies.getArenaOppfolgingService()).thenReturn(arenaOppfolgingService);
        when(oppfolgingResolverDependencies.getVeilarbaktivtetService()).thenReturn(veilarbaktivtetService);
        when(oppfolgingResolverDependencies.getYtelseskontraktV3()).thenReturn(ytelseskontraktV3);
        when(oppfolgingResolverDependencies.getUnleashService()).thenReturn(unleashService);
        when(oppfolgingResolverDependencies.getDkifService()).thenReturn(dkifService);
        when(oppfolgingsbrukerService.hentOppfolgingsbruker(FNR)).thenReturn(Optional.of(veilarbArenaOppfolging));

        when(ytelseskontraktV3.hentYtelseskontraktListe(any())).thenReturn(mock(WSHentYtelseskontraktListeResponse.class));

        gittOppfolgingStatus("", "");
    }

    @Test
    public void skal_publisere_paa_kafka_ved_oppdatering_av_manuell_status() {
        when(pepClientMock.harTilgangTilEnhet(any())).thenReturn(true);
        oppfolgingService.oppdaterManuellStatus(FNR, true, "test", SYSTEM, "test");
        verify(kafkaProducer, times(1)).send(fnr());
    }

    @Test
    public void skal_publisere_paa_kafka_ved_start_paa_oppfolging() {
        when(pepClientMock.harTilgangTilEnhet(any())).thenReturn(true);
        oppfolgingService.startOppfolging(FNR);
        verify(kafkaProducer, times(1)).send(fnr());
    }

    @Test
    public void skal_publisere_paa_kafka_ved_avsluttet_oppfolging() {
        when(pepClientMock.harTilgangTilEnhet(any())).thenReturn(true);
        oppfolgingService.avsluttOppfolging(FNR, VEILEDER, "");
        verify(kafkaProducer, times(1)).send(fnr());
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
    public void riktigServicegruppe() throws Exception {
        String servicegruppe = "BATT";
        gittServicegruppe(servicegruppe);

        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();
        assertThat(oppfolgingStatusData.servicegruppe, equalTo(servicegruppe));
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

    private void gittServicegruppe(String servicegruppe) {
        arenaOppfolging.setServicegruppe(servicegruppe);
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
        gittReservasjonIKrr();
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
        gittKRRFeil();
        gittOppfolgingStatus("ARBS", "");

        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();

        assertThat(oppfolgingStatusData.reservasjonKRR, is(true));
    }

    @Test
    public void medReservasjonOgUnderOppfolging() throws Exception {
        gittReservasjonIKrr();
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
    public void underOppfolgingNiva3_skalFeileHvisIkkeTilgang() throws Exception {
        doThrow(IngenTilgang.class).when(pepClientMock)
                .sjekkTilgangTilPerson(any(AbacPersonId.class), any(Action.ActionId.class), eq(ResourceType.VeilArbUnderOppfolging));

        oppfolgingService.underOppfolgingNiva3(FNR);
    }

    @Test
    public void underOppfolgingNiva3_skalReturnereFalseHvisIngenDataOmBruker() throws Exception {
        assertThat(oppfolgingService.underOppfolgingNiva3(FNR), is(false));
    }

    @Test
    public void underOppfolgingNiva3_skalReturnereTrueHvisBrukerHarOppfolgingsflagg() throws Exception {
        gittOppfolging(oppfolging.setUnderOppfolging(true));

        assertThat(oppfolgingService.underOppfolgingNiva3(FNR), is(true));
    }

    private void gittOppfolgingStatus(String formidlingskode, String kvalifiseringsgruppekode) {
        arenaOppfolging.setFormidlingsgruppe(formidlingskode);
        arenaOppfolging.setServicegruppe(kvalifiseringsgruppekode);
    }

    private void gittEnhet(String enhet) {
        arenaOppfolging.setOppfolgingsenhet(enhet);
        veilarbArenaOppfolging.setNav_kontor(enhet);
    }

    private OppfolgingStatusData hentOppfolgingStatus() throws Exception {
        return oppfolgingService.hentOppfolgingsStatus(FNR);
    }

    private void gittOppfolging(Oppfolging oppfolging) {
        when(oppfolgingRepositoryMock.hentOppfolging(AKTOR_ID)).thenReturn(Optional.of(oppfolging));
    }

    private void gittReservasjonIKrr() {
        val dkifResponse = "{\n" +
                "  \"kontaktinfo\": {\n" +
                "    \"fnr\": {\n" +
                "      \"kanVarsles\": false,\n" +
                "      \"reservert\": true,\n" +
                "    },\n" +
                "  }\n" +
                "}";


        when(dkifService.sjekkDkifRest(FNR)).thenReturn(dkifResponse);
    }

    private void gittKRRFeil() {
        when(dkifService.sjekkDkifRest(FNR)).thenReturn("{\n" +
                "  \"melding\": \"Tilgang nektet\"\n" +
                "}");
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

    private static Fnr fnr() {
        return new Fnr(FNR);
    }

}
