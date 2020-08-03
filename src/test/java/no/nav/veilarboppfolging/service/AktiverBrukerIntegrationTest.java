package no.nav.veilarboppfolging.service;

import io.vavr.control.Try;
import no.nav.tjeneste.virksomhet.behandlearbeidssoeker.v1.binding.BehandleArbeidssoekerV1;
import no.nav.veilarboppfolging.domain.AktiverArbeidssokerData;
import no.nav.veilarboppfolging.domain.Fnr;
import no.nav.veilarboppfolging.domain.Innsatsgruppe;
import no.nav.veilarboppfolging.domain.Oppfolging;
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository;
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Optional;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

class AktiverBrukerIntegrationTest {

    private AnnotationConfigApplicationContext context;

    private OppfolgingsStatusRepository oppfolgingsStatusRepository;
    private OppfolgingRepositoryService oppfolgingRepositoryService;
    private OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository;
    private BehandleArbeidssoekerV1 behandleArbeidssoekerV1;
    private AktiverBrukerService aktiverBrukerService;
    private String ident = "1111";

    @BeforeEach
    public void setup() {
//        setTemporaryProperty(VEILARBOPPFOLGINGDB_URL_PROPERTY, createInMemoryDatabaseUrl(), () -> {
//            setTemporaryProperty(VEILARBOPPFOLGINGDB_USERNAME_PROPERTY, "sa", () -> {
//                setTemporaryProperty(VEILARBOPPFOLGINGDB_PASSWORD_PROPERTY, "pw", () -> {
//                    context = new AnnotationConfigApplicationContext(
//                            DatabaseConfig.class,
//                            BrukerregistreringConfigTest.class
//                    );
//
//                    context.start();
//
//                    aktiverBrukerService = context.getBean(AktiverBrukerService.class);
//                    oppfolgingRepository = context.getBean(OppfolgingRepository.class);
//                    oppfolgingsPeriodeRepository = context.getBean(OppfolgingsPeriodeRepository.class);
//                    aktorService = context.getBean(AktorService.class);
//                    behandleArbeidssoekerV1 = context.getBean(BehandleArbeidssoekerV1.class);
//                    migrateDatabase(context.getBean(DataSource.class));
//                });
//            });
//        });
    }

    @AfterEach
    public void tearDown() {
        context.stop();
    }

    @Test
    public void skalRulleTilbakeDatabaseDersomKallTilArenaFeiler() throws Exception {
        cofigureMocks();
        doThrow(new RuntimeException()).when(behandleArbeidssoekerV1).aktiverBruker(any());

        AktiverArbeidssokerData data = lagBruker(ident);

        Try<Void> run = Try.run(() -> aktiverBrukerService.aktiverBruker(data));
        assertThat(run.isFailure()).isTrue();

        Optional<Oppfolging> oppfolging = oppfolgingRepositoryService.hentOppfolging(ident);

        assertThat(oppfolging.isPresent()).isFalse();
    }

    @Test
    public void skalLagreIDatabaseDersomKallTilArenaErOK() throws Exception {
        cofigureMocks();

        aktiverBrukerService.aktiverBruker(lagBruker(ident));

        Optional<Oppfolging> oppfolging = oppfolgingRepositoryService.hentOppfolging(ident);

        assertThat(oppfolging.isPresent()).isTrue();
    }

    @Test
    public void skalHaandtereAtOppfolgingstatusAlleredeFinnes() {
        cofigureMocks();
        oppfolgingsStatusRepository.opprettOppfolging(ident);
        oppfolgingsPeriodeRepository.avslutt(ident, "veilederid", "begrunnelse");

        aktiverBrukerService.aktiverBruker(lagBruker(ident));

        Optional<Oppfolging> oppfolging = oppfolgingRepositoryService.hentOppfolging(ident);

        assertThat(oppfolging.get().isUnderOppfolging()).isTrue();
    }

    private void cofigureMocks() {
//        when(aktorService.getAktorId(any())).thenReturn(Optional.of(ident));
    }

    private AktiverArbeidssokerData lagBruker(String ident) {
        return new AktiverArbeidssokerData(new Fnr(ident), Innsatsgruppe.STANDARD_INNSATS);
    }

//    @Configuration
//    @ComponentScan
//    @Import({
//            DatabaseRepositoryConfig.class
//    })
//    public static class BrukerregistreringConfigTest {
//
//        @Bean
//        public AktorService aktoerService() {
//            return mock(AktorService.class);
//        }
//
//        @Bean
//        public BehandleArbeidssoekerV1 behandleArbeidssoekerV1() {
//            return mock(BehandleArbeidssoekerV1.class);
//        }
//
//        @Bean
//        AktiverBrukerService aktiverBrukerService(
//                AktorService aktorService,
//                BehandleArbeidssoekerV1 behandleArbeidssoekerV1,
//                OppfolgingRepository oppfolgingRepository,
//                NyeBrukereFeedRepository nyeBrukereFeedRepository) {
//            return new AktiverBrukerService(
//                    authService, oppfolgingRepository,
//                    aktorService,
//                    behandleArbeidssoekerV1,
//                    nyeBrukereFeedRepository
//            );
//        }
//
//        @Bean
//        PepClient pepClient() {
//            return mock(PepClient.class);
//        }
//    }

}
