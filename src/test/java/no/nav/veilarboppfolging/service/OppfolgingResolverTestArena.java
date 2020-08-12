package no.nav.veilarboppfolging.service;

import org.junit.Ignore;
import org.junit.Test;

// TODO: Lag nye tester som en del av refaktoreringen av OppfolgingResolver
@Ignore
public class OppfolgingResolverTestArena extends OppfolgingResolverTest {

    @Test
    public void sjekkStatusIArenaOgOppdaterOppfolging__skal_fungere_selv_om_arena_feiler() {

//        when(arenaOppfolgingServiceMock.hentArenaOppfolging(anyString())).thenThrow(new RuntimeException("Feil i Arena"));
//
//        oppfolgingResolver.sjekkStatusIArenaOgOppdaterOppfolging();
//
//        verify(arenaOppfolgingServiceMock).hentArenaOppfolging(anyString());
//        verify(oppfolgingsbrukerServiceMock, times(0)).hentOppfolgingsbruker(anyString());
    }
}
