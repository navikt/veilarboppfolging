package no.nav.veilarboppfolging.service;

import no.nav.pto_schema.enums.arena.Formidlingsgruppe;
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe;
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2;
import no.nav.veilarboppfolging.client.veilarbarena.ArenaOppfolgingTilstand;
import no.nav.veilarboppfolging.oppfolgingsbruker.Oppfolgingsbruker;
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingService;
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository;
import no.nav.veilarboppfolging.repository.entity.OppfolgingEntity;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static no.nav.veilarboppfolging.config.ApplicationConfig.SYSTEM_USER_NAME;
import static no.nav.veilarboppfolging.test.TestData.TEST_AKTOR_ID;
import static no.nav.veilarboppfolging.test.TestData.TEST_FNR;
import static org.mockito.Mockito.*;

public class OppfolgingEndringServiceTest {

    private final AuthService authService = mock(AuthService.class);

    private final OppfolgingService oppfolgingService = mock(OppfolgingService.class);

    private final ArenaOppfolgingService arenaOppfolgingService = mock(ArenaOppfolgingService.class);

    private final KvpService kvpService = mock(KvpService.class);

    private final MetricsService metricsService = mock(MetricsService.class);

    private final OppfolgingsStatusRepository oppfolgingsStatusRepository = mock(OppfolgingsStatusRepository.class);

    private final OppfolgingEndringService oppfolgingEndringService = new OppfolgingEndringService(
            authService, oppfolgingService, arenaOppfolgingService,
            kvpService, metricsService, oppfolgingsStatusRepository
    );

    @Test
    public void oppdaterOppfolgingMedStatusFraArena__skal_ikke_oppdatere_hvis_bruker_ikke_under_oppfolging_i_veilarboppfolging_eller_arena() {
        when(authService.getAktorIdOrThrow(TEST_FNR)).thenReturn(TEST_AKTOR_ID);
        when(oppfolgingsStatusRepository.hentOppfolging(TEST_AKTOR_ID)).thenReturn(Optional.of(new OppfolgingEntity().setUnderOppfolging(false)));

        EndringPaaOppfoelgingsBrukerV2 brukverV2 = EndringPaaOppfoelgingsBrukerV2.builder()
                .fodselsnummer(TEST_FNR.get())
                .formidlingsgruppe(Formidlingsgruppe.ISERV)
                .kvalifiseringsgruppe(Kvalifiseringsgruppe.VURDI)
                .build();

        oppfolgingEndringService.oppdaterOppfolgingMedStatusFraArena(brukverV2);

        verify(oppfolgingService, never()).startOppfolgingHvisIkkeAlleredeStartet(any(Oppfolgingsbruker.class));
        verify(oppfolgingService, never()).avsluttOppfolging(any(), any(), any());
    }

    @Test
    public void oppdaterOppfolgingMedStatusFraArena__skal_starte_oppfolging_pa_bruker_som_ikke_er_under_oppfolging_i_veilarboppfolging_men_under_oppfolging_i_arena() {
        when(authService.getAktorIdOrThrow(TEST_FNR)).thenReturn(TEST_AKTOR_ID);
        when(oppfolgingsStatusRepository.hentOppfolging(TEST_AKTOR_ID)).thenReturn(Optional.of(new OppfolgingEntity().setUnderOppfolging(false)));

        EndringPaaOppfoelgingsBrukerV2 brukverV2 = EndringPaaOppfoelgingsBrukerV2.builder()
                .fodselsnummer(TEST_FNR.get())
                .formidlingsgruppe(Formidlingsgruppe.ARBS)
                .kvalifiseringsgruppe(Kvalifiseringsgruppe.VURDI)
                .build();

        oppfolgingEndringService.oppdaterOppfolgingMedStatusFraArena(brukverV2);

        verify(oppfolgingService, times(1)).startOppfolgingHvisIkkeAlleredeStartet(Oppfolgingsbruker.arenaSyncOppfolgingBruker(TEST_AKTOR_ID, Formidlingsgruppe.ARBS, brukverV2.getKvalifiseringsgruppe()));
        verify(oppfolgingService, never()).avsluttOppfolging(any(), any(), any());
    }

    @Test
    public void oppdaterOppfolgingMedStatusFraArena__skal_avslutte_oppfolging_pa_bruker_som_er_under_oppfolging_i_veilarboppfolging_men_ikke_under_oppfolging_i_arena() {
        when(authService.getAktorIdOrThrow(TEST_FNR)).thenReturn(TEST_AKTOR_ID);
        when(oppfolgingsStatusRepository.hentOppfolging(TEST_AKTOR_ID)).thenReturn(Optional.of(new OppfolgingEntity().setUnderOppfolging(true)));
        when(arenaOppfolgingService.kanEnkeltReaktiveres(TEST_FNR)).thenReturn(Optional.of(false));
        when(kvpService.erUnderKvp(TEST_AKTOR_ID)).thenReturn(false);

        EndringPaaOppfoelgingsBrukerV2 brukverV2 = EndringPaaOppfoelgingsBrukerV2.builder()
                .fodselsnummer(TEST_FNR.get())
                .formidlingsgruppe(Formidlingsgruppe.ISERV)
                .kvalifiseringsgruppe(Kvalifiseringsgruppe.VURDI)
                .build();

        oppfolgingEndringService.oppdaterOppfolgingMedStatusFraArena(brukverV2);


        verify(oppfolgingService, never()).startOppfolgingHvisIkkeAlleredeStartet(any(Oppfolgingsbruker.class));
        verify(oppfolgingService, times(1))
                .avsluttOppfolging(
                    TEST_FNR, SYSTEM_USER_NAME, "Oppfølging avsluttet automatisk pga. inaktiv bruker som ikke kan reaktiveres"
                );

        verify(metricsService, times(1)).rapporterAutomatiskAvslutningAvOppfolging(true);
    }

