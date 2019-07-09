package no.nav.fo.veilarboppfolging.services;

import org.junit.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class OppfolgingResolverTestArena extends OppfolgingResolverTest {

    public OppfolgingResolverTestArena() {
        super(true);
    }

    @Test
    public void sjekkStatusIArenaOgOppdaterOppfolging__skal_fungere_selv_om_arena_feiler() {

        when(arenaOppfolgingServiceMock.hentArenaOppfolging(anyString())).thenThrow(new RuntimeException("Feil i Arena"));

        oppfolgingResolver.sjekkStatusIArenaOgOppdaterOppfolging();

        verify(arenaOppfolgingServiceMock).hentArenaOppfolging(anyString());
        verify(oppfolgingsbrukerServiceMock, times(0)).hentOppfolgingsbruker(anyString());
    }
}
