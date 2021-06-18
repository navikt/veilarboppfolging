package no.nav.veilarboppfolging.service;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.client.dkif.DkifClient;
import no.nav.veilarboppfolging.client.dkif.DkifKontaktinfo;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolging;
import no.nav.veilarboppfolging.domain.ManuellStatus;
import no.nav.veilarboppfolging.repository.ManuellStatusRepository;
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository;
import no.nav.veilarboppfolging.test.DbTestUtils;
import no.nav.veilarboppfolging.test.IsolatedDatabaseTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static no.nav.veilarboppfolging.domain.KodeverkBruker.NAV;
import static no.nav.veilarboppfolging.domain.KodeverkBruker.SYSTEM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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
        when(dkifClient.hentKontaktInfo(FNR)).thenReturn(new DkifKontaktinfo());
        gittAktivOppfolging(AKTOR_ID);

        String begrunnelse = "test begrunnelse";
        String opprettetAvBruker = "test opprettet av";

        manuellStatusService.oppdaterManuellStatus(FNR, true, begrunnelse, SYSTEM, opprettetAvBruker);

        long gjeldendeManuellStatusId = oppfolgingsStatusRepository.fetch(AKTOR_ID).getGjeldendeManuellStatusId();
        ManuellStatus gjeldendeManuellStatus = manuellStatusRepository.fetch(gjeldendeManuellStatusId);

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

    private void gittAktivOppfolging(AktorId aktorId) {
        oppfolgingsStatusRepository.opprettOppfolging(aktorId);
        db.update("UPDATE OPPFOLGINGSTATUS SET UNDER_OPPFOLGING = ? WHERE AKTOR_ID = ?", true, aktorId.get());
    }
}
