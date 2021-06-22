package no.nav.veilarboppfolging.service;

import no.nav.common.types.identer.AktorId;
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV1;
import no.nav.veilarboppfolging.client.veilarbarena.ArenaOppfolgingTilstand;
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository;
import no.nav.veilarboppfolging.repository.entity.OppfolgingEntity;
import org.junit.jupiter.api.Test;

import java.util.Optional;

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

    private final UnleashService unleashService = mock(UnleashService.class);

    private final OppfolgingEndringService oppfolgingEndringService = new OppfolgingEndringService(
            authService, oppfolgingService, arenaOppfolgingService,
            kvpService, metricsService, oppfolgingsStatusRepository, unleashService
    );

    @Test
    public void oppdaterOppfolgingMedStatusFraArena__skal_ikke_oppdatere_hvis_bruker_ikke_under_oppfolging_i_veilarboppfolging_eller_arena() {
        when(authService.getAktorIdOrThrow(TEST_FNR)).thenReturn(TEST_AKTOR_ID);
        when(oppfolgingsStatusRepository.fetch(TEST_AKTOR_ID)).thenReturn(new OppfolgingEntity().setUnderOppfolging(false));

        EndringPaaOppfoelgingsBrukerV1 endringPaaOppfoelgingsBrukerV1 = new EndringPaaOppfoelgingsBrukerV1()
                .setFodselsnr(TEST_FNR.get())
                .setFormidlingsgruppekode("ISERV")
                .setKvalifiseringsgruppekode("VURDI");

        oppfolgingEndringService.oppdaterOppfolgingMedStatusFraArena(endringPaaOppfoelgingsBrukerV1);

        verify(oppfolgingService, never()).startOppfolgingHvisIkkeAlleredeStartet(any(AktorId.class));
        verify(oppfolgingService, never()).avsluttOppfolgingForBruker(any(), any(), any());
    }

    @Test
    public void oppdaterOppfolgingMedStatusFraArena__skal_starte_oppfolging_pa_bruker_som_ikke_er_under_oppfolging_i_veilarboppfolging_men_under_oppfolging_i_arena() {
        when(authService.getAktorIdOrThrow(TEST_FNR)).thenReturn(TEST_AKTOR_ID);
        when(oppfolgingsStatusRepository.fetch(TEST_AKTOR_ID)).thenReturn(new OppfolgingEntity().setUnderOppfolging(false));
        when(unleashService.skalOppdaterOppfolgingMedKafka()).thenReturn(true);

        EndringPaaOppfoelgingsBrukerV1 endringPaaOppfoelgingsBrukerV1 = new EndringPaaOppfoelgingsBrukerV1()
                .setFodselsnr(TEST_FNR.get())
                .setFormidlingsgruppekode("ARBS")
                .setKvalifiseringsgruppekode("VURDI");

        oppfolgingEndringService.oppdaterOppfolgingMedStatusFraArena(endringPaaOppfoelgingsBrukerV1);

        verify(oppfolgingService, times(1)).startOppfolgingHvisIkkeAlleredeStartet(TEST_AKTOR_ID);
        verify(oppfolgingService, never()).avsluttOppfolgingForBruker(any(), any(), any());
    }

    @Test
    public void oppdaterOppfolgingMedStatusFraArena__skal_ikke_starte_oppfolging_pa_bruker_hvis_toggel_er_av() {
        when(authService.getAktorIdOrThrow(TEST_FNR)).thenReturn(TEST_AKTOR_ID);
        when(oppfolgingsStatusRepository.fetch(TEST_AKTOR_ID)).thenReturn(new OppfolgingEntity().setUnderOppfolging(false));
        when(unleashService.skalOppdaterOppfolgingMedKafka()).thenReturn(false);

        EndringPaaOppfoelgingsBrukerV1 endringPaaOppfoelgingsBrukerV1 = new EndringPaaOppfoelgingsBrukerV1()
                .setFodselsnr(TEST_FNR.get())
                .setFormidlingsgruppekode("ARBS")
                .setKvalifiseringsgruppekode("VURDI");

        oppfolgingEndringService.oppdaterOppfolgingMedStatusFraArena(endringPaaOppfoelgingsBrukerV1);

        verify(oppfolgingService, never()).startOppfolgingHvisIkkeAlleredeStartet(any(AktorId.class));
        verify(oppfolgingService, never()).avsluttOppfolgingForBruker(any(), any(), any());
    }

    @Test
    public void oppdaterOppfolgingMedStatusFraArena__skal_avslutte_oppfolging_pa_bruker_som_er_under_oppfolging_i_veilarboppfolging_men_ikke_under_oppfolging_i_arena() {
        var arenaTilstand = new ArenaOppfolgingTilstand();
        arenaTilstand.setKanEnkeltReaktiveres(false);

        when(authService.getAktorIdOrThrow(TEST_FNR)).thenReturn(TEST_AKTOR_ID);
        when(oppfolgingsStatusRepository.fetch(TEST_AKTOR_ID)).thenReturn(new OppfolgingEntity().setUnderOppfolging(true));
        when(arenaOppfolgingService.hentOppfolgingTilstandDirekteFraArena(TEST_FNR)).thenReturn(Optional.of(arenaTilstand));
        when(kvpService.erUnderKvp(TEST_AKTOR_ID)).thenReturn(false);
        when(unleashService.skalOppdaterOppfolgingMedKafka()).thenReturn(true);


        EndringPaaOppfoelgingsBrukerV1 endringPaaOppfoelgingsBrukerV1 = new EndringPaaOppfoelgingsBrukerV1()
                .setFodselsnr(TEST_FNR.get())
                .setFormidlingsgruppekode("ISERV")
                .setKvalifiseringsgruppekode("VURDI");

        oppfolgingEndringService.oppdaterOppfolgingMedStatusFraArena(endringPaaOppfoelgingsBrukerV1);


        verify(oppfolgingService, never()).startOppfolgingHvisIkkeAlleredeStartet(any(AktorId.class));
        verify(oppfolgingService, times(1))
                .avsluttOppfolgingForBruker(
                    TEST_AKTOR_ID, null, "Oppfølging avsluttet automatisk pga. inaktiv bruker som ikke kan reaktiveres"
                );

        verify(metricsService, times(1)).rapporterAutomatiskAvslutningAvOppfolging(true);
    }

    @Test
    public void oppdaterOppfolgingMedStatusFraArena__skal_ikke_avslutte_oppfolging_pa_bruker_hvis_toggel_er_av() {
        var arenaTilstand = new ArenaOppfolgingTilstand();
        arenaTilstand.setKanEnkeltReaktiveres(false);

        when(authService.getAktorIdOrThrow(TEST_FNR)).thenReturn(TEST_AKTOR_ID);
        when(oppfolgingsStatusRepository.fetch(TEST_AKTOR_ID)).thenReturn(new OppfolgingEntity().setUnderOppfolging(true));
        when(arenaOppfolgingService.hentOppfolgingTilstandDirekteFraArena(TEST_FNR)).thenReturn(Optional.of(arenaTilstand));
        when(kvpService.erUnderKvp(TEST_AKTOR_ID)).thenReturn(false);
        when(unleashService.skalOppdaterOppfolgingMedKafka()).thenReturn(false);


        EndringPaaOppfoelgingsBrukerV1 endringPaaOppfoelgingsBrukerV1 = new EndringPaaOppfoelgingsBrukerV1()
                .setFodselsnr(TEST_FNR.get())
                .setFormidlingsgruppekode("ISERV")
                .setKvalifiseringsgruppekode("VURDI");

        oppfolgingEndringService.oppdaterOppfolgingMedStatusFraArena(endringPaaOppfoelgingsBrukerV1);


        verify(oppfolgingService, never()).startOppfolgingHvisIkkeAlleredeStartet(any(AktorId.class));
        verify(oppfolgingService, never()).avsluttOppfolgingForBruker(any(), any(), any());
        verify(metricsService, never()).rapporterAutomatiskAvslutningAvOppfolging(true);
    }

    @Test
    public void oppdaterOppfolgingMedStatusFraArena__skal_ikke_avslutte_oppfolging_pa_bruker_som_kan_enkelt_reaktiveres() {
        var arenaTilstand = new ArenaOppfolgingTilstand();
        arenaTilstand.setKanEnkeltReaktiveres(true);

        when(authService.getAktorIdOrThrow(TEST_FNR)).thenReturn(TEST_AKTOR_ID);
        when(oppfolgingsStatusRepository.fetch(TEST_AKTOR_ID)).thenReturn(new OppfolgingEntity().setUnderOppfolging(true));
        when(arenaOppfolgingService.hentOppfolgingTilstandDirekteFraArena(TEST_FNR)).thenReturn(Optional.of(arenaTilstand));
        when(kvpService.erUnderKvp(TEST_AKTOR_ID)).thenReturn(false);

        EndringPaaOppfoelgingsBrukerV1 endringPaaOppfoelgingsBrukerV1 = new EndringPaaOppfoelgingsBrukerV1()
                .setFodselsnr(TEST_FNR.get())
                .setFormidlingsgruppekode("ISERV")
                .setKvalifiseringsgruppekode("VURDI");

        oppfolgingEndringService.oppdaterOppfolgingMedStatusFraArena(endringPaaOppfoelgingsBrukerV1);


        verify(oppfolgingService, never()).startOppfolgingHvisIkkeAlleredeStartet(any(AktorId.class));
        verify(oppfolgingService, never()).avsluttOppfolgingForBruker(any(), any(), any());
    }

    @Test
    public void oppdaterOppfolgingMedStatusFraArena__skal_ikke_avslutte_oppfolging_pa_bruker_som_er_under_kvp() {
        var arenaTilstand = new ArenaOppfolgingTilstand();
        arenaTilstand.setKanEnkeltReaktiveres(false);

        when(authService.getAktorIdOrThrow(TEST_FNR)).thenReturn(TEST_AKTOR_ID);
        when(oppfolgingsStatusRepository.fetch(TEST_AKTOR_ID)).thenReturn(new OppfolgingEntity().setUnderOppfolging(true));
        when(arenaOppfolgingService.hentOppfolgingTilstandDirekteFraArena(TEST_FNR)).thenReturn(Optional.of(arenaTilstand));
        when(kvpService.erUnderKvp(TEST_AKTOR_ID)).thenReturn(true);

        EndringPaaOppfoelgingsBrukerV1 endringPaaOppfoelgingsBrukerV1 = new EndringPaaOppfoelgingsBrukerV1()
                .setFodselsnr(TEST_FNR.get())
                .setFormidlingsgruppekode("ISERV")
                .setKvalifiseringsgruppekode("VURDI");

        oppfolgingEndringService.oppdaterOppfolgingMedStatusFraArena(endringPaaOppfoelgingsBrukerV1);


        verify(oppfolgingService, never()).startOppfolgingHvisIkkeAlleredeStartet(any(AktorId.class));
        verify(oppfolgingService, never()).avsluttOppfolgingForBruker(any(), any(), any());
    }

}
