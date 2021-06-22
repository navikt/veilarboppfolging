package no.nav.veilarboppfolging.service;

import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.auth.context.UserRole;
import no.nav.common.test.auth.AuthTestUtils;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV1;
import no.nav.veilarboppfolging.repository.UtmeldingRepository;
import no.nav.veilarboppfolging.repository.entity.UtmeldingEntity;
import no.nav.veilarboppfolging.test.DbTestUtils;
import no.nav.veilarboppfolging.test.LocalH2Database;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.ZonedDateTime;
import java.util.Optional;

import static java.time.ZonedDateTime.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class IservServiceIntegrationTest {

    private final static Fnr FNR = Fnr.of("879037942");

    private final static AktorId AKTOR_ID = AktorId.of("1234");

    private static int nesteAktorId;

    private ZonedDateTime iservFraDato = now();

    private IservService iservService;

    private UtmeldingRepository utmeldingRepository;

    private AuthService authService = mock(AuthService.class);

    private OppfolgingService oppfolgingService = mock(OppfolgingService.class);

    @Before
    public void setup() {
        JdbcTemplate db = LocalH2Database.getDb();
        TransactionTemplate transactor = DbTestUtils.createTransactor(db);

        DbTestUtils.cleanupTestDb();

        when(oppfolgingService.erUnderOppfolging(any())).thenReturn(true);
        when(oppfolgingService.avsluttOppfolgingForSystemBruker(any())).thenReturn(true);
        when(authService.getFnrOrThrow(any())).thenReturn(FNR);

        utmeldingRepository = new UtmeldingRepository(db);

        iservService = new IservService(
                AuthContextHolderThreadLocal.instance(),
                () -> AuthTestUtils.createAuthContext(UserRole.SYSTEM, "srvtest").getIdToken().serialize(),
                mock(MetricsService.class),
                utmeldingRepository, oppfolgingService, authService, transactor
        );
    }

    @Test
    public void behandleEndretBruker_skalLagreNyIservBruker() {
        EndringPaaOppfoelgingsBrukerV1 veilarbArenaOppfolging = getArenaBruker();
        assertTrue(utmeldingRepository.eksisterendeIservBruker(AKTOR_ID).isEmpty());

        iservService.behandleEndretBruker(veilarbArenaOppfolging);

        Optional<UtmeldingEntity> kanskjeUtmelding = utmeldingRepository.eksisterendeIservBruker(AKTOR_ID);

        assertTrue(kanskjeUtmelding.isPresent());

        UtmeldingEntity utmelding = kanskjeUtmelding.get();

        assertEquals(AKTOR_ID.get(), utmelding.getAktor_Id());
        assertEquals(iservFraDato.truncatedTo(MILLIS), utmelding.getIservSiden().truncatedTo(MILLIS));
    }

    @Test
    public void behandleEndretBruker_skalOppdatereEksisterendeIservBruker() {
        EndringPaaOppfoelgingsBrukerV1 veilarbArenaOppfolging = getArenaBruker();
        utmeldingRepository.insertUtmeldingTabell(AKTOR_ID, iservFraDato);
        assertTrue(utmeldingRepository.eksisterendeIservBruker(AKTOR_ID).isPresent());

        veilarbArenaOppfolging.setIserv_fra_dato(veilarbArenaOppfolging.getIserv_fra_dato().plusDays(2));
        iservService.behandleEndretBruker(veilarbArenaOppfolging);

        Optional<UtmeldingEntity> kanskjeUtmelding = utmeldingRepository.eksisterendeIservBruker(AKTOR_ID);

        assertTrue(kanskjeUtmelding.isPresent());

        UtmeldingEntity utmelding = kanskjeUtmelding.get();

        assertEquals(AKTOR_ID.get(), utmelding.getAktor_Id());
        assertEquals(veilarbArenaOppfolging.getIserv_fra_dato().truncatedTo(MILLIS), utmelding.getIservSiden().truncatedTo(MILLIS));
    }

    @Test
    public void behandleEndretBruker_skalSletteBrukerSomIkkeLengerErIserv() {
        EndringPaaOppfoelgingsBrukerV1 veilarbArenaOppfolging = getArenaBruker();
        utmeldingRepository.insertUtmeldingTabell(AKTOR_ID, iservFraDato);
        assertTrue(utmeldingRepository.eksisterendeIservBruker(AKTOR_ID).isPresent());

        veilarbArenaOppfolging.setFormidlingsgruppekode("ARBS");
        iservService.behandleEndretBruker(veilarbArenaOppfolging);
        assertTrue(utmeldingRepository.eksisterendeIservBruker(AKTOR_ID).isEmpty());
    }

    @Test
    public void behandleEndretBruker_skalStarteBrukerSomHarOppfolgingsstatus() {
        EndringPaaOppfoelgingsBrukerV1 veilarbArenaOppfolging = getArenaBruker();
        veilarbArenaOppfolging.setFormidlingsgruppekode("ARBS");
        when(oppfolgingService.erUnderOppfolging(any())).thenReturn(false);

        iservService.behandleEndretBruker(veilarbArenaOppfolging);
        verify(oppfolgingService).startOppfolgingHvisIkkeAlleredeStartet(AKTOR_ID);
    }

    @Test
    public void behandleEndretBruker_skalIkkeStarteBrukerSomHarOppfolgingsstatusDersomAlleredeUnderOppfolging() {
        EndringPaaOppfoelgingsBrukerV1 veilarbArenaOppfolging = getArenaBruker();
        veilarbArenaOppfolging.setFormidlingsgruppekode("ARBS");

        iservService.behandleEndretBruker(veilarbArenaOppfolging);
        verify(oppfolgingService, never()).startOppfolgingHvisIkkeAlleredeStartet(any(AktorId.class));
    }
  
    @Test
    public void behandleEndretBruker_skalIkkeStarteBrukerSomIkkeHarOppfolgingsstatus() {
        EndringPaaOppfoelgingsBrukerV1 veilarbArenaOppfolging = getArenaBruker();
        veilarbArenaOppfolging.setFormidlingsgruppekode("IARBS");
        veilarbArenaOppfolging.setKvalifiseringsgruppekode("IkkeOppfolging");
        when(oppfolgingService.erUnderOppfolging(any())).thenReturn(false);

        iservService.behandleEndretBruker(veilarbArenaOppfolging);
        verifyNoInteractions(oppfolgingService);
    }

    @Test
    public void finnBrukereMedIservI28Dager() {
        assertTrue(utmeldingRepository.finnBrukereMedIservI28Dager().isEmpty());

        insertIservBruker(now().minusDays(30));
        insertIservBruker(now().minusDays(27));
        insertIservBruker(now().minusDays(15));
        insertIservBruker(now());

        assertEquals(1, utmeldingRepository.finnBrukereMedIservI28Dager().size());
    }

    @Test
    public void avsluttOppfolging(){
        EndringPaaOppfoelgingsBrukerV1 veilarbArenaOppfolging = insertIservBruker(iservFraDato);
        AktorId aktorId = AktorId.of(veilarbArenaOppfolging.getAktoerid());
        assertTrue(utmeldingRepository.eksisterendeIservBruker(aktorId).isPresent());

        iservService.avslutteOppfolging(aktorId);

        verify(oppfolgingService).avsluttOppfolgingForSystemBruker(any());
        assertTrue(utmeldingRepository.eksisterendeIservBruker(aktorId).isEmpty());
    }

    @Test
    public void automatiskAvslutteOppfolging_skalAvslutteBrukerSomErIserv28dagerOgUnderOppfolging(){
        insertIservBruker(now().minusDays(30));

        iservService.automatiskAvslutteOppfolging();

        assertTrue(utmeldingRepository.eksisterendeIservBruker(AKTOR_ID).isEmpty());
    }

    @Test
    public void automatiskAvslutteOppfolging_skalFjerneBrukerSomErIserv28dagerOgIkkeUnderOppfolging(){
        EndringPaaOppfoelgingsBrukerV1 bruker = insertIservBruker(now().minusDays(30));
        when(oppfolgingService.erUnderOppfolging(AktorId.of(bruker.getAktoerid()))).thenReturn(false);

        iservService.automatiskAvslutteOppfolging();

        verify(oppfolgingService, never()).avsluttOppfolgingForSystemBruker(any(Fnr.class));

        assertTrue(utmeldingRepository.eksisterendeIservBruker(AKTOR_ID).isEmpty());
    }
    
    @Test
    public void automatiskAvslutteOppfolging_skalIkkeFjerneBrukerSomErIserv28dagerMenIkkeAvsluttet(){
        EndringPaaOppfoelgingsBrukerV1 bruker = insertIservBruker(now().minusDays(30));
        when(oppfolgingService.avsluttOppfolgingForSystemBruker(any())).thenReturn(false);

        iservService.automatiskAvslutteOppfolging();

        assertTrue(utmeldingRepository.eksisterendeIservBruker(AktorId.of(bruker.getAktoerid())).isPresent());
    }
    
    private EndringPaaOppfoelgingsBrukerV1 getArenaBruker() {
        EndringPaaOppfoelgingsBrukerV1 veilarbArenaOppfolging = new EndringPaaOppfoelgingsBrukerV1();
        veilarbArenaOppfolging.setAktoerid(AKTOR_ID.get());
        veilarbArenaOppfolging.setFormidlingsgruppekode("ISERV");
        veilarbArenaOppfolging.setIserv_fra_dato(iservFraDato);
        return veilarbArenaOppfolging;
    }

    private EndringPaaOppfoelgingsBrukerV1 insertIservBruker(ZonedDateTime iservFraDato) {
        EndringPaaOppfoelgingsBrukerV1 veilarbArenaOppfolging = new EndringPaaOppfoelgingsBrukerV1();
        veilarbArenaOppfolging.setAktoerid(Integer.toString(nesteAktorId++));
        veilarbArenaOppfolging.setFormidlingsgruppekode("ISERV");
        veilarbArenaOppfolging.setIserv_fra_dato(iservFraDato);
        veilarbArenaOppfolging.setFodselsnr("1111");

        assertTrue(utmeldingRepository.eksisterendeIservBruker(AKTOR_ID).isEmpty());
        iservService.behandleEndretBruker(veilarbArenaOppfolging);
        return veilarbArenaOppfolging;
    }
}
