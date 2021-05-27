package no.nav.veilarboppfolging.service;

import io.vavr.collection.Stream;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.informasjon.ytelseskontrakt.WSYtelseskontrakt;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.WSHentYtelseskontraktListeRequest;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.meldinger.WSHentYtelseskontraktListeResponse;
import no.nav.veilarboppfolging.client.dkif.DkifClient;
import no.nav.veilarboppfolging.client.dkif.DkifKontaktinfo;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClient;
import no.nav.veilarboppfolging.client.ytelseskontrakt.YtelseskontraktClient;
import no.nav.veilarboppfolging.client.ytelseskontrakt.YtelseskontraktMapper;
import no.nav.veilarboppfolging.client.ytelseskontrakt.YtelseskontraktResponse;
import no.nav.veilarboppfolging.domain.*;
import no.nav.veilarboppfolging.repository.*;
import no.nav.veilarboppfolging.test.IsolatedDatabaseTest;
import no.nav.veilarboppfolging.utils.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OppfolgingServiceTest extends IsolatedDatabaseTest {

    private static final String FNR = "fnr";
    private static final String AKTOR_ID = "aktorId";
    private static final String ENHET = "enhet";
    private static final String VEILEDER = "veileder";
    private static final String BEGRUNNELSE = "begrunnelse";

    private ArenaOppfolgingTilstand arenaOppfolgingTilstand;

    private VeilarbarenaClient veilarbarenaClient = mock(VeilarbarenaClient.class);
    private DkifClient dkifClient = mock(DkifClient.class);
    private AuthService authService = mock(AuthService.class);
    private KafkaProducerService kafkaProducerService = mock(KafkaProducerService.class);
    private YtelseskontraktClient ytelseskontraktClient = mock(YtelseskontraktClient.class);
    private ArenaOppfolgingService arenaOppfolgingService = mock(ArenaOppfolgingService.class);
    private EskaleringService eskaleringService = mock(EskaleringService.class);
    private NyeBrukereFeedRepository nyeBrukereFeedRepository = mock(NyeBrukereFeedRepository.class);
    private KvpService kvpService = mock(KvpService.class);
    private KvpRepository kvpRepository = mock(KvpRepository.class);
    private MetricsService metricsService = mock(MetricsService.class);

    private OppfolgingsStatusRepository oppfolgingsStatusRepository;
    private OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository;
    private ManuellStatusRepository manuellStatusRepository;
    private OppfolgingService oppfolgingService;

    @Before
    public void setup() {


        arenaOppfolgingTilstand = new ArenaOppfolgingTilstand();
        oppfolgingsStatusRepository = new OppfolgingsStatusRepository(db);
        oppfolgingsPeriodeRepository = new OppfolgingsPeriodeRepository(db);
        manuellStatusRepository = new ManuellStatusRepository(db);

        oppfolgingService = new OppfolgingService(kafkaProducerService,
                new YtelserOgAktiviteterService(ytelseskontraktClient),
                dkifClient,
                kvpService,
                metricsService,
                arenaOppfolgingService,
                authService,
                oppfolgingsStatusRepository,
                oppfolgingsPeriodeRepository,
                manuellStatusRepository,
                null,
                eskaleringService,
                null,
                kvpRepository,
                nyeBrukereFeedRepository,
                null,
                null);


        gittArenaOppfolgingStatus("", "");

        when(authService.getAktorIdOrThrow(FNR)).thenReturn(AKTOR_ID);
        when(arenaOppfolgingService.hentOppfolgingTilstand(FNR)).thenReturn(Optional.of(arenaOppfolgingTilstand));
        when(ytelseskontraktClient.hentYtelseskontraktListe(any())).thenReturn(mock(YtelseskontraktResponse.class));
        when(dkifClient.hentKontaktInfo(FNR)).thenReturn(new DkifKontaktinfo());
    }

    @Test
    public void skal_publisere_paa_kafka_ved_start_paa_oppfolging() {
        arenaOppfolgingTilstand.setFormidlingsgruppe("IARBS");
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID);

        when(kvpService.erUnderKvp(anyLong())).thenReturn(false);

        assertTrue(oppfolgingsPeriodeRepository.hentOppfolgingsperioder(AKTOR_ID).isEmpty());

        oppfolgingService.startOppfolging(FNR);

        assertUnderOppfolgingLagret(AKTOR_ID);

        List<Oppfolgingsperiode> oppfolgingsperioder = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(AKTOR_ID);
        assertEquals(1, oppfolgingsperioder.size());

        verify(kafkaProducerService, times(1)).publiserOppfolgingStartet(AKTOR_ID, oppfolgingsperioder.get(0).getStartDato());
    }

    @Test
    public void skal_publisere_paa_kafka_ved_avsluttet_oppfolging() {
        arenaOppfolgingTilstand.setFormidlingsgruppe("IARBS");
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID);
        oppfolgingService.startOppfolging(FNR);

        assertUnderOppfolgingLagret(AKTOR_ID);

        arenaOppfolgingTilstand.setFormidlingsgruppe("ISERV");

        oppfolgingService.avsluttOppfolging(FNR, VEILEDER, "");
        verify(kafkaProducerService, times(1)).publiserOppfolgingAvsluttet(AKTOR_ID);
    }

    @Test(expected = ResponseStatusException.class)
    @SneakyThrows
    public void start_oppfolging_uten_enhet_tilgang() {
        when(authService.harTilgangTilEnhet(any())).thenReturn(false);
        doCallRealMethod().when(authService).sjekkTilgangTilEnhet(any());

        oppfolgingService.startOppfolging(FNR);
    }

    @Test(expected = ResponseStatusException.class)
    @SneakyThrows
    public void avslutt_oppfolging_uten_enhet_tilgang() {
        when(authService.harTilgangTilEnhet(any())).thenReturn(false);
        doCallRealMethod().when(authService).sjekkTilgangTilEnhet(any());
        oppfolgingService.avsluttOppfolging(FNR, VEILEDER, BEGRUNNELSE);
    }

    @Test
    public void medEnhetTilgang() {
        when(authService.harTilgangTilEnhet(any())).thenReturn(true);

        arenaOppfolgingTilstand.setOppfolgingsenhet(ENHET);

        VeilederTilgang veilederTilgang = oppfolgingService.hentVeilederTilgang(FNR);
        assertTrue(veilederTilgang.isTilgangTilBrukersKontor());
    }

    @Test
    public void utenEnhetTilgang() {
        when(authService.harTilgangTilEnhet(any())).thenReturn(false);

        arenaOppfolgingTilstand.setOppfolgingsenhet(ENHET);

        VeilederTilgang veilederTilgang = oppfolgingService.hentVeilederTilgang(FNR);
        assertThat(veilederTilgang.isTilgangTilBrukersKontor(), equalTo(false));
    }

    @Test
    public void ukjentAktor() {
        doCallRealMethod().when(authService).sjekkLesetilgangMedFnr(any());
        doThrow(new IllegalArgumentException()).when(authService).getAktorIdOrThrow(FNR);
        assertThrows(IllegalArgumentException.class, this::hentOppfolgingStatus);
    }

    @Test
    public void riktigFnr() {
        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();
        assertEquals(FNR, oppfolgingStatusData.fnr);
    }

    @Test
    public void riktigServicegruppe() {
        String servicegruppe = "BATT";
        arenaOppfolgingTilstand.setServicegruppe(servicegruppe);

        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();
        assertEquals(servicegruppe, oppfolgingStatusData.servicegruppe);
    }

    @Test
    public void hentOppfolgingStatus_brukerSomIkkeErUnderOppfolgingOppdateresIkkeDersomIkkeUnderOppfolgingIArena() {
        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();

        assertFalse(oppfolgingStatusData.underOppfolging);
    }

    @Test
    public void hentOppfolgingStatus_brukerSomIkkeErUnderOppfolgingSettesUnderOppfolgingDersomArenaHarRiktigStatus() {
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID);

        assertFalse(oppfolgingsStatusRepository.fetch(AKTOR_ID).isUnderOppfolging());

        gittArenaOppfolgingStatus("ARBS", "");
        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();

        assertUnderOppfolgingLagret(AKTOR_ID);
        assertTrue(oppfolgingStatusData.underOppfolging);
    }

    @Test
    public void hentOppfolgingStatus_brukerSomErUnderOppfolgingOgISERVMeldesUtDersomArenaSierReaktiveringIkkeErMulig() {
        oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(AKTOR_ID);
        assertUnderOppfolgingLagret(AKTOR_ID);

        gittInaktivOppfolgingStatus(false);
        when(arenaOppfolgingService.hentOppfolgingTilstandDirekteFraArena(FNR)).thenReturn(Optional.of(arenaOppfolgingTilstand));

        hentOppfolgingStatus();

        assertEquals(
                oppfolgingsPeriodeRepository.hentAvsluttetOppfolgingsperioder(AKTOR_ID).size(),
                oppfolgingsPeriodeRepository.hentOppfolgingsperioder(AKTOR_ID).size()
        );
        assertTrue(oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(AKTOR_ID).isEmpty());
        assertFalse(oppfolgingsStatusRepository.fetch(AKTOR_ID).isUnderOppfolging());
    }

    @Test
    public void hentOppfolgingStatus_brukerSomErUnderOppfolgingOgISERVSkalReaktiveresDersomArenaSierReaktiveringErMulig() {
        oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(AKTOR_ID);
        assertUnderOppfolgingLagret(AKTOR_ID);

        gittInaktivOppfolgingStatus(true);

        OppfolgingStatusData status = hentOppfolgingStatus();

        assertTrue(status.kanReaktiveres);
        assertTrue(status.inaktivIArena);
        assertTrue(status.underOppfolging);
    }

    @Test
    public void hentOppfolgingStatus_brukerSomErKRRSkalVareManuell() {
        gittReservasjonIKrr(true);
        OppfolgingStatusData status = hentOppfolgingStatus();

        assertTrue(status.reservasjonKRR);
        assertTrue(status.manuell);
    }

    @Test
    public void utenReservasjon() {
        gittReservasjonIKrr(false);
        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();
        assertFalse(oppfolgingStatusData.reservasjonKRR);
    }

    @Test
    public void medReservasjonOgUnderOppfolging() {
        gittReservasjonIKrr(true);
        gittArenaOppfolgingStatus("ARBS", "");

        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();

        assertUnderOppfolgingLagret(AKTOR_ID);
        assertTrue(oppfolgingStatusData.reservasjonKRR);
    }

    @Test
    public void underOppfolging() {
        gittArenaOppfolgingStatus("ARBS", "");

        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();

        assertUnderOppfolgingLagret(AKTOR_ID);
        assertTrue(oppfolgingStatusData.underOppfolging);
    }

    @Test
    public void ikkeArbeidssokerUnderOppfolging() {
        gittArenaOppfolgingStatus("IARBS", "BATT");

        val oppfolgingOgVilkarStatus = hentOppfolgingStatus();

        assertTrue(oppfolgingOgVilkarStatus.underOppfolging);
    }

    @Test
    public void ikkeArbeidssokerIkkeUnderOppfolging() {
        gittArenaOppfolgingStatus("IARBS", "");

        val oppfolgingOgVilkarStatus = hentOppfolgingStatus();

        assertFalse(oppfolgingOgVilkarStatus.underOppfolging);
    }

    @Test
    public void kanIkkeAvslutteNarManIkkeErUnderOppfolging() throws Exception {
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID);
        gittYtelserMedStatus();

        AvslutningStatusData avslutningStatusData = oppfolgingService.hentAvslutningStatus(FNR);

        assertFalse(avslutningStatusData.kanAvslutte);
    }

    @Test
    public void kanIkkeAvslutteNarManIkkeErUnderOppfolgingIArena() throws Exception {
        oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(AKTOR_ID);
        assertUnderOppfolgingLagret(AKTOR_ID);

        gittArenaOppfolgingStatus("ARBS", null);
        gittYtelserMedStatus();

        AvslutningStatusData avslutningStatusData = oppfolgingService.hentAvslutningStatus(FNR);

        assertFalse(avslutningStatusData.kanAvslutte);
    }

    @Test
    public void kanAvslutteMedVarselOmAktiveYtelser() throws Exception {
        oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(AKTOR_ID);
        assertUnderOppfolgingLagret(AKTOR_ID);

        gittArenaOppfolgingStatus("ISERV", "");
        gittYtelserMedStatus("Inaktiv", "Aktiv");

        AvslutningStatusData avslutningStatusData = oppfolgingService.hentAvslutningStatus(FNR);

        assertTrue(avslutningStatusData.kanAvslutte);
        assertTrue(avslutningStatusData.harYtelser);
    }

    @Test(expected = ResponseStatusException.class)
    public void underOppfolgingNiva3_skalFeileHvisIkkeTilgang() {
        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED))
                .when(authService).sjekkTilgangTilPersonMedNiva3(AKTOR_ID);

        oppfolgingService.underOppfolgingNiva3(FNR);
    }

    @Test
    public void underOppfolgingNiva3_skalReturnereFalseHvisIngenDataOmBruker() {
        assertFalse(oppfolgingService.underOppfolgingNiva3(FNR));
    }

    @Test
    public void underOppfolgingNiva3_skalReturnereTrueHvisBrukerHarOppfolgingsflagg() {
        oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(AKTOR_ID);
        assertUnderOppfolgingLagret(AKTOR_ID);

        assertTrue(oppfolgingService.underOppfolgingNiva3(FNR));
    }

    private void assertUnderOppfolgingLagret(String aktorId) {
        assertTrue(oppfolgingsStatusRepository.fetch(aktorId).isUnderOppfolging());
        assertTrue(oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(aktorId).isPresent());
        assertEquals(
                oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId).size(),
                oppfolgingsPeriodeRepository.hentAvsluttetOppfolgingsperioder(aktorId).size() + 1
        );
    }

    private void gittInaktivOppfolgingStatus(Boolean kanEnkeltReaktiveres) {
        arenaOppfolgingTilstand.setFormidlingsgruppe("ISERV");
        arenaOppfolgingTilstand.setKanEnkeltReaktiveres(kanEnkeltReaktiveres);
    }

    private void gittArenaOppfolgingStatus(String formidlingskode, String kvalifiseringsgruppekode) {
        arenaOppfolgingTilstand.setFormidlingsgruppe(formidlingskode);
        arenaOppfolgingTilstand.setServicegruppe(kvalifiseringsgruppekode);
    }

    private OppfolgingStatusData hentOppfolgingStatus() {
        return oppfolgingService.hentOppfolgingsStatus(FNR);
    }

    private void gittReservasjonIKrr(boolean reservert) {
        DkifKontaktinfo kontaktinfo = new DkifKontaktinfo();
        kontaktinfo.setPersonident("fnr");
        kontaktinfo.setKanVarsles(false);
        kontaktinfo.setReservert(reservert);

        when(dkifClient.hentKontaktInfo(FNR)).thenReturn(kontaktinfo);
    }

    private void gittYtelserMedStatus(String... statuser) throws Exception {
        WSHentYtelseskontraktListeRequest request = new WSHentYtelseskontraktListeRequest();
        request.setPersonidentifikator(FNR);
        WSHentYtelseskontraktListeResponse response = new WSHentYtelseskontraktListeResponse();

        List<WSYtelseskontrakt> ytelser = Stream.of(statuser)
                .map((status) -> {
                    WSYtelseskontrakt ytelse = new WSYtelseskontrakt();
                    ytelse.setStatus(status);
                    ytelse.setDatoKravMottatt(DateUtils.convertDateToXMLGregorianCalendar(LocalDate.now()));
                    return ytelse;
                })
                .collect(toList());

        response.getYtelseskontraktListe().addAll(ytelser);

        when(ytelseskontraktClient.hentYtelseskontraktListe(FNR))
                .thenReturn(YtelseskontraktMapper.tilYtelseskontrakt(response));
    }
}
