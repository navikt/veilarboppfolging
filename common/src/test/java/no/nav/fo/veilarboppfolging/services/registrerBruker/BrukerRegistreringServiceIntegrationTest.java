package no.nav.fo.veilarboppfolging.services.registrerBruker;

import io.vavr.control.Try;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.config.DatabaseConfig;
import no.nav.fo.veilarboppfolging.config.RemoteFeatureConfig;
import no.nav.fo.veilarboppfolging.db.ArbeidssokerregistreringRepository;
import no.nav.fo.veilarboppfolging.db.NyeBrukereFeedRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingsPeriodeRepository;
import no.nav.fo.veilarboppfolging.domain.BrukerRegistrering;
import no.nav.fo.veilarboppfolging.domain.Oppfolging;
import no.nav.fo.veilarboppfolging.domain.StartRegistreringStatus;
import no.nav.sbl.jdbc.Database;
import no.nav.tjeneste.virksomhet.behandlearbeidssoeker.v1.binding.BehandleArbeidssoekerV1;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;

import java.util.Optional;

import static no.nav.fo.veilarboppfolging.config.DatabaseConfig.DATA_SOURCE_JDNI_NAME;
import static no.nav.fo.veilarboppfolging.config.JndiLocalContextConfig.setupInMemoryDatabase;
import static no.nav.fo.veilarboppfolging.services.registrerBruker.BrukerRegistreringServiceTest.getBrukerRegistreringSelvgaaende;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BrukerRegistreringServiceIntegrationTest {

    private static AnnotationConfigApplicationContext context;

    private static BrukerRegistreringService brukerRegistreringService;
    private static OppfolgingRepository oppfolgingRepository;
    private static OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository;
    private static AktorService aktorService;
    private static BehandleArbeidssoekerV1 behandleArbeidssoekerV1;
    private static RemoteFeatureConfig.OpprettBrukerIArenaFeature opprettBrukerIArenaFeature;
    private static RemoteFeatureConfig.RegistreringFeature registreringFeature;
    private static StartRegistreringStatusResolver startRegistreringStatusResolver;

    private static String ident = "***REMOVED***";
    private static final BrukerRegistrering SELVGAENDE_BRUKER = getBrukerRegistreringSelvgaaende();

    @BeforeEach
    public void setup() throws Exception {

        SimpleNamingContextBuilder builder = new SimpleNamingContextBuilder();
        builder.bind(DATA_SOURCE_JDNI_NAME, setupInMemoryDatabase());
        builder.activate();

        context = new AnnotationConfigApplicationContext(
                DatabaseConfig.class,
                BrukerregistreringConfigTest.class
        );

        context.start();

        brukerRegistreringService = context.getBean(BrukerRegistreringService.class);
        oppfolgingRepository = context.getBean(OppfolgingRepository.class);
        oppfolgingsPeriodeRepository = context.getBean(OppfolgingsPeriodeRepository.class);
        aktorService = context.getBean(AktorService.class);
        behandleArbeidssoekerV1 = context.getBean(BehandleArbeidssoekerV1.class);
        opprettBrukerIArenaFeature = context.getBean(RemoteFeatureConfig.OpprettBrukerIArenaFeature.class);
        registreringFeature = context.getBean(RemoteFeatureConfig.RegistreringFeature.class);
        startRegistreringStatusResolver = context.getBean(StartRegistreringStatusResolver.class);
    }

    @AfterEach
    public void tearDown() {
        context.stop();
    }

    @Test
    public void skalRulleTilbakeDatabaseDersomKallTilArenaFeiler() throws Exception {
        cofigureMocks();
        doThrow(new RuntimeException()).when(behandleArbeidssoekerV1).aktiverBruker(any());

        Try<Void> run = Try.run(() -> brukerRegistreringService.registrerBruker(SELVGAENDE_BRUKER, ident));
        assertThat(run.isFailure()).isTrue();

        Optional<Oppfolging> oppfolging = oppfolgingRepository.hentOppfolging(ident);

        assertThat(oppfolging.isPresent()).isFalse();
    }

    @Test
    public void skalLagreIDatabaseDersomKallTilArenaErOK() throws Exception {
        cofigureMocks();

        brukerRegistreringService.registrerBruker(SELVGAENDE_BRUKER, ident);

        Optional<Oppfolging> oppfolging = oppfolgingRepository.hentOppfolging(ident);

        assertThat(oppfolging.isPresent()).isTrue();
    }

    @Test
    public void skalHaandtereAtOppfolgingstatusAlleredeFinnes() {
        cofigureMocks();
        String ident = "33333333333333";
        oppfolgingRepository.opprettOppfolging(ident);
        oppfolgingsPeriodeRepository.avslutt(ident, "veilederid", "begrunnelse" );

        brukerRegistreringService.registrerBruker(SELVGAENDE_BRUKER, ident);

        Optional<Oppfolging> oppfolging = oppfolgingRepository.hentOppfolging(ident);

        assertThat(oppfolging.get().isUnderOppfolging()).isTrue();
    }

    private void cofigureMocks() {
        when(registreringFeature.erAktiv()).thenReturn(true);
        when(opprettBrukerIArenaFeature.erAktiv()).thenReturn(true);
        when(aktorService.getAktorId(any())).thenAnswer((invocation -> Optional.of(invocation.getArgument(0))));

        when(startRegistreringStatusResolver.hentStartRegistreringStatus(any())).thenReturn(
                new StartRegistreringStatus()
                        .setOppfyllerKravForAutomatiskRegistrering(true)
                        .setUnderOppfolging(false)
        );

    }


    @Configuration
    @ComponentScan
    public static class BrukerregistreringConfigTest {

        @Bean
        public ArbeidssokerregistreringRepository arbeidssokerregistreringRepository(JdbcTemplate db) {
            return new ArbeidssokerregistreringRepository(db);
        }

        @Bean
        public OppfolgingRepository oppfolgingRepository(Database database) {
            return new OppfolgingRepository(database);
        }

        @Bean
        public OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository(Database database) {
            return new OppfolgingsPeriodeRepository(database);
        }

        @Bean
        public AktorService aktoerService() {
            return mock(AktorService.class);
        }


        @Bean
        public BehandleArbeidssoekerV1 behandleArbeidssoekerV1() {
            return mock(BehandleArbeidssoekerV1.class);
        }

        @Bean
        public RemoteFeatureConfig.OpprettBrukerIArenaFeature opprettBrukerIArenaFeature() {
            return mock(RemoteFeatureConfig.OpprettBrukerIArenaFeature.class);
        }

        @Bean
        public RemoteFeatureConfig.RegistreringFeature registreringFeature() {
            return mock(RemoteFeatureConfig.RegistreringFeature.class);
        }

        @Bean
        public NyeBrukereFeedRepository nyeBrukereFeedRepository(Database database) {
            return new NyeBrukereFeedRepository(database);
        }

        @Bean
        public StartRegistreringStatusResolver startRegistreringStatusResolver() {
            return mock(StartRegistreringStatusResolver.class);
        }

        @Bean
        BrukerRegistreringService registrerBrukerService(
                ArbeidssokerregistreringRepository arbeidssokerregistreringRepository,
                AktorService aktorService,
                BehandleArbeidssoekerV1 behandleArbeidssoekerV1,
                RemoteFeatureConfig.OpprettBrukerIArenaFeature sjekkRegistrereBrukerArenaFeature,
                RemoteFeatureConfig.RegistreringFeature skalRegistrereBrukerGenerellFeature,
                OppfolgingRepository oppfolgingRepository,
                NyeBrukereFeedRepository nyeBrukereFeedRepository,
                StartRegistreringStatusResolver startRegistreringStatusResolver)
        {
            return new BrukerRegistreringService(
                    arbeidssokerregistreringRepository,
                    oppfolgingRepository,
                    aktorService,
                    behandleArbeidssoekerV1,
                    sjekkRegistrereBrukerArenaFeature,
                    skalRegistrereBrukerGenerellFeature,
                    nyeBrukereFeedRepository,
                    startRegistreringStatusResolver
            );
        }

        @Bean
        PepClient pepClient() {
            return mock(PepClient.class);
        }
    }


}