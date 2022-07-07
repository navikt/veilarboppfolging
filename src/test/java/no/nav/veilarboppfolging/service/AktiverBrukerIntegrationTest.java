package no.nav.veilarboppfolging.service;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.client.behandle_arbeidssoker.BehandleArbeidssokerClient;
import no.nav.veilarboppfolging.client.dkif.DkifKontaktinfo;
import no.nav.veilarboppfolging.controller.request.Innsatsgruppe;
import no.nav.veilarboppfolging.domain.Oppfolging;
import no.nav.veilarboppfolging.repository.*;
import no.nav.veilarboppfolging.test.DbTestUtils;
import no.nav.veilarboppfolging.test.LocalH2Database;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AktiverBrukerIntegrationTest {

    private OppfolgingsStatusRepository oppfolgingsStatusRepository;

    private OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository;

    private AuthService authService;

    private BehandleArbeidssokerClient behandleArbeidssokerClient;

    private OppfolgingService oppfolgingService;

    private AktiverBrukerService aktiverBrukerService;

    private ManuellStatusService manuellStatusService;

    private final Fnr FNR = Fnr.of("1111");

    private final AktorId AKTOR_ID = AktorId.of("1234523423");

    @Before
    public void setup() {
        JdbcTemplate db = LocalH2Database.getDb();
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
                null,
                null, authService,
                oppfolgingsStatusRepository, oppfolgingsPeriodeRepository,
                manuellStatusService,

                new KvpRepository(db, transactor),
                new MaalRepository(db, transactor),
                mock(BrukerOppslagFlereOppfolgingAktorRepository.class),
                null,
                transactor
        );

        aktiverBrukerService = new AktiverBrukerService(
                authService, oppfolgingService,
                behandleArbeidssokerClient,
                DbTestUtils.createTransactor(db)
        );

        DbTestUtils.cleanupTestDb();
        when(authService.getAktorIdOrThrow(any(Fnr.class))).thenReturn(AKTOR_ID);
        when(manuellStatusService.hentDkifKontaktinfo(any())).thenReturn(new DkifKontaktinfo());
    }

    @Test
    public void skalRulleTilbakeDatabaseDersomKallTilArenaFeiler() {
        doThrow(new RuntimeException()).when(behandleArbeidssokerClient).opprettBrukerIArena(any(), any());

        assertThrows(
                RuntimeException.class,
                () -> aktiverBrukerService.aktiverBruker(FNR, Innsatsgruppe.STANDARD_INNSATS)
        );

        Optional<Oppfolging> oppfolging = oppfolgingService.hentOppfolging(AKTOR_ID);

        assertThat(oppfolging.isPresent()).isFalse();
    }

    @Test
    public void skalLagreIDatabaseDersomKallTilArenaErOK() {
        aktiverBrukerService.aktiverBruker(FNR, Innsatsgruppe.STANDARD_INNSATS);

        Optional<Oppfolging> oppfolging = oppfolgingService.hentOppfolging(AKTOR_ID);

        assertThat(oppfolging.isPresent()).isTrue();
    }

    @Test
    public void skalHaandtereAtOppfolgingstatusAlleredeFinnes() {
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID);
        oppfolgingsPeriodeRepository.avslutt(AKTOR_ID, "veilederid", "begrunnelse");

        aktiverBrukerService.aktiverBruker(FNR, Innsatsgruppe.STANDARD_INNSATS);

        Optional<Oppfolging> oppfolging = oppfolgingService.hentOppfolging(AKTOR_ID);

        assertThat(oppfolging.get().isUnderOppfolging()).isTrue();
    }

}
