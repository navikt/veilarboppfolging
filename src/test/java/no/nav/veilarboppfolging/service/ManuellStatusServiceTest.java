package no.nav.veilarboppfolging.service;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolging;
import no.nav.veilarboppfolging.client.digdir_krr.DigdirClient;
import no.nav.veilarboppfolging.client.digdir_krr.DigdirKontaktinfo;
import no.nav.veilarboppfolging.repository.ManuellStatusRepository;
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository;
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository;
import no.nav.veilarboppfolging.repository.entity.ManuellStatusEntity;
import no.nav.veilarboppfolging.test.DbTestUtils;
import no.nav.veilarboppfolging.test.IsolatedDatabaseTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static no.nav.veilarboppfolging.repository.enums.KodeverkBruker.NAV;
import static no.nav.veilarboppfolging.repository.enums.KodeverkBruker.SYSTEM;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ManuellStatusServiceTest extends IsolatedDatabaseTest {

    private static final Fnr FNR = Fnr.of("fnr");
    private static final AktorId AKTOR_ID = AktorId.of("aktorId");
    private static final String ENHET = "enhet";
    private static final String VEILEDER = "veileder";
    private static final String BEGRUNNELSE = "begrunnelse";

    private final AuthService authService = mock(AuthService.class);

    private final ArenaOppfolgingService arenaOppfolgingService = mock(ArenaOppfolgingService.class);

    private final DigdirClient digdirClient = mock(DigdirClient.class);

    private final KafkaProducerService kafkaProducerService = mock(KafkaProducerService.class);

    private OppfolgingsStatusRepository oppfolgingsStatusRepository;

    private ManuellStatusRepository manuellStatusRepository;

    private OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository;

    private ManuellStatusService manuellStatusService;

    private final OppfolgingService oppfolgingService = mock(OppfolgingService.class);

    @Before
    public void setup() {
        TransactionTemplate transactor = DbTestUtils.createTransactor(db);

        when(arenaOppfolgingService.hentOppfolgingFraVeilarbarena(FNR))
                .thenReturn(Optional.of(new VeilarbArenaOppfolging().setNav_kontor(ENHET)));
        doCallRealMethod().when(authService).sjekkTilgangTilEnhet(any());
        when(authService.getAktorIdOrThrow(FNR)).thenReturn(AKTOR_ID);

        manuellStatusRepository = new ManuellStatusRepository(db, transactor);
        oppfolgingsStatusRepository = new OppfolgingsStatusRepository(db);
        oppfolgingsPeriodeRepository = new OppfolgingsPeriodeRepository(db, transactor);

        manuellStatusService = new ManuellStatusService(
                authService,
                manuellStatusRepository,
                arenaOppfolgingService,
                oppfolgingService,
                oppfolgingsStatusRepository,
                digdirClient,
                kafkaProducerService,
                transactor
        );
    }

    @Test
    public void oppdaterManuellStatus_oppretter_manuell_status_og_publiserer_paa_kafka_ved_oppdatering_av_manuell_status() {
        when(oppfolgingService.erUnderOppfolging(AKTOR_ID)).thenReturn(true);
        when(authService.harTilgangTilEnhet(any())).thenReturn(true);
        when(digdirClient.hentKontaktInfo(FNR)).thenReturn(Optional.of(new DigdirKontaktinfo()));
        gittAktivOppfolging();

        String begrunnelse = "test begrunnelse";
        String opprettetAvBruker = "test opprettet av";

        manuellStatusService.oppdaterManuellStatus(FNR, true, begrunnelse, SYSTEM, opprettetAvBruker);

        long gjeldendeManuellStatusId = oppfolgingsStatusRepository.hentOppfolging(AKTOR_ID).orElseThrow().getGjeldendeManuellStatusId();
        Optional<ManuellStatusEntity> maybeGjeldendeManuellStatus = manuellStatusRepository.hentManuellStatus(gjeldendeManuellStatusId);

        assertTrue(maybeGjeldendeManuellStatus.isPresent());

        ManuellStatusEntity gjeldendeManuellStatus = maybeGjeldendeManuellStatus.get();

        assertEquals(AKTOR_ID.get(), gjeldendeManuellStatus.getAktorId());
        assertEquals(SYSTEM, gjeldendeManuellStatus.getOpprettetAv());
        assertEquals(begrunnelse, gjeldendeManuellStatus.getBegrunnelse());
        assertEquals(SYSTEM, gjeldendeManuellStatus.getOpprettetAv());
        assertEquals(opprettetAvBruker, gjeldendeManuellStatus.getOpprettetAvBrukerId());
        assertTrue(gjeldendeManuellStatus.isManuell());

        verify(kafkaProducerService, times(1)).publiserEndringPaManuellStatus(AKTOR_ID, true);
    }

    @Test(expected = ResponseStatusException.class)
    public void oppdaterManuellStatus_kaster_exception_dersom_ikke_tilgang_til_enhet() {
        when(authService.harTilgangTilEnhet(ENHET)).thenReturn(false);
        manuellStatusService.oppdaterManuellStatus(FNR, true, BEGRUNNELSE, NAV, VEILEDER);
    }


    @Test(expected = ResponseStatusException.class)
    public void settDigitalBruker_kaster_exception_dersom_ikke_tilgang_til_enhet() {
        when(authService.harTilgangTilEnhet(any())).thenReturn(false);
        manuellStatusService.settDigitalBruker(FNR);
    }

    @Test
    public void synkroniserManuellStatusMedDigDir__skal_lage_manuell_status_hvis_reservert() {
        DigdirKontaktinfo kontaktinfo = new DigdirKontaktinfo()
                .setPersonident(FNR.get())
                .setKanVarsles(true)
                .setReservert(true)
                .setEpostadresse("email")
                .setMobiltelefonnummer("12345");

        when(oppfolgingService.erUnderOppfolging(AKTOR_ID)).thenReturn(true);

        when(digdirClient.hentKontaktInfo(FNR)).thenReturn(Optional.of(kontaktinfo));

        gittAktivOppfolging();

        manuellStatusService.synkroniserManuellStatusMedDigdir(FNR);

        List<ManuellStatusEntity> history = manuellStatusRepository.history(AKTOR_ID);

        assertEquals(1, history.size());
    }

    @Test
    public void synkroniserManuellStatusMedDigdir__skal_ikke_lage_manuell_status_hvis_ikke_under_oppfolging() {
        when(oppfolgingService.erUnderOppfolging(AKTOR_ID)).thenReturn(false);

        manuellStatusService.synkroniserManuellStatusMedDigdir(FNR);

        verifyNoInteractions(digdirClient);

        List<ManuellStatusEntity> history = manuellStatusRepository.history(AKTOR_ID);

        assertTrue(history.isEmpty());
    }

    @Test
    public void synkroniserManuellStatusMedDigDir__skal_ikke_lage_manuell_status_hvis_ikke_reservert_nar_vi_ikke_har_status_pa_bruker_fra_for() {
        DigdirKontaktinfo kontaktinfo = new DigdirKontaktinfo()
                .setPersonident(FNR.get())
                .setKanVarsles(true)
                .setReservert(false)
                .setEpostadresse("email")
                .setMobiltelefonnummer("12345");

        when(oppfolgingService.erUnderOppfolging(AKTOR_ID)).thenReturn(true);
        when(authService.harTilgangTilEnhet(any())).thenReturn(true);
        when(digdirClient.hentKontaktInfo(FNR)).thenReturn(Optional.of(kontaktinfo));

        manuellStatusService.synkroniserManuellStatusMedDigdir(FNR);

        List<ManuellStatusEntity> history = manuellStatusRepository.history(AKTOR_ID);

        assertTrue(history.isEmpty());
    }

    @Test
    public void synkroniserManuellStatusMedDigDir__skal_lage_manuell_status_hvis_ikke_reservert_nar_vi_har_status_pa_bruker_fra_for() {
        DigdirKontaktinfo kontaktinfo = new DigdirKontaktinfo()
                .setPersonident(FNR.get())
                .setKanVarsles(true)
                .setReservert(false)
                .setEpostadresse("email")
                .setMobiltelefonnummer("12345");
        String begrunnelse = "test begrunnelse";
        String opprettetAvBruker = "test opprettet av";
        gittAktivOppfolging();
        when(authService.harTilgangTilEnhet(any())).thenReturn(true);
        when(digdirClient.hentKontaktInfo(FNR)).thenReturn(Optional.of(kontaktinfo));
        manuellStatusService.oppdaterManuellStatus(FNR, true, begrunnelse, SYSTEM, opprettetAvBruker);

        manuellStatusService.synkroniserManuellStatusMedDigdir(FNR);

        List<ManuellStatusEntity> history = manuellStatusRepository.history(AKTOR_ID);

        assertFalse(history.isEmpty());
    }

    @Test
    public void oppdateringer_av_manuell_status_reflekteres_i_om_bruker_er_manuell() {
        when(authService.harTilgangTilEnhet(any())).thenReturn(true);
        gittAktivOppfolging();

        assertFalse(manuellStatusService.erManuell(AKTOR_ID));

        manuellStatusService.settBrukerTilManuellGrunnetReservasjonIKRR(AKTOR_ID);
        assertTrue(manuellStatusService.erManuell(AKTOR_ID));

        manuellStatusService.settDigitalBruker(FNR);
        assertFalse(manuellStatusService.erManuell(AKTOR_ID));

        manuellStatusService.settBrukerTilManuellGrunnetReservasjonIKRR(AKTOR_ID);
        assertTrue(manuellStatusService.erManuell(AKTOR_ID));


        oppfolgingsPeriodeRepository.avslutt(AKTOR_ID, "", "");
        assertFalse(manuellStatusService.erManuell(AKTOR_ID));
    }

    @Test
    public void settBrukerTilManuellGrunnetReservasjonIKRR__skal_lage_manuell_status() {
        gittAktivOppfolging();

        manuellStatusService.settBrukerTilManuellGrunnetReservasjonIKRR(AKTOR_ID);

        ManuellStatusEntity manuellStatus = manuellStatusService.hentManuellStatus(AKTOR_ID).orElseThrow();

        assertTrue(manuellStatus.getId() > 0);
        assertEquals("Brukeren er reservert i Kontakt- og reservasjonsregisteret", manuellStatus.getBegrunnelse());
        assertEquals(SYSTEM, manuellStatus.getOpprettetAv());
        assertNull(manuellStatus.getOpprettetAvBrukerId());
        assertTrue(manuellStatus.getDato().isBefore(ZonedDateTime.now().plusSeconds(5)));
    }

    @Test
    public void settBrukerTilManuellGrunnetReservasjonIKRR__skal_ikke_lage_manuell_status_hvis_allerede_manuell() {
        gittAktivOppfolging();

        ManuellStatusEntity manuellStatus = new ManuellStatusEntity()
                .setManuell(true)
                .setAktorId(AKTOR_ID.get())
                .setOpprettetAv(SYSTEM)
                .setBegrunnelse("test");

        manuellStatusRepository.create(manuellStatus);

        manuellStatusService.settBrukerTilManuellGrunnetReservasjonIKRR(AKTOR_ID);

        List<ManuellStatusEntity> history = manuellStatusRepository.history(AKTOR_ID);

        assertEquals(1, history.size());
    }

    @Test
    public void hentDigDirKontaktinfo__skal_returnere_kontaktinfo_fra_digDir() {
        DigdirKontaktinfo kontaktinfo = new DigdirKontaktinfo()
                .setPersonident(FNR.get())
                .setKanVarsles(true)
                .setReservert(true)
                .setEpostadresse("email")
                .setMobiltelefonnummer("12345");

        when(digdirClient.hentKontaktInfo(FNR)).thenReturn(Optional.of(kontaktinfo));

        assertEquals(kontaktinfo, manuellStatusService.hentDigdirKontaktinfo(FNR));
    }

    @Test
    public void hentDigdirKontaktinfo__skal_returnere_fallback_hvis_digdir_mangler_kontaktinfo() {
        when(digdirClient.hentKontaktInfo(FNR)).thenReturn(Optional.empty());

        DigdirKontaktinfo fallbackKontaktInfo = new DigdirKontaktinfo()
                .setPersonident(FNR.get())
                .setKanVarsles(true)
                .setReservert(false);

        assertEquals(fallbackKontaktInfo, manuellStatusService.hentDigdirKontaktinfo(FNR));
    }


    private void gittAktivOppfolging() {
        oppfolgingsStatusRepository.opprettOppfolging(ManuellStatusServiceTest.AKTOR_ID);
        db.update("UPDATE OPPFOLGINGSTATUS SET UNDER_OPPFOLGING = ? WHERE AKTOR_ID = ?", true, ManuellStatusServiceTest.AKTOR_ID.get());
        when(oppfolgingService.erUnderOppfolging(AKTOR_ID)).thenReturn(true);
    }
}
