package no.nav.veilarboppfolging.service;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.common.client.aktorregister.IngenGjeldendeIdentException;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;
import no.nav.pto_schema.enums.arena.Formidlingsgruppe;
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe;
import no.nav.veilarboppfolging.ForbiddenException;
import no.nav.veilarboppfolging.client.amttiltak.AmtTiltakClient;
import no.nav.veilarboppfolging.client.digdir_krr.KRRData;
import no.nav.veilarboppfolging.client.veilarbarena.ArenaOppfolgingTilstand;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolgingsStatus;
import no.nav.veilarboppfolging.controller.response.UnderOppfolgingDTO;
import no.nav.veilarboppfolging.controller.response.VeilederTilgang;
import no.nav.veilarboppfolging.domain.AvslutningStatusData;
import no.nav.veilarboppfolging.domain.OppfolgingStatusData;
import no.nav.veilarboppfolging.domain.StartetAvType;
import no.nav.veilarboppfolging.eventsLogger.BigQueryClient;
import no.nav.veilarboppfolging.kafka.dto.OppfolgingsperiodeDTO;
import no.nav.veilarboppfolging.oppfolgingsbruker.Oppfolgingsbruker;
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingService;
import no.nav.veilarboppfolging.repository.KvpRepository;
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository;
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository;
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity;
import no.nav.veilarboppfolging.test.DbTestUtils;
import no.nav.veilarboppfolging.test.IsolatedDatabaseTest;
import no.nav.veilarboppfolging.utils.OppfolgingsperiodeUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OppfolgingServiceTest extends IsolatedDatabaseTest {

    private static final Fnr FNR = Fnr.of("fnr");
    private static final AktorId AKTOR_ID = AktorId.of("aktorId");
    private static final String ENHET = "5563";
    private static final String VEILEDER = "veileder";
    private static final String BEGRUNNELSE = "begrunnelse";
    private static final NavIdent NAV_IDENT = NavIdent.of("Z1234");

    private ArenaOppfolgingTilstand arenaOppfolgingTilstand;
    private VeilarbArenaOppfolgingsStatus arenaOppfolgingStatus;

    private AuthService authService = mock(AuthService.class);
    private KafkaProducerService kafkaProducerService = mock(KafkaProducerService.class);
    private ArenaOppfolgingService arenaOppfolgingService = mock(ArenaOppfolgingService.class);
    private ArenaYtelserService arenaYtelserService = mock(ArenaYtelserService.class);
    private KvpService kvpService = mock(KvpService.class);
    private KvpRepository kvpRepository = mock(KvpRepository.class);
    private MetricsService metricsService = mock(MetricsService.class);
    private ManuellStatusService manuellStatusService = mock(ManuellStatusService.class);
    private AmtTiltakClient amtTiltakClient = mock(AmtTiltakClient.class);
    private OppfolgingsStatusRepository oppfolgingsStatusRepository;
    private OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository;
    private OppfolgingService oppfolgingService;
    private BigQueryClient bigQueryClient = mock(BigQueryClient.class);

    @Before
    public void setup() {
        TransactionTemplate transactor = DbTestUtils.createTransactor(db);

        arenaOppfolgingTilstand = new ArenaOppfolgingTilstand();
        arenaOppfolgingStatus = new VeilarbArenaOppfolgingsStatus();
        oppfolgingsStatusRepository = new OppfolgingsStatusRepository(new NamedParameterJdbcTemplate(db));
        oppfolgingsPeriodeRepository = new OppfolgingsPeriodeRepository(db, transactor);

        oppfolgingService = new OppfolgingService(kafkaProducerService,
                kvpService,
                arenaOppfolgingService,
                authService,
                oppfolgingsStatusRepository,
                oppfolgingsPeriodeRepository,
                manuellStatusService,
                amtTiltakClient,
                kvpRepository,
                null,
                null,
                 transactor,
                arenaYtelserService,
                bigQueryClient
                );


        gittArenaOppfolgingStatus("", "");

        when(authService.getAktorIdOrThrow(FNR)).thenReturn(AKTOR_ID);
        when(authService.getFnrOrThrow(AKTOR_ID)).thenReturn(FNR);
        when(arenaOppfolgingService.hentArenaOppfolgingTilstand(FNR)).thenReturn(Optional.of(arenaOppfolgingTilstand));
        when(arenaOppfolgingService.hentArenaOppfolgingsStatus(FNR)).thenReturn(Optional.of(arenaOppfolgingStatus));
        when(arenaOppfolgingService.hentArenaOppfolgingsEnhetId(FNR)).thenReturn(EnhetId.of(ENHET));
        when(manuellStatusService.hentDigdirKontaktinfo(FNR)).thenReturn(new KRRData());
        when(amtTiltakClient.harAktiveTiltaksdeltakelser(FNR.get())).thenReturn(false);
    }

    @Test
    public void skal_publisere_paa_kafka_ved_start_paa_oppfolging() {
        arenaOppfolgingTilstand.setFormidlingsgruppe("IARBS");
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID);
        assertTrue(oppfolgingsPeriodeRepository.hentOppfolgingsperioder(AKTOR_ID).isEmpty());

        oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(Oppfolgingsbruker.arenaSyncOppfolgingBruker(AKTOR_ID, Formidlingsgruppe.IARBS, Kvalifiseringsgruppe.BATT));
        assertUnderOppfolgingLagret(AKTOR_ID);

        List<OppfolgingsperiodeEntity> oppfolgingsperioder = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(AKTOR_ID);
        assertEquals(1, oppfolgingsperioder.size());
        verify(kafkaProducerService, times(1)).publiserOppfolgingsperiode(any(OppfolgingsperiodeDTO.class));
        verify(kafkaProducerService).publiserVisMinSideMicrofrontend(AKTOR_ID, "ao-min-side-microfrontend");
    }

    @Test
    public void skal_publisere_paa_kafka_ved_avsluttet_oppfolging() {
        arenaOppfolgingTilstand.setFormidlingsgruppe("IARBS");
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID);
        oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(Oppfolgingsbruker.arenaSyncOppfolgingBruker(AKTOR_ID, Formidlingsgruppe.IARBS, Kvalifiseringsgruppe.BATT));

        UnderOppfolgingDTO underOppfolgingDTO = oppfolgingService.oppfolgingData(FNR);
        Assertions.assertThat(underOppfolgingDTO.isUnderOppfolging()).isTrue();
        Assertions.assertThat(underOppfolgingDTO.isErManuell()).isFalse();


        assertUnderOppfolgingLagret(AKTOR_ID);

        arenaOppfolgingTilstand.setFormidlingsgruppe("ISERV");

        reset(kafkaProducerService);
        oppfolgingService.avsluttOppfolging(FNR, VEILEDER, "");

        verify(kafkaProducerService).publiserOppfolgingsperiode(any(OppfolgingsperiodeDTO.class));
        verify(kafkaProducerService).publiserVeilederTilordnet(AKTOR_ID, null);
        verify(kafkaProducerService).publiserEndringPaNyForVeileder(AKTOR_ID, false);
        verify(kafkaProducerService).publiserEndringPaManuellStatus(AKTOR_ID, false);
        verify(kafkaProducerService).publiserSkjulMinSideMicrofrontend(AKTOR_ID, "ao-min-side-microfrontend");
    }

    @Test(expected = ForbiddenException.class)
    @SneakyThrows
    public void avslutt_oppfolging_uten_skrivetilgang_til_bruker() {
        when(authService.erInternBruker()).thenReturn(true);
        doCallRealMethod().when(authService).sjekkTilgangTilEnhet(any());
        oppfolgingService.avsluttOppfolging(FNR, VEILEDER, BEGRUNNELSE);
    }

    @Test
    public void skal_ikke_avslutte_oppfolging_hvis_aktive_tiltaksdeltakelser() {
        when(amtTiltakClient.harAktiveTiltaksdeltakelser(FNR.get())).thenReturn(true);
        arenaOppfolgingTilstand.setFormidlingsgruppe("IARBS");
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID);
        oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(Oppfolgingsbruker.arbeidssokerStartetAvBrukerEllerSystem(AKTOR_ID, StartetAvType.BRUKER));

        assertUnderOppfolgingLagret(AKTOR_ID);

        arenaOppfolgingTilstand.setFormidlingsgruppe("ISERV");

        reset(kafkaProducerService);
        oppfolgingService.avsluttOppfolging(FNR, VEILEDER, "");

        verify(kafkaProducerService, never()).publiserOppfolgingsperiode(any(OppfolgingsperiodeDTO.class));
        assertHarGjeldendeOppfolgingsperiode(AKTOR_ID);
    }

    @Test
    public void hentVeilederTilgang__medEnhetTilgang() {
        when(authService.harTilgangTilEnhet(any())).thenReturn(true);
        arenaOppfolgingTilstand.setOppfolgingsenhet(ENHET);

        val tilgang = oppfolgingService.hentVeilederTilgang(FNR);
        assertNotNull(tilgang);
        assertTrue(tilgang.isTilgangTilBrukersKontor());
    }

    @Test
    public void hentVeilederTilgang__utenEnhetTilgang() {
        arenaOppfolgingTilstand.setOppfolgingsenhet(ENHET);

        VeilederTilgang veilederIkkeTilgang = oppfolgingService.hentVeilederTilgang(FNR);
        assertFalse(veilederIkkeTilgang.isTilgangTilBrukersKontor());
    }

    @Test
    public void hentVeilederTilgang__skal_ikke_ha_tilgang_nar_bruker_ikke_har_enhet() {
        arenaOppfolgingTilstand.setOppfolgingsenhet(ENHET);

        VeilederTilgang veilederIkkeTilgang = oppfolgingService.hentVeilederTilgang(FNR);
        assertFalse(veilederIkkeTilgang.isTilgangTilBrukersKontor());
    }

    @Test
    public void skal_krasje_nar_aktorId_er_ukjent() {
        doCallRealMethod().when(authService).sjekkLesetilgangMedFnr(any());
        when(authService.erEksternBruker()).thenReturn(false);
        doThrow(new IngenGjeldendeIdentException()).when(authService).getAktorIdOrThrow(FNR);
        assertThrows(IngenGjeldendeIdentException.class, this::hentOppfolgingStatus);
    }

    @Test
    public void riktigFnr() {
        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();
        assertEquals(FNR.get(), oppfolgingStatusData.fnr);
    }

    @Test
    public void riktigServicegruppe() {
        String servicegruppe = "BATT";
        arenaOppfolgingTilstand.setServicegruppe(servicegruppe);
        arenaOppfolgingStatus.setServicegruppe(servicegruppe);

        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();
        assertEquals(servicegruppe, oppfolgingStatusData.servicegruppe);
    }

    @Test
    public void hentOppfolgingStatus_brukerSomIkkeErUnderOppfolgingOppdateresIkkeDersomIkkeUnderOppfolgingIArena() {
        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();

        assertFalse(oppfolgingStatusData.underOppfolging);
    }

    @Test
    public void hentOppfolgingStatus_brukerSomErUnderOppfolgingOgISERVSkalReaktiveresDersomArenaSierReaktiveringErMulig() {
        oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(Oppfolgingsbruker.arbeidssokerStartetAvVeileder(AKTOR_ID, StartetAvType.VEILEDER, NAV_IDENT));
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
    public void ikkeArbeidssokerIkkeUnderOppfolging() {
        gittArenaOppfolgingStatus("IARBS", "");

        val oppfolgingOgVilkarStatus = hentOppfolgingStatus();

        assertFalse(oppfolgingOgVilkarStatus.underOppfolging);
    }

    @Test
    public void kanIkkeAvslutteNarManIkkeErUnderOppfolging() {
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID);

        AvslutningStatusData avslutningStatusData = oppfolgingService.hentAvslutningStatus(FNR);

        assertFalse(avslutningStatusData.kanAvslutte);
    }

    @Test
    public void kanIkkeAvslutteNarManIkkeErUnderOppfolgingIArena() {
        oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(Oppfolgingsbruker.arbeidssokerStartetAvVeileder(AKTOR_ID, StartetAvType.VEILEDER, NAV_IDENT));
        assertUnderOppfolgingLagret(AKTOR_ID);

        gittArenaOppfolgingStatus("ARBS", null);

        AvslutningStatusData avslutningStatusData = oppfolgingService.hentAvslutningStatus(FNR);

        assertFalse(avslutningStatusData.kanAvslutte);
    }

    @Test
    public void kanIkkeAvslutteHvisManHarAktiveTiltaksdeltakelser() {
        when(amtTiltakClient.harAktiveTiltaksdeltakelser(FNR.get())).thenReturn(true);
        oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(Oppfolgingsbruker.arbeidssokerStartetAvVeileder(AKTOR_ID, StartetAvType.VEILEDER, NAV_IDENT));
        assertUnderOppfolgingLagret(AKTOR_ID);

        gittArenaOppfolgingStatus("ISERV", "");

        AvslutningStatusData avslutningStatusData = oppfolgingService.hentAvslutningStatus(FNR);

        assertFalse(avslutningStatusData.kanAvslutte);
        assertTrue(avslutningStatusData.harAktiveTiltaksdeltakelser);
    }

    @Test
    public void kanAvslutteMedVarselOmAktiveYtelser() {
        oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(Oppfolgingsbruker.arenaSyncOppfolgingBruker(AKTOR_ID, Formidlingsgruppe.IARBS, Kvalifiseringsgruppe.BATT));
        assertUnderOppfolgingLagret(AKTOR_ID);

        gittArenaOppfolgingStatus("ISERV", "");
        when(arenaYtelserService.harPagaendeYtelse(FNR)).thenReturn(true);

        AvslutningStatusData avslutningStatusData = oppfolgingService.hentAvslutningStatus(FNR);

        assertTrue(avslutningStatusData.kanAvslutte);
        assertTrue(avslutningStatusData.harYtelser);
    }

    @Test(expected = ForbiddenException.class)
    public void underOppfolgingNiva3_skalFeileHvisIkkeTilgang() {
        doThrow(new ForbiddenException("Hei"))
                .when(authService).sjekkTilgangTilPersonMedNiva3(AKTOR_ID);

        oppfolgingService.erUnderOppfolgingNiva3(FNR);
    }

    @Test
    public void underOppfolgingNiva3_skalReturnereFalseHvisIngenDataOmBruker() {
        assertFalse(oppfolgingService.erUnderOppfolgingNiva3(FNR));
    }

    @Test
    public void underOppfolgingNiva3_skalReturnereTrueHvisBrukerHarOppfolgingsflagg() {
        oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(Oppfolgingsbruker.arenaSyncOppfolgingBruker(AKTOR_ID, Formidlingsgruppe.IARBS, Kvalifiseringsgruppe.BKART));
        assertUnderOppfolgingLagret(AKTOR_ID);

        assertTrue(oppfolgingService.erUnderOppfolgingNiva3(FNR));
    }

    @Test
    public void startOppfolgingHvisIkkeAlleredeStartet__skal_opprette_ikke_opprette_manuell_status_hvis_ikke_reservert_i_krr() {
        gittReservasjonIKrr(false);

        oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(Oppfolgingsbruker.arenaSyncOppfolgingBruker(AKTOR_ID, Formidlingsgruppe.IARBS, Kvalifiseringsgruppe.IKVAL));

        verify(manuellStatusService, never()).settBrukerTilManuellGrunnetReservertIKRR(any());
    }

    @Test
    public void startOppfolgingHvisIkkeAlleredeStartet__skal_opprette_manuell_status_hvis_reservert_i_krr() {
        gittReservasjonIKrr(true);

        oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(Oppfolgingsbruker.arenaSyncOppfolgingBruker(AKTOR_ID, Formidlingsgruppe.IARBS, Kvalifiseringsgruppe.IVURD));

        verify(manuellStatusService, times(1)).settBrukerTilManuellGrunnetReservertIKRR(AKTOR_ID);
    }

    private void assertUnderOppfolgingLagret(AktorId aktorId) {
        assertTrue(oppfolgingsStatusRepository.hentOppfolging(aktorId).orElseThrow().isUnderOppfolging());

        assertHarGjeldendeOppfolgingsperiode(aktorId);

        assertEquals(
                oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId).size(),
                oppfolgingsPeriodeRepository.hentAvsluttetOppfolgingsperioder(aktorId).size() + 1
        );
    }


    private void assertHarGjeldendeOppfolgingsperiode(AktorId aktorId) {
        assertTrue(harGjeldendeOppfolgingsperiode(aktorId));
    }

    private boolean harGjeldendeOppfolgingsperiode(AktorId aktorId) {
        List<OppfolgingsperiodeEntity> oppfolgingsperioder = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId);
        OppfolgingsperiodeEntity sisteOppfolgingsperiode = OppfolgingsperiodeUtils.hentSisteOppfolgingsperiode(oppfolgingsperioder);
        return sisteOppfolgingsperiode != null && sisteOppfolgingsperiode.getStartDato() != null && sisteOppfolgingsperiode.getSluttDato() == null;
    }

    private void gittInaktivOppfolgingStatus(Boolean kanEnkeltReaktiveres) {
        arenaOppfolgingTilstand.setFormidlingsgruppe("ISERV");
        arenaOppfolgingStatus.setKanEnkeltReaktiveres(kanEnkeltReaktiveres);
        arenaOppfolgingStatus.setFormidlingsgruppe("ISERV");
//        arenaOppfolgingTilstand.setKanEnkeltReaktiveres(kanEnkeltReaktiveres);
    }

    private void gittArenaOppfolgingStatus(String formidlingskode, String kvalifiseringsgruppekode) {
        arenaOppfolgingTilstand.setFormidlingsgruppe(formidlingskode);
        arenaOppfolgingTilstand.setServicegruppe(kvalifiseringsgruppekode);
    }

    private OppfolgingStatusData hentOppfolgingStatus() {
        return oppfolgingService.hentOppfolgingsStatus(FNR);
    }

    private void gittReservasjonIKrr(boolean reservert) {
        KRRData kontaktinfo = new KRRData()
            .withPersonident("fnr")
            .withKanVarsles(false)
            .withReservert(reservert);

        when(manuellStatusService.hentDigdirKontaktinfo(FNR)).thenReturn(kontaktinfo);
    }
}
