package no.nav.veilarboppfolging.service;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.client.dkif.DkifClient;
import no.nav.veilarboppfolging.client.dkif.DkifKontaktinfo;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolging;
import no.nav.veilarboppfolging.repository.ManuellStatusRepository;
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

    private AuthService authService = mock(AuthService.class);
    private ArenaOppfolgingService arenaOppfolgingService = mock(ArenaOppfolgingService.class);
    private DkifClient dkifClient = mock(DkifClient.class);
    private KafkaProducerService kafkaProducerService = mock(KafkaProducerService.class);

    private OppfolgingsStatusRepository oppfolgingsStatusRepository;
    private ManuellStatusRepository manuellStatusRepository;

    private ManuellStatusService manuellStatusService;

    @Before
    public void setup() {
        TransactionTemplate transactor = DbTestUtils.createTransactor(db);

        when(arenaOppfolgingService.hentOppfolgingFraVeilarbarena(FNR))
                .thenReturn(Optional.of(new VeilarbArenaOppfolging().setNav_kontor(ENHET)));
        doCallRealMethod().when(authService).sjekkTilgangTilEnhet(any());
        when(authService.getAktorIdOrThrow(FNR)).thenReturn(AKTOR_ID);

        oppfolgingsStatusRepository = new OppfolgingsStatusRepository(db);
        manuellStatusRepository = new ManuellStatusRepository(db, transactor);

        manuellStatusService = new ManuellStatusService(
                authService,
                manuellStatusRepository,
                arenaOppfolgingService,
                oppfolgingsStatusRepository,
                dkifClient,
                kafkaProducerService,
                transactor
        );
    }

    @Test
    public void oppdaterManuellStatus_oppretter_manuell_status_og_publiserer_paa_kafka_ved_oppdatering_av_manuell_status() {
        when(authService.harTilgangTilEnhet(any())).thenReturn(true);
        when(dkifClient.hentKontaktInfo(FNR)).thenReturn(Optional.of(new DkifKontaktinfo()));
        gittAktivOppfolging(AKTOR_ID);

        String begrunnelse = "test begrunnelse";
        String opprettetAvBruker = "test opprettet av";

        manuellStatusService.oppdaterManuellStatus(FNR, true, begrunnelse, SYSTEM, opprettetAvBruker);

        long gjeldendeManuellStatusId = oppfolgingsStatusRepository.fetch(AKTOR_ID).getGjeldendeManuellStatusId();
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
    public void synkroniserManuellStatusMedDkif__skal_lage_manuell_status_hvis_reservert() {
        DkifKontaktinfo kontaktinfo = new DkifKontaktinfo()
                .setPersonident(FNR.get())
                .setKanVarsles(true)
                .setReservert(true)
                .setEpostadresse("email")
                .setMobiltelefonnummer("12345");

        when(dkifClient.hentKontaktInfo(FNR)).thenReturn(Optional.of(kontaktinfo));

        gittAktivOppfolging(AKTOR_ID);

        manuellStatusService.synkroniserManuellStatusMedDkif(FNR);

        List<ManuellStatusEntity> history = manuellStatusRepository.history(AKTOR_ID);

        assertEquals(1, history.size());
    }

    @Test
    public void synkroniserManuellStatusMedDkif__skal_ikke_lage_manuell_status_hvis_ikke_reservert() {
        DkifKontaktinfo kontaktinfo = new DkifKontaktinfo()
                .setPersonident(FNR.get())
                .setKanVarsles(true)
                .setReservert(false)
                .setEpostadresse("email")
                .setMobiltelefonnummer("12345");

        when(dkifClient.hentKontaktInfo(FNR)).thenReturn(Optional.of(kontaktinfo));

        manuellStatusService.synkroniserManuellStatusMedDkif(FNR);

        List<ManuellStatusEntity> history = manuellStatusRepository.history(AKTOR_ID);

        assertTrue(history.isEmpty());
    }

    @Test
    public void settBrukerTilManuellGrunnetReservasjonIKRR__skal_lage_manuell_status() {
        gittAktivOppfolging(AKTOR_ID);

        manuellStatusService.settBrukerTilManuellGrunnetReservasjonIKRR(AKTOR_ID);

        ManuellStatusEntity manuellStatus = manuellStatusRepository.hentSisteManuellStatus(AKTOR_ID).orElseThrow();

        assertTrue(manuellStatus.getId() > 0);
        assertEquals("Brukeren er reservert i Kontakt- og reservasjonsregisteret", manuellStatus.getBegrunnelse());
        assertEquals(SYSTEM, manuellStatus.getOpprettetAv());
        assertNull(manuellStatus.getOpprettetAvBrukerId());
        assertTrue(manuellStatus.getDato().isBefore(ZonedDateTime.now().plusSeconds(5)));
    }

    @Test
    public void settBrukerTilManuellGrunnetReservasjonIKRR__skal_ikke_lage_manuell_status_hvis_allerede_manuell() {
        gittAktivOppfolging(AKTOR_ID);

        ManuellStatusEntity manuellStatus = new ManuellStatusEntity()
                .setManuell(true)
                .setAktorId(AKTOR_ID.get())
                .setBegrunnelse("test");

        manuellStatusRepository.create(manuellStatus);

        manuellStatusService.settBrukerTilManuellGrunnetReservasjonIKRR(AKTOR_ID);

        List<ManuellStatusEntity> history = manuellStatusRepository.history(AKTOR_ID);

        assertEquals(1, history.size());
    }

    @Test
    public void hentDkifKontaktinfo__skal_returnere_kontaktinfo_fra_dkif() {
        DkifKontaktinfo kontaktinfo = new DkifKontaktinfo()
                .setPersonident(FNR.get())
                .setKanVarsles(true)
                .setReservert(true)
                .setEpostadresse("email")
                .setMobiltelefonnummer("12345");

        when(dkifClient.hentKontaktInfo(FNR)).thenReturn(Optional.of(kontaktinfo));

        assertEquals(kontaktinfo, manuellStatusService.hentDkifKontaktinfo(FNR));
    }

    @Test
    public void hentDkifKontaktinfo__skal_returnere_fallback_hvis_dkif_mangler_kontaktinfo() {
        when(dkifClient.hentKontaktInfo(FNR)).thenReturn(Optional.empty());

        DkifKontaktinfo fallbackKontaktInfo = new DkifKontaktinfo()
                .setPersonident(FNR.get())
                .setKanVarsles(true)
                .setReservert(false);

        assertEquals(fallbackKontaktInfo, manuellStatusService.hentDkifKontaktinfo(FNR));
    }


    private void gittAktivOppfolging(AktorId aktorId) {
        oppfolgingsStatusRepository.opprettOppfolging(aktorId);
        db.update("UPDATE OPPFOLGINGSTATUS SET UNDER_OPPFOLGING = ? WHERE AKTOR_ID = ?", true, aktorId.get());
    }
}
