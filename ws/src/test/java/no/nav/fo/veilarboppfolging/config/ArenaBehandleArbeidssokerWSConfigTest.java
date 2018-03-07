package no.nav.fo.veilarboppfolging.config;

import no.nav.fo.veilarboppfolging.config.RemoteFeatureConfig.OpprettBrukerIArenaFeature;
import org.junit.BeforeClass;
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

    @BeforeClass
    public static void setUp() throws Exception {
        System.setProperty("behandlearbeidssoker.endpoint.url", "behandlearbeidssoker");
    }

    @Mock
    private OpprettBrukerIArenaFeature opprettBrukerIArenaFeature;

    @InjectMocks
    private ArenaBehandleArbeidssokerWSConfig arenaBehandleArbeidssokerWSConfig;

    @Test
    public void skalGiVellykketSelftestPingDersomArenaTjenestenErToggletAv() {
        when(opprettBrukerIArenaFeature.erAktiv()).thenReturn(false);
        assertTrue(arenaBehandleArbeidssokerWSConfig.behandleArbeidssokerPing().ping().erVellykket());
    }
}