package no.nav.veilarboppfolging.service;

import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.auth.context.UserRole;
import no.nav.common.test.auth.AuthTestUtils;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV1;
import no.nav.veilarboppfolging.domain.IservMapper;
import no.nav.veilarboppfolging.domain.OppfolgingTable;
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository;
import no.nav.veilarboppfolging.repository.UtmeldingRepository;
import no.nav.veilarboppfolging.test.DbTestUtils;
import no.nav.veilarboppfolging.test.LocalH2Database;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.ZonedDateTime;

import static java.time.ZonedDateTime.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class IservServiceIntegrationTest {

    private final static Fnr FNR = Fnr.of("879037942");

    private final static AktorId AKTOR_ID = AktorId.of("1234");

    private static int nesteAktorId;

    private ZonedDateTime iservFraDato = now();

    private IservService iservService;

    private UtmeldingRepository utmeldingRepository;

    private AuthService authService = mock(AuthService.class);

    private OppfolgingsStatusRepository oppfolgingStatusRepository = mock(OppfolgingsStatusRepository.class);

    private OppfolgingService oppfolgingService = mock(OppfolgingService.class);

    @Before
    public void setup() {
        JdbcTemplate db = LocalH2Database.getDb();
        TransactionTemplate transactor = DbTestUtils.createTransactor(db);

        DbTestUtils.cleanupTestDb();

        when(oppfolgingStatusRepository.fetch(any())).thenReturn(new OppfolgingTable().setUnderOppfolging(true));
        when(oppfolgingService.avsluttOppfolgingForSystemBruker(any())).thenReturn(true);
        when(authService.getFnrOrThrow(any())).thenReturn(FNR);

        utmeldingRepository = new UtmeldingRepository(db);

        iservService = new IservService(
                AuthContextHolderThreadLocal.instance(),
                () -> AuthTestUtils.createAuthContext(UserRole.SYSTEM, "srvtest").getIdToken().serialize(),
                mock(MetricsService.class),
                utmeldingRepository, oppfolgingService, oppfolgingStatusRepository, authService, transactor
        );
    }

    @Test
    public void behandleEndretBruker_skalLagreNyIservBruker() {
        EndringPaaOppfoelgingsBrukerV1 veilarbArenaOppfolging = getArenaBruker();
        assertThat(utmeldingRepository.eksisterendeIservBruker(veilarbArenaOppfolging)).isNull();

        iservService.behandleEndretBruker(veilarbArenaOppfolging);

        IservMapper iservMapper = utmeldingRepository.eksisterendeIservBruker(veilarbArenaOppfolging);
        assertThat(iservMapper).isNotNull();
        assertThat(iservMapper.getAktor_Id()).isEqualTo(AKTOR_ID.get());
        assertThat(iservMapper.getIservSiden().truncatedTo(MILLIS)).isEqualTo(iservFraDato.truncatedTo(MILLIS));
    }

    @Test
    public void behandleEndretBruker_skalOppdatereEksisterendeIservBruker() {
        EndringPaaOppfoelgingsBrukerV1 veilarbArenaOppfolging = getArenaBruker();
        utmeldingRepository.insertUtmeldingTabell(veilarbArenaOppfolging);
        assertThat(utmeldingRepository.eksisterendeIservBruker(veilarbArenaOppfolging)).isNotNull();

        veilarbArenaOppfolging.setIserv_fra_dato(veilarbArenaOppfolging.getIserv_fra_dato().plusDays(2));
        iservService.behandleEndretBruker(veilarbArenaOppfolging);

        IservMapper iservMapper = utmeldingRepository.eksisterendeIservBruker(veilarbArenaOppfolging);
        assertThat(iservMapper).isNotNull();
        assertThat(iservMapper.getAktor_Id()).isEqualTo(AKTOR_ID.get());
        assertThat(iservMapper.getIservSiden().truncatedTo(MILLIS)).isEqualTo(veilarbArenaOppfolging.getIserv_fra_dato().truncatedTo(MILLIS));
    }

    @Test
    public void behandleEndretBruker_skalSletteBrukerSomIkkeLengerErIserv() {
        EndringPaaOppfoelgingsBrukerV1 veilarbArenaOppfolging = getArenaBruker();
        utmeldingRepository.insertUtmeldingTabell(veilarbArenaOppfolging);
        assertThat(utmeldingRepository.eksisterendeIservBruker(veilarbArenaOppfolging)).isNotNull();

        veilarbArenaOppfolging.setFormidlingsgruppekode("ARBS");
        iservService.behandleEndretBruker(veilarbArenaOppfolging);
        assertThat(utmeldingRepository.eksisterendeIservBruker(veilarbArenaOppfolging)).isNull();
    }

    @Test
    public void behandleEndretBruker_skalStarteBrukerSomHarOppfolgingsstatus() {
        EndringPaaOppfoelgingsBrukerV1 veilarbArenaOppfolging = getArenaBruker();
        veilarbArenaOppfolging.setFormidlingsgruppekode("ARBS");
        when(oppfolgingStatusRepository.fetch(any())).thenReturn(new OppfolgingTable().setUnderOppfolging(false));

        iservService.behandleEndretBruker(veilarbArenaOppfolging);
        verify(oppfolgingService).startOppfolgingHvisIkkeAlleredeStartet(AKTOR_ID);
    }

    @Test
    public void behandleEndretBruker_skalIkkeStarteBrukerSomHarOppfolgingsstatusDersomAlleredeUnderOppfolging() {
        EndringPaaOppfoelgingsBrukerV1 veilarbArenaOppfolging = getArenaBruker();
        veilarbArenaOppfolging.setFormidlingsgruppekode("ARBS");

        iservService.behandleEndretBruker(veilarbArenaOppfolging);
        verifyZeroInteractions(oppfolgingService);
    }
  
    @Test
    public void behandleEndretBruker_skalIkkeStarteBrukerSomIkkeHarOppfolgingsstatus() {
        EndringPaaOppfoelgingsBrukerV1 veilarbArenaOppfolging = getArenaBruker();
        veilarbArenaOppfolging.setFormidlingsgruppekode("IARBS");
        veilarbArenaOppfolging.setKvalifiseringsgruppekode("IkkeOppfolging");
        when(oppfolgingStatusRepository.fetch(any())).thenReturn(new OppfolgingTable().setUnderOppfolging(false));

        iservService.behandleEndretBruker(veilarbArenaOppfolging);
        verifyZeroInteractions(oppfolgingService);
    }

    @Test
    public void finnBrukereMedIservI28Dager() {
        assertThat(utmeldingRepository.finnBrukereMedIservI28Dager()).isEmpty();

        insertIservBruker(now().minusDays(30));
        insertIservBruker(now().minusDays(27));
        insertIservBruker(now().minusDays(15));
        insertIservBruker(now());

        assertThat(utmeldingRepository.finnBrukereMedIservI28Dager()).hasSize(1);
    }

    @Test
    public void avsluttOppfolging(){
        EndringPaaOppfoelgingsBrukerV1 veilarbArenaOppfolging = insertIservBruker(iservFraDato);
        assertThat(utmeldingRepository.eksisterendeIservBruker(veilarbArenaOppfolging)).isNotNull();

        iservService.avslutteOppfolging(AktorId.of(veilarbArenaOppfolging.getAktoerid()));

        verify(oppfolgingService).avsluttOppfolgingForSystemBruker(any());
        assertThat(utmeldingRepository.eksisterendeIservBruker(veilarbArenaOppfolging)).isNull();
    }

    @Test
    public void automatiskAvslutteOppfolging_skalAvslutteBrukerSomErIserv28dagerOgUnderOppfolging(){
        EndringPaaOppfoelgingsBrukerV1 bruker = insertIservBruker(now().minusDays(30));

        iservService.automatiskAvslutteOppfolging();

        assertThat(utmeldingRepository.eksisterendeIservBruker(bruker)).isNull();
    }

    @Test
    public void automatiskAvslutteOppfolging_skalFjerneBrukerSomErIserv28dagerOgIkkeUnderOppfolging(){
        EndringPaaOppfoelgingsBrukerV1 bruker = insertIservBruker(now().minusDays(30));
        when(oppfolgingStatusRepository.fetch(AktorId.of(bruker.getAktoerid()))).thenReturn(new OppfolgingTable().setUnderOppfolging(false));

        iservService.automatiskAvslutteOppfolging();

        verifyNoInteractions(oppfolgingService);
        assertThat(utmeldingRepository.eksisterendeIservBruker(bruker)).isNull();
    }
    
    @Test
    public void automatiskAvslutteOppfolging_skalIkkeFjerneBrukerSomErIserv28dagerMenIkkeAvsluttet(){
        EndringPaaOppfoelgingsBrukerV1 bruker = insertIservBruker(now().minusDays(30));
        when(oppfolgingService.avsluttOppfolgingForSystemBruker(any())).thenReturn(false);

        iservService.automatiskAvslutteOppfolging();

        assertThat(utmeldingRepository.eksisterendeIservBruker(bruker)).isNotNull();
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

        assertThat(utmeldingRepository.eksisterendeIservBruker(veilarbArenaOppfolging)).isNull();
        iservService.behandleEndretBruker(veilarbArenaOppfolging);
        return veilarbArenaOppfolging;
    }
}