    @Test
    public void oppdaterOppfolgingMedStatusFraArena__skal_ikke_avslutte_oppfolging_pa_bruker_som_kan_enkelt_reaktiveres() {
        when(authService.getAktorIdOrThrow(TEST_FNR)).thenReturn(TEST_AKTOR_ID);
        when(oppfolgingsStatusRepository.hentOppfolging(TEST_AKTOR_ID)).thenReturn(Optional.of(new OppfolgingEntity().setUnderOppfolging(true)));
        when(arenaOppfolgingService.kanEnkeltReaktiveres(TEST_FNR)).thenReturn(Optional.of(true));
        when(kvpService.erUnderKvp(TEST_AKTOR_ID)).thenReturn(false);
        when(oppfolgingService.harAktiveTiltaksdeltakelser(TEST_FNR)).thenReturn(false);

        EndringPaaOppfoelgingsBrukerV2 brukverV2 = EndringPaaOppfoelgingsBrukerV2.builder()
                .fodselsnummer(TEST_FNR.get())
                .formidlingsgruppe(Formidlingsgruppe.ISERV)
                .kvalifiseringsgruppe(Kvalifiseringsgruppe.VURDI)
                .build();

        oppfolgingEndringService.oppdaterOppfolgingMedStatusFraArena(brukverV2);

        verify(oppfolgingService, never()).startOppfolgingHvisIkkeAlleredeStartet(any(Oppfolgingsbruker.class));
        verify(oppfolgingService, never()).avsluttOppfolging(any(), any(), any());
    }

    @Test
    public void oppdaterOppfolgingMedStatusFraArena__skal_ikke_avslutte_oppfolging_pa_bruker_som_er_under_kvp() {
        when(authService.getAktorIdOrThrow(TEST_FNR)).thenReturn(TEST_AKTOR_ID);
        when(oppfolgingsStatusRepository.hentOppfolging(TEST_AKTOR_ID)).thenReturn(Optional.of(new OppfolgingEntity().setUnderOppfolging(true)));
        when(arenaOppfolgingService.kanEnkeltReaktiveres(TEST_FNR)).thenReturn(Optional.of(false));
        when(kvpService.erUnderKvp(TEST_AKTOR_ID)).thenReturn(true);
        when(oppfolgingService.harAktiveTiltaksdeltakelser(TEST_FNR)).thenReturn(false);

        EndringPaaOppfoelgingsBrukerV2 brukverV2 = EndringPaaOppfoelgingsBrukerV2.builder()
                .fodselsnummer(TEST_FNR.get())
                .formidlingsgruppe(Formidlingsgruppe.ISERV)
                .kvalifiseringsgruppe(Kvalifiseringsgruppe.VURDI)
                .build();

        oppfolgingEndringService.oppdaterOppfolgingMedStatusFraArena(brukverV2);


        verify(oppfolgingService, never()).startOppfolgingHvisIkkeAlleredeStartet(any(Oppfolgingsbruker.class));
        verify(oppfolgingService, never()).avsluttOppfolging(any(), any(), any());
    }

    @Test
    public void oppdaterOppfolgingMedStatusFraArena__skal_ikke_avslutte_oppfolging_pa_bruker_som_har_aktive_tiltaksdeltakelser() {
        when(authService.getAktorIdOrThrow(TEST_FNR)).thenReturn(TEST_AKTOR_ID);
        when(oppfolgingsStatusRepository.hentOppfolging(TEST_AKTOR_ID)).thenReturn(Optional.of(new OppfolgingEntity().setUnderOppfolging(true)));
        when(arenaOppfolgingService.kanEnkeltReaktiveres(TEST_FNR)).thenReturn(Optional.of(false));
        when(kvpService.erUnderKvp(TEST_AKTOR_ID)).thenReturn(false);
        when(oppfolgingService.harAktiveTiltaksdeltakelser(TEST_FNR)).thenReturn(true);

        EndringPaaOppfoelgingsBrukerV2 brukverV2 = EndringPaaOppfoelgingsBrukerV2.builder()
                .fodselsnummer(TEST_FNR.get())
                .formidlingsgruppe(Formidlingsgruppe.ISERV)
                .kvalifiseringsgruppe(Kvalifiseringsgruppe.VURDI)
                .build();

        oppfolgingEndringService.oppdaterOppfolgingMedStatusFraArena(brukverV2);


        verify(oppfolgingService, never()).startOppfolgingHvisIkkeAlleredeStartet(any(Oppfolgingsbruker.class));
        verify(oppfolgingService, never()).avsluttOppfolging(any(), any(), any());
    }
}
