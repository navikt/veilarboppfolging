package no.nav.veilarboppfolging.service;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.IntegrationTest;
import no.nav.veilarboppfolging.client.amttiltak.AmtTiltakClient;
import no.nav.veilarboppfolging.client.behandle_arbeidssoker.BehandleArbeidssokerClient;
import no.nav.veilarboppfolging.client.digdir_krr.KRRData;
import no.nav.veilarboppfolging.controller.request.Innsatsgruppe;
import no.nav.veilarboppfolging.controller.request.SykmeldtBrukerType;
import no.nav.veilarboppfolging.domain.Oppfolging;
import no.nav.veilarboppfolging.repository.*;
import no.nav.veilarboppfolging.test.DbTestUtils;
import no.nav.veilarboppfolging.test.LocalH2Database;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
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
        JdbcTemplate db = LocalH2Database.getDb();
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(db);
        TransactionTemplate transactor = DbTestUtils.createTransactor(db);

        oppfolgingsStatusRepository = new OppfolgingsStatusRepository(db);
        oppfolgingsPeriodeRepository = new OppfolgingsPeriodeRepository(db, transactor);

        authService = mock(AuthService.class);
        behandleArbeidssokerClient = mock(BehandleArbeidssokerClient.class);

        manuellStatusService = mock(ManuellStatusService.class);

        oppfolgingService = new OppfolgingService(
                mock(KafkaProducerService.class),
                null,
                null,
                null, authService,
                oppfolgingsStatusRepository, oppfolgingsPeriodeRepository,
                manuellStatusService,
                mock(AmtTiltakClient.class),
                new KvpRepository(db, namedParameterJdbcTemplate, transactor),
                new MaalRepository(db, transactor),
                mock(BrukerOppslagFlereOppfolgingAktorRepository.class),
                transactor
        );

        aktiverBrukerService = new AktiverBrukerService(
                authService, oppfolgingService,
                behandleArbeidssokerClient,
                DbTestUtils.createTransactor(db)
        );

        DbTestUtils.cleanupTestDb();
        when(authService.getAktorIdOrThrow(any(Fnr.class))).thenReturn(AKTOR_ID);
        when(manuellStatusService.hentDigdirKontaktinfo(any())).thenReturn(new KRRData());
    }

    @Test
    public void skalLagreIDatabaseDersomKallTilArenaErOK() {
        startOppfolging(AKTOR_ID, FNR);
        Optional<Oppfolging> oppfolging = oppfolgingService.hentOppfolging(AKTOR_ID);
        assertThat(oppfolging.isPresent()).isTrue();
    }

    @Test
    public void skalHaandtereAtOppfolgingstatusAlleredeFinnes() {
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID);
        oppfolgingsPeriodeRepository.avslutt(AKTOR_ID, "veilederid", "begrunnelse");
        startOppfolging(AKTOR_ID, FNR);
        Optional<Oppfolging> oppfolging = oppfolgingService.hentOppfolging(AKTOR_ID);
        assertThat(oppfolging.get().isUnderOppfolging()).isTrue();
    }

    @Test
    public void aktiver_sykmeldt_skal_starte_oppfolging() {
        var oppfolgingFør = oppfolgingService.hentOppfolging(AKTOR_ID);
        assertThat(oppfolgingFør.isEmpty()).isTrue();
        aktiverBrukerService.aktiverSykmeldt(FNR, SykmeldtBrukerType.SKAL_TIL_SAMME_ARBEIDSGIVER);
        var oppfolging = oppfolgingService.hentOppfolging(AKTOR_ID);
        assertThat(oppfolging.get().isUnderOppfolging()).isTrue();
    }

}
