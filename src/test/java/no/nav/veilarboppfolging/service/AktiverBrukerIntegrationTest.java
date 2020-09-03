package no.nav.veilarboppfolging.service;

import io.vavr.control.Try;
import no.nav.veilarboppfolging.client.behandle_arbeidssoker.BehandleArbeidssokerClient;
import no.nav.veilarboppfolging.domain.AktiverArbeidssokerData;
import no.nav.veilarboppfolging.domain.Fnr;
import no.nav.veilarboppfolging.domain.Innsatsgruppe;
import no.nav.veilarboppfolging.domain.Oppfolging;
import no.nav.veilarboppfolging.repository.*;
import no.nav.veilarboppfolging.test.DbTestUtils;
import no.nav.veilarboppfolging.test.LocalH2Database;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AktiverBrukerIntegrationTest {

    private OppfolgingsStatusRepository oppfolgingsStatusRepository;

    private OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository;

    private AuthService authService;

    private BehandleArbeidssokerClient behandleArbeidssokerClient;

    private OppfolgingService oppfolgingService;

    private AktiverBrukerService aktiverBrukerService;

    private final String IDENT = "1111";

    @Before
    public void setup() {
        JdbcTemplate db = LocalH2Database.getDb();
        oppfolgingsStatusRepository = new OppfolgingsStatusRepository(db);
        oppfolgingsPeriodeRepository = new OppfolgingsPeriodeRepository(db);

        authService = mock(AuthService.class);
        behandleArbeidssokerClient = mock(BehandleArbeidssokerClient.class);

        oppfolgingService = new OppfolgingService(
                mock(KafkaProducerService.class), null, null, null, null, null, authService,
                oppfolgingsStatusRepository, oppfolgingsPeriodeRepository,
                new ManuellStatusRepository(db), null,
                null, new EskaleringsvarselRepository(db),
                new KvpRepository(db), new NyeBrukereFeedRepository(db), new MaalRepository(db));

        aktiverBrukerService = new AktiverBrukerService(
                authService, oppfolgingService,
                behandleArbeidssokerClient, new NyeBrukereFeedRepository(db),
                DbTestUtils.getTransactor(db)
        );

        DbTestUtils.cleanupTestDb();
        when(authService.getAktorIdOrThrow(any())).thenReturn(IDENT);
    }

    @Test
    public void skalRulleTilbakeDatabaseDersomKallTilArenaFeiler() {
        doThrow(new RuntimeException()).when(behandleArbeidssokerClient).opprettBrukerIArena(anyString(), any());

        AktiverArbeidssokerData data = lagBruker(IDENT);

        Try<Void> run = Try.run(() -> aktiverBrukerService.aktiverBruker(data));
        assertThat(run.isFailure()).isTrue();

        Optional<Oppfolging> oppfolging = oppfolgingService.hentOppfolging(IDENT);

        assertThat(oppfolging.isPresent()).isFalse();
    }

    @Test
    public void skalLagreIDatabaseDersomKallTilArenaErOK() {
        aktiverBrukerService.aktiverBruker(lagBruker(IDENT));

        Optional<Oppfolging> oppfolging = oppfolgingService.hentOppfolging(IDENT);

        assertThat(oppfolging.isPresent()).isTrue();
    }

    @Test
    public void skalHaandtereAtOppfolgingstatusAlleredeFinnes() {
        oppfolgingsStatusRepository.opprettOppfolging(IDENT);
        oppfolgingsPeriodeRepository.avslutt(IDENT, "veilederid", "begrunnelse");

        aktiverBrukerService.aktiverBruker(lagBruker(IDENT));

        Optional<Oppfolging> oppfolging = oppfolgingService.hentOppfolging(IDENT);

        assertThat(oppfolging.get().isUnderOppfolging()).isTrue();
    }

    private AktiverArbeidssokerData lagBruker(String ident) {
        return new AktiverArbeidssokerData(new Fnr(ident), Innsatsgruppe.STANDARD_INNSATS);
    }

}
