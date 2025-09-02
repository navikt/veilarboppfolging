package no.nav.veilarboppfolging.service;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.IntegrationTest;
import no.nav.veilarboppfolging.LocalDatabaseSingleton;
import no.nav.veilarboppfolging.client.amtdeltaker.AmtDeltakerClient;
import no.nav.veilarboppfolging.client.digdir_krr.KRRData;
import no.nav.veilarboppfolging.domain.Oppfolging;
import no.nav.veilarboppfolging.eventsLogger.BigQueryClient;
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.AktiverBrukerManueltService;
import no.nav.veilarboppfolging.repository.*;
import no.nav.veilarboppfolging.test.DbTestUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AktiverBrukerIntegrationTest extends IntegrationTest {

    private OppfolgingsStatusRepository oppfolgingsStatusRepository;
    private AktiverBrukerManueltService aktiverBrukerManueltService;
    private ManuellStatusService manuellStatusService;
    private final Fnr FNR = Fnr.of("1111");
    private final AktorId AKTOR_ID = AktorId.of("1234523423");
    private BigQueryClient bigQueryClient;
    private KafkaProducerService kafkaProducerService;

    @Before
    public void setup() {
        JdbcTemplate jdbcTemplate = LocalDatabaseSingleton.INSTANCE.getJdbcTemplate();
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        TransactionTemplate transactor = DbTestUtils.createTransactor(jdbcTemplate);
        oppfolgingsStatusRepository = new OppfolgingsStatusRepository(new NamedParameterJdbcTemplate(jdbcTemplate));
        oppfolgingsPeriodeRepository = new OppfolgingsPeriodeRepository(jdbcTemplate, transactor);

        authService = mock(AuthService.class);
        when(authService.getFnrOrThrow(AKTOR_ID)).thenReturn(FNR);
        bigQueryClient = mock(BigQueryClient.class);
        kafkaProducerService = mock(KafkaProducerService.class);
        manuellStatusService = mock(ManuellStatusService.class);

        oppfolgingService = new OppfolgingService(
                kafkaProducerService,
                null,
                null,
                authService,
                oppfolgingsStatusRepository,
                oppfolgingsPeriodeRepository,
                manuellStatusService,
                mock(AmtDeltakerClient.class),
                new KvpRepository(jdbcTemplate, namedParameterJdbcTemplate, transactor),
                new MaalRepository(jdbcTemplate, transactor),
                mock(BrukerOppslagFlereOppfolgingAktorRepository.class),
                transactor,
                mock(ArenaYtelserService.class),
                bigQueryClient,
                "https://test.nav.no"
        );

        startOppfolgingService = new StartOppfolgingService(
                manuellStatusService,
                oppfolgingsStatusRepository,
                oppfolgingsPeriodeRepository,
                kafkaProducerService,
                bigQueryClient,
                transactor,
                arenaOppfolgingService,
                "https://test.nav.no"
        );

        aktiverBrukerManueltService = new AktiverBrukerManueltService(
                authService, startOppfolgingService,
                DbTestUtils.createTransactor(jdbcTemplate)
        );


        DbTestUtils.cleanupTestDb();
        when(authService.getAktorIdOrThrow(any(Fnr.class))).thenReturn(AKTOR_ID);
        when(authService.getInnloggetVeilederIdent()).thenReturn("G321321");
        when(manuellStatusService.hentDigdirKontaktinfo(any())).thenReturn(new KRRData());
    }

    @Test
    public void skalLagreIDatabaseDersomKallTilArenaErOK() {
        startOppfolgingSomArbeidsoker(AKTOR_ID, FNR);
        Optional<Oppfolging> oppfolging = oppfolgingService.hentOppfolging(AKTOR_ID);
        assertThat(oppfolging.isPresent()).isTrue();
    }

    @Test
    public void skalHaandtereAtOppfolgingstatusAlleredeFinnes() {
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID);
        oppfolgingsPeriodeRepository.avslutt(AKTOR_ID, "veilederid", "begrunnelse");
        startOppfolgingSomArbeidsoker(AKTOR_ID, FNR);
        Optional<Oppfolging> oppfolging = oppfolgingService.hentOppfolging(AKTOR_ID);
        assertThat(oppfolging.get().isUnderOppfolging()).isTrue();
    }

    @Test
    public void aktiver_sykmeldt_skal_starte_oppfolging() {
        var oppfolgingFør = oppfolgingService.hentOppfolging(AKTOR_ID);
        assertThat(oppfolgingFør.isEmpty()).isTrue();
        aktiverBrukerManueltService.aktiverBrukerManuelt(FNR, "1234");
        var oppfolging = oppfolgingService.hentOppfolging(AKTOR_ID);
        assertThat(oppfolging.get().isUnderOppfolging()).isTrue();
    }

}
