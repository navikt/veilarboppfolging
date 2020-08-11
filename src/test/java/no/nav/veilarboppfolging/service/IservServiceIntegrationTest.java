package no.nav.veilarboppfolging.service;

import no.nav.veilarboppfolging.domain.IservMapper;
import no.nav.veilarboppfolging.domain.OppfolgingTable;
import no.nav.veilarboppfolging.domain.VeilarbArenaOppfolgingEndret;
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository;
import no.nav.veilarboppfolging.repository.UtmeldingRepository;
import no.nav.veilarboppfolging.test.DbTestUtils;
import no.nav.veilarboppfolging.test.LocalH2Database;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.ZonedDateTime;

import static java.time.ZonedDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class IservServiceIntegrationTest {

    private final static String AKTORID = "1234";

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

        DbTestUtils.cleanupTestDb();

        when(oppfolgingStatusRepository.fetch(anyString())).thenReturn(new OppfolgingTable().setUnderOppfolging(true));
        when(oppfolgingService.avsluttOppfolgingForSystemBruker(anyString(), anyString(), anyString())).thenReturn(true);
        when(authService.getFnrOrThrow(anyString())).thenReturn(AKTORID);

        utmeldingRepository = new UtmeldingRepository(db);

        iservService = new IservService(
                serviceUserCredentials, systemUserTokenProvider, mock(MetricsService.class), utmeldingRepository,
                oppfolgingService, oppfolgingStatusRepository, authService);
    }

    @Test
    public void behandleEndretBruker_skalLagreNyIservBruker() {
        VeilarbArenaOppfolgingEndret veilarbArenaOppfolging = getArenaBruker();
        assertThat(utmeldingRepository.eksisterendeIservBruker(veilarbArenaOppfolging)).isNull();

        iservService.behandleEndretBruker(veilarbArenaOppfolging);

        IservMapper iservMapper = utmeldingRepository.eksisterendeIservBruker(veilarbArenaOppfolging);
        assertThat(iservMapper).isNotNull();
        assertThat(iservMapper.getAktor_Id()).isEqualTo(AKTORID);
        assertThat(iservMapper.getIservSiden()).isEqualTo(iservFraDato);
    }

    @Test
    public void behandleEndretBruker_skalOppdatereEksisterendeIservBruker() {
        VeilarbArenaOppfolgingEndret veilarbArenaOppfolging = getArenaBruker();
        utmeldingRepository.insertUtmeldingTabell(veilarbArenaOppfolging);
        assertThat(utmeldingRepository.eksisterendeIservBruker(veilarbArenaOppfolging)).isNotNull();

        veilarbArenaOppfolging.setIserv_fra_dato(veilarbArenaOppfolging.iserv_fra_dato.plusDays(2));
        iservService.behandleEndretBruker(veilarbArenaOppfolging);

        IservMapper iservMapper = utmeldingRepository.eksisterendeIservBruker(veilarbArenaOppfolging);
        assertThat(iservMapper).isNotNull();
        assertThat(iservMapper.getAktor_Id()).isEqualTo(AKTORID);
        assertThat(iservMapper.getIservSiden()).isEqualTo(veilarbArenaOppfolging.getIserv_fra_dato());
    }

    @Test
    public void behandleEndretBruker_skalSletteBrukerSomIkkeLengerErIserv() {
        VeilarbArenaOppfolgingEndret veilarbArenaOppfolging = getArenaBruker();
        utmeldingRepository.insertUtmeldingTabell(veilarbArenaOppfolging);
        assertThat(utmeldingRepository.eksisterendeIservBruker(veilarbArenaOppfolging)).isNotNull();

        veilarbArenaOppfolging.setFormidlingsgruppekode("ARBS");
        iservService.behandleEndretBruker(veilarbArenaOppfolging);
        assertThat(utmeldingRepository.eksisterendeIservBruker(veilarbArenaOppfolging)).isNull();
    }

    @Test
    public void behandleEndretBruker_skalStarteBrukerSomHarOppfolgingsstatus() {
        VeilarbArenaOppfolgingEndret veilarbArenaOppfolging = getArenaBruker();
        veilarbArenaOppfolging.setFormidlingsgruppekode("ARBS");
        when(oppfolgingStatusRepository.fetch(anyString())).thenReturn(new OppfolgingTable().setUnderOppfolging(false));

        iservService.behandleEndretBruker(veilarbArenaOppfolging);
        verify(oppfolgingService).startOppfolgingHvisIkkeAlleredeStartet(AKTORID);
    }

    @Test
    public void behandleEndretBruker_skalIkkeStarteBrukerSomHarOppfolgingsstatusDersomAlleredeUnderOppfolging() {
        VeilarbArenaOppfolgingEndret veilarbArenaOppfolging = getArenaBruker();
        veilarbArenaOppfolging.setFormidlingsgruppekode("ARBS");

        iservService.behandleEndretBruker(veilarbArenaOppfolging);
        verifyZeroInteractions(oppfolgingService);
    }
  
    @Test
    public void behandleEndretBruker_skalIkkeStarteBrukerSomIkkeHarOppfolgingsstatus() {
        VeilarbArenaOppfolgingEndret veilarbArenaOppfolging = getArenaBruker();
        veilarbArenaOppfolging.setFormidlingsgruppekode("IARBS");
        veilarbArenaOppfolging.setKvalifiseringsgruppekode("IkkeOppfolging");
        when(oppfolgingStatusRepository.fetch(anyString())).thenReturn(new OppfolgingTable().setUnderOppfolging(false));

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
        VeilarbArenaOppfolgingEndret veilarbArenaOppfolging = insertIservBruker(iservFraDato);
        assertThat(utmeldingRepository.eksisterendeIservBruker(veilarbArenaOppfolging)).isNotNull();

        iservService.avslutteOppfolging(veilarbArenaOppfolging.aktoerid);

        verify(oppfolgingService).avsluttOppfolgingForSystemBruker(anyString(), anyString(), anyString());
        assertThat(utmeldingRepository.eksisterendeIservBruker(veilarbArenaOppfolging)).isNull();
    }

    @Test
    public void automatiskAvslutteOppfolging_skalAvslutteBrukerSomErIserv28dagerOgUnderOppfolging(){
        VeilarbArenaOppfolgingEndret bruker = insertIservBruker(now().minusDays(30));

        iservService.automatiskAvslutteOppfolging();

        assertThat(utmeldingRepository.eksisterendeIservBruker(bruker)).isNull();
    }

    @Test
    public void automatiskAvslutteOppfolging_skalFjerneBrukerSomErIserv28dagerOgIkkeUnderOppfolging(){
        VeilarbArenaOppfolgingEndret bruker = insertIservBruker(now().minusDays(30));
        when(oppfolgingStatusRepository.fetch(bruker.aktoerid)).thenReturn(new OppfolgingTable().setUnderOppfolging(false));

        iservService.automatiskAvslutteOppfolging();

        verifyNoInteractions(oppfolgingService);
        assertThat(utmeldingRepository.eksisterendeIservBruker(bruker)).isNull();
    }
    
    @Test
    public void automatiskAvslutteOppfolging_skalIkkeFjerneBrukerSomErIserv28dagerMenIkkeAvsluttet(){
        VeilarbArenaOppfolgingEndret bruker = insertIservBruker(now().minusDays(30));
        when(oppfolgingService.avsluttOppfolgingForSystemBruker(anyString(), anyString(), anyString())).thenReturn(false);

        iservService.automatiskAvslutteOppfolging();

        assertThat(utmeldingRepository.eksisterendeIservBruker(bruker)).isNotNull();
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

        assertThat(utmeldingRepository.eksisterendeIservBruker(veilarbArenaOppfolging)).isNull();
        iservService.behandleEndretBruker(veilarbArenaOppfolging);
        return veilarbArenaOppfolging;
    }
}
