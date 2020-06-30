package no.nav.veilarboppfolging.services;

import no.nav.veilarboppfolging.domain.ArenaOppfolging;
import no.nav.veilarboppfolging.domain.Oppfolging;
import no.nav.veilarboppfolging.utils.mappers.VeilarbArenaOppfolging;
import org.junit.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class OppfolgingResolverTestVeilarbArena extends OppfolgingResolverTest {

    public OppfolgingResolverTestVeilarbArena() {
        super(false);
    }

    @Test
    public void setter_bruker_under_oppfolging_dersom_under_oppfolging_i_veilarbarena_men_ikke_i_folge_oppfolgingsflagg() {
        gittTilstand(false,
                Optional.of(new VeilarbArenaOppfolging()
                        .setFormidlingsgruppekode("ARBS")
                        .setKvalifiseringsgruppekode("BATT")),
                Optional.of(new ArenaOppfolging()
                        .setFormidlingsgruppe("ARBS")
                        .setServicegruppe("BATT")));

        oppfolgingResolver = OppfolgingResolver.lagOppfolgingResolver(FNR, oppfolgingResolverDependenciesMock);

        oppfolgingResolver.sjekkStatusIArenaOgOppdaterOppfolging();

        verify(oppfolgingRepositoryMock).startOppfolgingHvisIkkeAlleredeStartet(anyString());
    }

    @Test
    public void henter_ikke_direkte_fra_arena_dersom_bruker_er_under_oppfolging_i_folge_oppfolgingsflagg_og_veilarbarena() {

        gittTilstand(true,
                Optional.of(new VeilarbArenaOppfolging()
                        .setFormidlingsgruppekode("ARBS")
                        .setKvalifiseringsgruppekode("BATT")),
                Optional.empty());

        oppfolgingResolver = OppfolgingResolver.lagOppfolgingResolver(FNR, oppfolgingResolverDependenciesMock);

        oppfolgingResolver.sjekkStatusIArenaOgOppdaterOppfolging();

        verify(oppfolgingsbrukerServiceMock, times(1)).hentOppfolgingsbruker(any());
        verify(arenaOppfolgingServiceMock, times(0)).hentArenaOppfolging(any());
    }

    @Test
    public void henter_ikke_direkte_fra_arena_dersom_bruker_ikke_er_under_oppfolging_i_folge_oppfolgingsflagg_og_veilarbarena() {

        gittTilstand(false,
                Optional.of(new VeilarbArenaOppfolging()
                        .setFormidlingsgruppekode("IKKE")
                        .setKvalifiseringsgruppekode("OPPF")),
                Optional.empty());

        oppfolgingResolver = OppfolgingResolver.lagOppfolgingResolver(FNR, oppfolgingResolverDependenciesMock);

        oppfolgingResolver.sjekkStatusIArenaOgOppdaterOppfolging();

        verify(oppfolgingsbrukerServiceMock, times(1)).hentOppfolgingsbruker(any());
        verify(arenaOppfolgingServiceMock, times(0)).hentArenaOppfolging(any());
    }

    @Test
    public void henter_ikke_direkte_fra_arena_dersom_bruker_ikke_er_under_oppfolging_i_folge_oppfolgingsflagg_og_ikke_og_ikke_finnes_i_veilarbarena() {

        gittTilstand(false,
                Optional.empty(),
                Optional.empty());

        oppfolgingResolver = OppfolgingResolver.lagOppfolgingResolver(FNR, oppfolgingResolverDependenciesMock);

        oppfolgingResolver.sjekkStatusIArenaOgOppdaterOppfolging();

        verify(oppfolgingsbrukerServiceMock, times(1)).hentOppfolgingsbruker(any());
        verify(arenaOppfolgingServiceMock, times(0)).hentArenaOppfolging(any());
    }

    @Test
    public void henter_direkte_fra_arena_dersom_bruker_er_under_oppfolgingi_folge_oppfolgingsflagg_og_ikke_finnes_i_veilarbarena() {

        gittTilstand(true,
                Optional.empty(),
                Optional.of(new ArenaOppfolging()));

        oppfolgingResolver = OppfolgingResolver.lagOppfolgingResolver(FNR, oppfolgingResolverDependenciesMock);

        oppfolgingResolver.sjekkStatusIArenaOgOppdaterOppfolging();

        verify(oppfolgingsbrukerServiceMock, times(1)).hentOppfolgingsbruker(any());
        verify(arenaOppfolgingServiceMock, times(1)).hentArenaOppfolging(any());
    }

    @Test
    public void henter_direkte_fra_arena_dersom_bruker_er_under_oppfolgingi_folge_oppfolgingsflagg_men_ikke_i_folge_veilarbarena() {

        gittTilstand(true,
                Optional.of(new VeilarbArenaOppfolging().setFormidlingsgruppekode("ISERV").setKvalifiseringsgruppekode("IKKE_OPPF")),
                Optional.of(new ArenaOppfolging()));

        oppfolgingResolver = OppfolgingResolver.lagOppfolgingResolver(FNR, oppfolgingResolverDependenciesMock);

        oppfolgingResolver.sjekkStatusIArenaOgOppdaterOppfolging();

        verify(oppfolgingsbrukerServiceMock, times(1)).hentOppfolgingsbruker(any());
        verify(arenaOppfolgingServiceMock, times(1)).hentArenaOppfolging(any());
    }

    @Test
    public void henter_direkte_fra_arena_dersom_bruker_er_under_oppfolging_i_folge_veilarbarena_men_ikke_i_folge_oppfolgingsflagg() {

        gittTilstand(false,
                Optional.of(new VeilarbArenaOppfolging().setFormidlingsgruppekode("ARBS").setKvalifiseringsgruppekode("BATT")),
                Optional.of(new ArenaOppfolging()));

        oppfolgingResolver = OppfolgingResolver.lagOppfolgingResolver(FNR, oppfolgingResolverDependenciesMock);

        oppfolgingResolver.sjekkStatusIArenaOgOppdaterOppfolging();

        verify(oppfolgingsbrukerServiceMock, times(1)).hentOppfolgingsbruker(any());
        verify(arenaOppfolgingServiceMock, times(1)).hentArenaOppfolging(any());
    }

    private void gittTilstand(boolean oppfolgingsflagg,
                              Optional<VeilarbArenaOppfolging> veilarbarena,
                              Optional<ArenaOppfolging> arena) {
        when(oppfolgingRepositoryMock.hentOppfolging(any()))
                .thenReturn(Optional.of(new Oppfolging().setUnderOppfolging(oppfolgingsflagg)));
        when(oppfolgingsbrukerServiceMock.hentOppfolgingsbruker(any()))
                .thenReturn(veilarbarena);
        setupArenaService();
        when(arenaOppfolgingServiceMock.hentArenaOppfolging(any())).thenReturn(arena.orElse(null));
    }
}
