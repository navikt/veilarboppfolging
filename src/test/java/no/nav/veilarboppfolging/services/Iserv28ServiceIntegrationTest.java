package no.nav.veilarboppfolging.services;

import net.javacrumbs.shedlock.core.LockingTaskExecutor;

import no.nav.dialogarena.aktor.AktorService;
import no.nav.veilarboppfolging.test.DatabaseTest;
import no.nav.veilarboppfolging.db.OppfolgingRepository;
import no.nav.veilarboppfolging.db.OppfolgingsStatusRepository;
import no.nav.veilarboppfolging.domain.IservMapper;
import no.nav.veilarboppfolging.domain.OppfolgingTable;

import no.nav.veilarboppfolging.domain.VeilarbArenaOppfolgingEndret;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;
import java.time.ZonedDateTime;

import static java.time.ZonedDateTime.now;
import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class Iserv28ServiceIntegrationTest extends DatabaseTest {

    private static int nesteAktorId;

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Inject
    private LockingTaskExecutor taskExecutor;

    private IservService iservService;

    private ZonedDateTime iservFraDato = now();
    private final static String AKTORID = "1234";

    private OppfolgingsStatusRepository oppfolgingStatusRepository = mock(OppfolgingsStatusRepository.class);

    private OppfolgingService oppfolgingService = mock(OppfolgingService.class);
    private OppfolgingRepository oppfolgingRepository = mock(OppfolgingRepository.class);

    @Before
    public void setup() throws Exception {
        AktorService aktorService = mock(AktorService.class);
        when(aktorService.getFnr(anyString())).thenReturn(of("fnr"));
        when(oppfolgingStatusRepository.fetch(anyString())).thenReturn(new OppfolgingTable().setUnderOppfolging(true));
        when(oppfolgingService.avsluttOppfolgingForSystemBruker(anyString(), anyString(), anyString())).thenReturn(true);
        SystemUserSubjectProvider systemUserSubjectProvider = mock(SystemUserSubjectProvider.class);
        iservService = new IservService(jdbcTemplate, oppfolgingService, oppfolgingStatusRepository, oppfolgingRepository, aktorService, taskExecutor, systemUserSubjectProvider);
    }

    @Test
    public void behandleEndretBruker_skalLagreNyIservBruker() {
        VeilarbArenaOppfolgingEndret veilarbArenaOppfolging = getArenaBruker();
        assertThat(iservService.eksisterendeIservBruker(veilarbArenaOppfolging)).isNull();

        iservService.behandleEndretBruker(veilarbArenaOppfolging);

        IservMapper iservMapper = iservService.eksisterendeIservBruker(veilarbArenaOppfolging);
        assertThat(iservMapper).isNotNull();
        assertThat(iservMapper.getAktor_Id()).isEqualTo(AKTORID);
        assertThat(iservMapper.getIservSiden()).isEqualTo(iservFraDato);
    }

    @Test
    public void behandleEndretBruker_skalOppdatereEksisterendeIservBruker() {
        VeilarbArenaOppfolgingEndret veilarbArenaOppfolging = getArenaBruker();
        iservService.insertUtmeldingTabell(veilarbArenaOppfolging);
        assertThat(iservService.eksisterendeIservBruker(veilarbArenaOppfolging)).isNotNull();

        veilarbArenaOppfolging.setIserv_fra_dato(veilarbArenaOppfolging.iserv_fra_dato.plusDays(2));
        iservService.behandleEndretBruker(veilarbArenaOppfolging);

        IservMapper iservMapper = iservService.eksisterendeIservBruker(veilarbArenaOppfolging);
        assertThat(iservMapper).isNotNull();
        assertThat(iservMapper.getAktor_Id()).isEqualTo(AKTORID);
        assertThat(iservMapper.getIservSiden()).isEqualTo(veilarbArenaOppfolging.getIserv_fra_dato());
    }

    @Test
    public void behandleEndretBruker_skalSletteBrukerSomIkkeLengerErIserv() {
        VeilarbArenaOppfolgingEndret veilarbArenaOppfolging = getArenaBruker();
        iservService.insertUtmeldingTabell(veilarbArenaOppfolging);
        assertThat(iservService.eksisterendeIservBruker(veilarbArenaOppfolging)).isNotNull();

        veilarbArenaOppfolging.setFormidlingsgruppekode("ARBS");
        iservService.behandleEndretBruker(veilarbArenaOppfolging);
        assertThat(iservService.eksisterendeIservBruker(veilarbArenaOppfolging)).isNull();
    }

    @Test
    public void behandleEndretBruker_skalStarteBrukerSomHarOppfolgingsstatus() {
        VeilarbArenaOppfolgingEndret veilarbArenaOppfolging = getArenaBruker();
        veilarbArenaOppfolging.setFormidlingsgruppekode("ARBS");
        when(oppfolgingStatusRepository.fetch(anyString())).thenReturn(new OppfolgingTable().setUnderOppfolging(false));

        iservService.behandleEndretBruker(veilarbArenaOppfolging);
        verify(oppfolgingRepository).startOppfolgingHvisIkkeAlleredeStartet(AKTORID);
    }

    @Test
    public void behandleEndretBruker_skalIkkeStarteBrukerSomHarOppfolgingsstatusDersomAlleredeUnderOppfolging() {
        VeilarbArenaOppfolgingEndret veilarbArenaOppfolging = getArenaBruker();
        veilarbArenaOppfolging.setFormidlingsgruppekode("ARBS");

        iservService.behandleEndretBruker(veilarbArenaOppfolging);
        verifyZeroInteractions(oppfolgingRepository);
    }
  
    @Test
    public void behandleEndretBruker_skalIkkeStarteBrukerSomIkkeHarOppfolgingsstatus() {
        VeilarbArenaOppfolgingEndret veilarbArenaOppfolging = getArenaBruker();
        veilarbArenaOppfolging.setFormidlingsgruppekode("IARBS");
        veilarbArenaOppfolging.setKvalifiseringsgruppekode("IkkeOppfolging");
        when(oppfolgingStatusRepository.fetch(anyString())).thenReturn(new OppfolgingTable().setUnderOppfolging(false));

        iservService.behandleEndretBruker(veilarbArenaOppfolging);
        verifyZeroInteractions(oppfolgingRepository);
    }

    @Test
    public void finnBrukereMedIservI28Dager() {
        assertThat(iservService.finnBrukereMedIservI28Dager()).isEmpty();

        insertIservBruker(now().minusDays(30));
        insertIservBruker(now().minusDays(27));
        insertIservBruker(now().minusDays(15));
        insertIservBruker(now());

        assertThat(iservService.finnBrukereMedIservI28Dager()).hasSize(1);
    }

    @Test
    public void avsluttOppfolging(){
        VeilarbArenaOppfolgingEndret veilarbArenaOppfolging = insertIservBruker(iservFraDato);
        assertThat(iservService.eksisterendeIservBruker(veilarbArenaOppfolging)).isNotNull();

        iservService.avslutteOppfolging(veilarbArenaOppfolging.aktoerid);

        verify(oppfolgingService).avsluttOppfolgingForSystemBruker(anyString(), anyString(), anyString());
        assertThat(iservService.eksisterendeIservBruker(veilarbArenaOppfolging)).isNull();
    }

    @Test
    public void automatiskAvslutteOppfolging_skalAvslutteBrukerSomErIserv28dagerOgUnderOppfolging(){
        VeilarbArenaOppfolgingEndret bruker = insertIservBruker(now().minusDays(30));

        iservService.automatiskAvslutteOppfolging();

        assertThat(iservService.eksisterendeIservBruker(bruker)).isNull();
    }

    @Test
    public void automatiskAvslutteOppfolging_skalFjerneBrukerSomErIserv28dagerOgIkkeUnderOppfolging(){
        VeilarbArenaOppfolgingEndret bruker = insertIservBruker(now().minusDays(30));
        when(oppfolgingStatusRepository.fetch(bruker.aktoerid)).thenReturn(new OppfolgingTable().setUnderOppfolging(false));

        iservService.automatiskAvslutteOppfolging();

        verifyZeroInteractions(oppfolgingService);
        assertThat(iservService.eksisterendeIservBruker(bruker)).isNull();
    }
    
    @Test
    public void automatiskAvslutteOppfolging_skalIkkeFjerneBrukerSomErIserv28dagerMenIkkeAvsluttet(){
        VeilarbArenaOppfolgingEndret bruker = insertIservBruker(now().minusDays(30));
        when(oppfolgingService.avsluttOppfolgingForSystemBruker(anyString(), anyString(), anyString())).thenReturn(false);

        iservService.automatiskAvslutteOppfolging();

        assertThat(iservService.eksisterendeIservBruker(bruker)).isNotNull();
    }
    
    private VeilarbArenaOppfolgingEndret getArenaBruker() {
        VeilarbArenaOppfolgingEndret veilarbArenaOppfolging = new VeilarbArenaOppfolgingEndret();
        veilarbArenaOppfolging.setAktoerid(AKTORID);
        veilarbArenaOppfolging.setFormidlingsgruppekode("ISERV");
        veilarbArenaOppfolging.setIserv_fra_dato(iservFraDato);
        return veilarbArenaOppfolging;
    }

    private VeilarbArenaOppfolgingEndret insertIservBruker(ZonedDateTime iservFraDato) {
        VeilarbArenaOppfolgingEndret veilarbArenaOppfolging = new VeilarbArenaOppfolgingEndret();
        veilarbArenaOppfolging.setAktoerid(Integer.toString(nesteAktorId++));
        veilarbArenaOppfolging.setFormidlingsgruppekode("ISERV");
        veilarbArenaOppfolging.setIserv_fra_dato(iservFraDato);
        veilarbArenaOppfolging.setFodselsnr("1111");

        assertThat(iservService.eksisterendeIservBruker(veilarbArenaOppfolging)).isNull();
        iservService.behandleEndretBruker(veilarbArenaOppfolging);
        return veilarbArenaOppfolging;
    }
}
