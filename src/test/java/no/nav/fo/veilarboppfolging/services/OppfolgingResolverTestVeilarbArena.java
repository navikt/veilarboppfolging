package no.nav.fo.veilarboppfolging.services;

import no.nav.fo.veilarboppfolging.domain.ArenaOppfolging;
import no.nav.fo.veilarboppfolging.domain.Oppfolging;
import no.nav.fo.veilarboppfolging.mappers.VeilarbArenaOppfolging;
import org.junit.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class OppfolgingResolverTestVeilarbArena extends OppfolgingResolverTest {

    public OppfolgingResolverTestVeilarbArena() {
        super(false);
    }

    @Test
    public void henter_bruker_direkte_fra_arena_dersom_bruker_ikke_er_under_oppfolging() {

        when(oppfolgingRepositoryMock.hentOppfolging(any())).thenReturn(Optional.of(new Oppfolging().setUnderOppfolging(false)));
        when(oppfolgingsbrukerServiceMock.hentOppfolgingsbruker(any())).thenReturn(Optional.of(new VeilarbArenaOppfolging()));
        setupArenaService();
        when(arenaOppfolgingServiceMock.hentArenaOppfolging(any())).thenReturn(new ArenaOppfolging());


        oppfolgingResolver = new OppfolgingResolver(FNR, oppfolgingResolverDependenciesMock);

        oppfolgingResolver.sjekkStatusIArenaOgOppdaterOppfolging();

        verify(oppfolgingsbrukerServiceMock, times(1)).hentOppfolgingsbruker(any());
        verify(arenaOppfolgingServiceMock, times(1)).hentArenaOppfolging(any());

    }

    @Test
    public void henter_ikke_bruker_direkte_fra_arena_dersom_bruker_er_under_oppfolging() {

        when(oppfolgingRepositoryMock.hentOppfolging(any())).thenReturn(Optional.of(new Oppfolging().setUnderOppfolging(true)));
        when(oppfolgingsbrukerServiceMock.hentOppfolgingsbruker(any())).thenReturn(Optional.of(new VeilarbArenaOppfolging()));
        setupArenaService();

        oppfolgingResolver = new OppfolgingResolver(FNR, oppfolgingResolverDependenciesMock);

        oppfolgingResolver.sjekkStatusIArenaOgOppdaterOppfolging();

        verify(oppfolgingsbrukerServiceMock, times(1)).hentOppfolgingsbruker(any());
        verify(arenaOppfolgingServiceMock, times(0)).hentArenaOppfolging(any());

    }

    @Test
    public void henter_bruker_direkte_fra_arena_dersom_bruker_er_under_oppfolging_og_iserv__lik_data() {

        when(oppfolgingRepositoryMock.hentOppfolging(any())).thenReturn(Optional.of(new Oppfolging().setUnderOppfolging(true)));
        when(oppfolgingsbrukerServiceMock.hentOppfolgingsbruker(any())).thenReturn(Optional.of(new VeilarbArenaOppfolging().setFormidlingsgruppekode("ISERV")));
        setupArenaService();
        when(arenaOppfolgingServiceMock.hentArenaOppfolging(any())).thenReturn(new ArenaOppfolging().setFormidlingsgruppe("ISERV").setKanEnkeltReaktiveres(true));

        oppfolgingResolver = new OppfolgingResolver(FNR, oppfolgingResolverDependenciesMock);

        oppfolgingResolver.sjekkStatusIArenaOgOppdaterOppfolging();

        verify(oppfolgingsbrukerServiceMock, times(1)).hentOppfolgingsbruker(any());
        verify(arenaOppfolgingServiceMock, times(1)).hentArenaOppfolging(any());


        assertThat(oppfolgingResolver.getErSykmeldtMedArbeidsgiver()).isFalse();
        assertThat(oppfolgingResolver.getInaktivIArena()).isTrue();
        assertThat(oppfolgingResolver.getKanReaktiveres()).isTrue();

    }

    @Test
    public void henter_bruker_direkte_fra_arena_dersom_bruker_er_under_oppfolging_og_iserv__ulik_data() {

        when(oppfolgingRepositoryMock.hentOppfolging(any())).thenReturn(Optional.of(new Oppfolging().setUnderOppfolging(true)));
        when(oppfolgingsbrukerServiceMock.hentOppfolgingsbruker(any())).thenReturn(Optional.of(new VeilarbArenaOppfolging().setFormidlingsgruppekode("ISERV")));
        setupArenaService();
        when(arenaOppfolgingServiceMock.hentArenaOppfolging(any())).thenReturn(new ArenaOppfolging().setFormidlingsgruppe("IARBS").setServicegruppe("IKKE_OPPF").setKanEnkeltReaktiveres(true));

        oppfolgingResolver = new OppfolgingResolver(FNR, oppfolgingResolverDependenciesMock);

        oppfolgingResolver.sjekkStatusIArenaOgOppdaterOppfolging();

        verify(oppfolgingsbrukerServiceMock, times(1)).hentOppfolgingsbruker(any());
        verify(arenaOppfolgingServiceMock, times(1)).hentArenaOppfolging(any());

        assertThat(oppfolgingResolver.getErSykmeldtMedArbeidsgiver()).isTrue();
        assertThat(oppfolgingResolver.getInaktivIArena()).isFalse();
        assertThat(oppfolgingResolver.getKanReaktiveres()).isTrue();
    }
}
