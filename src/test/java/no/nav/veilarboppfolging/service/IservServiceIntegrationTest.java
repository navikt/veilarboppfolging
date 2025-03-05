package no.nav.veilarboppfolging.service;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto_schema.enums.arena.Formidlingsgruppe;
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2;
import no.nav.veilarboppfolging.LocalDatabaseSingleton;
import no.nav.veilarboppfolging.domain.AvslutningStatusData;
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.Avregistrering;
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.UtmeldtEtter28Dager;
import no.nav.veilarboppfolging.repository.UtmeldingRepository;
import no.nav.veilarboppfolging.repository.entity.UtmeldingEntity;
import no.nav.veilarboppfolging.service.utmelding.KanskjeIservBruker;
import no.nav.veilarboppfolging.test.DbTestUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.ZonedDateTime;
import java.util.Optional;

import static java.time.ZonedDateTime.now;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class IservServiceIntegrationTest {

    private final static Fnr FNR = Fnr.of("879037942");
    private final static AktorId AKTOR_ID = AktorId.of("1234");
    private static int nesteFnr;
    private ZonedDateTime iservFraDato = now();
    private IservService iservService;
    private UtmeldingRepository utmeldingRepository;
    private AuthService authService = mock(AuthService.class);
    private OppfolgingService oppfolgingService = mock(OppfolgingService.class);

    @Before
    public void setup() {
        JdbcTemplate db = LocalDatabaseSingleton.INSTANCE.getJdbcTemplate();

        DbTestUtils.cleanupTestDb();

        when(oppfolgingService.erUnderOppfolging(any(AktorId.class))).thenReturn(true);
        when(oppfolgingService.avsluttOppfolging(any(Avregistrering.class))).thenReturn(AvslutningStatusData.builder().underOppfolging(false).build());
        when(authService.getFnrOrThrow(any())).thenReturn(FNR);

        utmeldingRepository = new UtmeldingRepository(db);

        iservService = new IservService(mock(MetricsService.class), utmeldingRepository, oppfolgingService, authService);
    }

    @Test
    public void oppdaterUtmeldingsStatus_skalLagreNyIservBruker() {
        when(authService.getAktorIdOrThrow(FNR)).thenReturn(AKTOR_ID);
        var brukerV2 = kanskjeIservBruker();
        assertTrue(utmeldingRepository.eksisterendeIservBruker(AKTOR_ID).isEmpty());
        iservService.oppdaterUtmeldingsStatus(brukerV2);
        Optional<UtmeldingEntity> kanskjeUtmelding = utmeldingRepository.eksisterendeIservBruker(AKTOR_ID);
        assertTrue(kanskjeUtmelding.isPresent());
        UtmeldingEntity utmelding = kanskjeUtmelding.get();
        assertEquals(AKTOR_ID.get(), utmelding.getAktor_Id());
        assertEquals(iservFraDato.toLocalDate(), utmelding.getIservSiden().toLocalDate());
    }

    @Test
    public void oppdaterUtmeldingsStatus_skalOppdatereEksisterendeIservBruker() {
        when(authService.getAktorIdOrThrow(FNR)).thenReturn(AKTOR_ID);
        var brukerV2 = kanskjeIservBruker(iservFraDato.plusDays(2), Formidlingsgruppe.ISERV);
        utmeldingRepository.insertUtmeldingTabell(AKTOR_ID, iservFraDato);
        assertTrue(utmeldingRepository.eksisterendeIservBruker(AKTOR_ID).isPresent());
        iservService.oppdaterUtmeldingsStatus(brukerV2);
        Optional<UtmeldingEntity> kanskjeUtmelding = utmeldingRepository.eksisterendeIservBruker(AKTOR_ID);
        assertTrue(kanskjeUtmelding.isPresent());
        UtmeldingEntity utmelding = kanskjeUtmelding.get();
        assertEquals(AKTOR_ID.get(), utmelding.getAktor_Id());
        assertEquals(brukerV2.getIservFraDato(), utmelding.getIservSiden().toLocalDate());
    }

    @Test
    public void oppdaterUtmeldingsStatus_skalSletteBrukerSomIkkeLengerErIserv() {
        when(authService.getAktorIdOrThrow(FNR)).thenReturn(AKTOR_ID);

        var brukerV2 = kanskjeIservBruker(iservFraDato, Formidlingsgruppe.ARBS);

        utmeldingRepository.insertUtmeldingTabell(AKTOR_ID, iservFraDato);
        assertTrue(utmeldingRepository.eksisterendeIservBruker(AKTOR_ID).isPresent());

        iservService.oppdaterUtmeldingsStatus(brukerV2);
        assertTrue(utmeldingRepository.eksisterendeIservBruker(AKTOR_ID).isEmpty());
    }
  
    @Test
    public void oppdaterUtmeldingsStatus_skalIkkeStarteBrukerSomIkkeHarOppfolgingsstatus() {
        when(authService.getAktorIdOrThrow(FNR)).thenReturn(AKTOR_ID);

        var brukerV2 = kanskjeIservBruker(iservFraDato, Formidlingsgruppe.IARBS);

        when(oppfolgingService.erUnderOppfolging(AKTOR_ID)).thenReturn(false);

        iservService.oppdaterUtmeldingsStatus(brukerV2);
        verifyNoInteractions(oppfolgingService);
    }

    @Test
    public void finnBrukereMedIservI28Dager() {
        assertTrue(utmeldingRepository.finnBrukereMedIservI28Dager().isEmpty());

        insertIservBruker(AktorId.of("0"), iservFraDato.minusDays(30));
        insertIservBruker(AktorId.of("1"), iservFraDato.minusDays(27));
        insertIservBruker(AktorId.of("2"), iservFraDato.minusDays(15));
        insertIservBruker(AktorId.of("3"), iservFraDato);

        assertEquals(1, utmeldingRepository.finnBrukereMedIservI28Dager().size());
    }

    @Test
    public void avsluttOppfolging(){
        when(authService.getAktorIdOrThrow(FNR)).thenReturn(AKTOR_ID);

        insertIservBruker(AKTOR_ID, iservFraDato);

        assertTrue(utmeldingRepository.eksisterendeIservBruker(AKTOR_ID).isPresent());

        iservService.avslutteOppfolging(AKTOR_ID);

        verify(oppfolgingService).avsluttOppfolging(new UtmeldtEtter28Dager(AKTOR_ID));
        assertTrue(utmeldingRepository.eksisterendeIservBruker(AKTOR_ID).isEmpty());
    }

    @Test
    public void automatiskAvslutteOppfolging_skalAvslutteBrukerSomErIserv28dagerOgUnderOppfolging(){
        when(authService.getAktorIdOrThrow(FNR)).thenReturn(AKTOR_ID);

        insertIservBruker(AKTOR_ID, iservFraDato.minusDays(30));

        iservService.automatiskAvslutteOppfolging();

        assertTrue(utmeldingRepository.eksisterendeIservBruker(AKTOR_ID).isEmpty());
    }

    @Test
    public void automatiskAvslutteOppfolging_skalFjerneBrukerSomErIserv28dagerOgIkkeUnderOppfolging(){
        when(authService.getAktorIdOrThrow(FNR)).thenReturn(AKTOR_ID);

        insertIservBruker(AKTOR_ID, iservFraDato.minusDays(30));

        when(oppfolgingService.erUnderOppfolging(AKTOR_ID)).thenReturn(false);

        iservService.automatiskAvslutteOppfolging();

        verify(oppfolgingService, never()).avsluttOppfolging(any(Avregistrering.class));

        assertTrue(utmeldingRepository.eksisterendeIservBruker(AKTOR_ID).isEmpty());
    }
    
    @Test
    public void automatiskAvslutteOppfolging_skalIkkeFjerneBrukerSomErIserv28dagerMenIkkeAvsluttet(){
        when(authService.getAktorIdOrThrow(FNR)).thenReturn(AKTOR_ID);

        insertIservBruker(AKTOR_ID, iservFraDato.minusDays(30));

        when(oppfolgingService.avsluttOppfolging(any(UtmeldtEtter28Dager.class))).thenReturn(AvslutningStatusData.builder().underOppfolging(true).build());

        iservService.automatiskAvslutteOppfolging();

        assertTrue(utmeldingRepository.eksisterendeIservBruker(AKTOR_ID).isPresent());
    }
    
    private EndringPaaOppfoelgingsBrukerV2.EndringPaaOppfoelgingsBrukerV2Builder getArenaBrukerBuilder() {
        return EndringPaaOppfoelgingsBrukerV2.builder()
                .fodselsnummer(FNR.get())
                .formidlingsgruppe(Formidlingsgruppe.ISERV)
                .iservFraDato(iservFraDato.toLocalDate());
    }

    private KanskjeIservBruker kanskjeIservBruker() {
        return kanskjeIservBruker(iservFraDato, Formidlingsgruppe.ISERV);
    }

    private KanskjeIservBruker kanskjeIservBruker(ZonedDateTime iservFraDato, Formidlingsgruppe formidlingsgruppe) {
        return new KanskjeIservBruker(iservFraDato.toLocalDate(), FNR.get(), formidlingsgruppe);
    }

    private EndringPaaOppfoelgingsBrukerV2 insertIservBruker(AktorId aktorId, ZonedDateTime iservFraDato) {
        var brukerV2 = EndringPaaOppfoelgingsBrukerV2.builder()
                .fodselsnummer(Integer.toString(nesteFnr++))
                .formidlingsgruppe(Formidlingsgruppe.ISERV)
                .iservFraDato(iservFraDato.toLocalDate())
                .build();

        utmeldingRepository.insertUtmeldingTabell(aktorId, iservFraDato);

        return brukerV2;
    }

}
