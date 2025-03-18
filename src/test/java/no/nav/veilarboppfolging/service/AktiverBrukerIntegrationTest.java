package no.nav.veilarboppfolging.service;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.IntegrationTest;
import no.nav.veilarboppfolging.LocalDatabaseSingleton;
import no.nav.veilarboppfolging.client.amttiltak.AmtTiltakClient;
import no.nav.veilarboppfolging.client.digdir_krr.KRRData;
import no.nav.veilarboppfolging.domain.Oppfolging;
import no.nav.veilarboppfolging.eventsLogger.BigQueryClient;
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.AktiverBrukerService;
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
import static org.mockito.Mockito.*;

public class AktiverBrukerIntegrationTest extends IntegrationTest {

    private OppfolgingsStatusRepository oppfolgingsStatusRepository;
    private AktiverBrukerService aktiverBrukerService;
    private ManuellStatusService manuellStatusService;
    private final Fnr FNR = Fnr.of("1111");
    private final AktorId AKTOR_ID = AktorId.of("1234523423");

    @Before
    public void setup() {
        JdbcTemplate jdbcTemplate = LocalDatabaseSingleton.INSTANCE.getJdbcTemplate();
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        TransactionTemplate transactor = DbTestUtils.createTransactor(jdbcTemplate);
        oppfolgingsStatusRepository = new OppfolgingsStatusRepository(new NamedParameterJdbcTemplate(jdbcTemplate));
        oppfolgingsPeriodeRepository = new OppfolgingsPeriodeRepository(jdbcTemplate, transactor);

        authService = mock(AuthService.class);

        manuellStatusService = mock(ManuellStatusService.class);

        oppfolgingService = new OppfolgingService(
                mock(KafkaProducerService.class),
                null,
                null,
                authService,
                oppfolgingsStatusRepository,
                oppfolgingsPeriodeRepository,
                manuellStatusService,
                mock(AmtTiltakClient.class),
                new KvpRepository(jdbcTemplate, namedParameterJdbcTemplate, transactor),
                new MaalRepository(jdbcTemplate, transactor),
                mock(BrukerOppslagFlereOppfolgingAktorRepository.class),
                transactor,
                mock(ArenaYtelserService.class),
                mock(BigQueryClient.class),
                "https://test.nav.no"
        );

        aktiverBrukerService = new AktiverBrukerService(
                authService, oppfolgingService,
                DbTestUtils.createTransactor(jdbcTemplate)
        );

        DbTestUtils.cleanupTestDb();
        when(authService.getAktorIdOrThrow(any(Fnr.class))).thenReturn(AKTOR_ID);
        when(authService.getInnloggetVeilederIdent()).thenReturn("G321321");
        when(manuellStatusService.hentDigdirKontaktinfo(any())).thenReturn(new KRRData());
    }

    @Test
    public void skalLagreIDatabaseDersomKallTilArenaErOK() {
        startOppfolgingSomArbeidsoker(AKTOR_ID);
        Optional<Oppfolging> oppfolging = oppfolgingService.hentOppfolging(AKTOR_ID);
        assertThat(oppfolging.isPresent()).isTrue();
    }

    @Test
    public void skalHaandtereAtOppfolgingstatusAlleredeFinnes() {
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID);
        oppfolgingsPeriodeRepository.avslutt(AKTOR_ID, "veilederid", "begrunnelse");
        startOppfolgingSomArbeidsoker(AKTOR_ID);
        Optional<Oppfolging> oppfolging = oppfolgingService.hentOppfolging(AKTOR_ID);
        assertThat(oppfolging.get().isUnderOppfolging()).isTrue();
    }

    @Test
    public void aktiver_sykmeldt_skal_starte_oppfolging() {
        var oppfolgingFør = oppfolgingService.hentOppfolging(AKTOR_ID);
        assertThat(oppfolgingFør.isEmpty()).isTrue();
        aktiverBrukerService.aktiverBrukerManuelt(FNR);
        var oppfolging = oppfolgingService.hentOppfolging(AKTOR_ID);
        assertThat(oppfolging.get().isUnderOppfolging()).isTrue();
    }

}
