package no.nav.fo.veilarboppfolging.config;

import no.nav.fo.veilarboppfolging.config.RemoteFeatureConfig.SjekkRegistrereBrukerArenaFeature;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ArenaBehandleArbeidssokerWSConfigTest {

    static AnnotationConfigApplicationContext context;

    @Mock
    private SjekkRegistrereBrukerArenaFeature skalRegistrereBrukerArenaFeature;

    @InjectMocks
    private ArenaBehandleArbeidssokerWSConfig arenaBehandleArbeidssokerWSConfig;

    @After
    public void tearDown() {
        if(context != null) {
            context.stop();
        }
    }

    @Test
    public void skalGiVellykketSelftestPingDersomArenaTjenestenErToggletAv() {
        when(skalRegistrereBrukerArenaFeature.erAktiv()).thenReturn(true);
        assertTrue(arenaBehandleArbeidssokerWSConfig.behandleArbeidssokerPing().ping().erVellykket());
    }
}