package no.nav.veilarboppfolging.service;

import no.nav.common.types.identer.AktorId;
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository;
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository;
import no.nav.veilarboppfolging.repository.VeilederTilordningerRepository;
import no.nav.veilarboppfolging.test.IsolatedDatabaseTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.mockito.Mockito.*;

public class KafkaRepubliseringServiceTest extends IsolatedDatabaseTest {

    private KafkaRepubliseringService kafkaRepubliseringService;

    private OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository;
    private OppfolgingsStatusRepository oppfolgingsStatusRepository;
    private VeilederTilordningerRepository veilederTilordningerRepository;
    private KafkaProducerService kafkaProducerService;

    @Before
    public void before() {
        oppfolgingsPeriodeRepository = new OppfolgingsPeriodeRepository(db, new TransactionTemplate(new DataSourceTransactionManager(db.getDataSource())));
        oppfolgingsStatusRepository = new OppfolgingsStatusRepository(db);
        veilederTilordningerRepository = new VeilederTilordningerRepository(db);

        kafkaProducerService = mock(KafkaProducerService.class);

        kafkaRepubliseringService = new KafkaRepubliseringService(
                oppfolgingsPeriodeRepository,
                oppfolgingsStatusRepository,
                veilederTilordningerRepository,
                kafkaProducerService
        );
    }

    @Test
    public void republiserEndringPaNyForVeileder__republiserer_for_alle_brukere_under_oppfolging_som_har_veileder() {
        AktorId aktorid1 = AktorId.of("aktorid_1");
        AktorId aktorid2 = AktorId.of("aktorid_2");
        AktorId aktorid3 = AktorId.of("aktorid_3");
        AktorId aktorid4 = AktorId.of("aktorid_4");

        oppfolgingsStatusRepository.opprettOppfolging(aktorid1);
        oppfolgingsStatusRepository.opprettOppfolging(aktorid2);
        oppfolgingsStatusRepository.opprettOppfolging(aktorid3);
        oppfolgingsStatusRepository.opprettOppfolging(aktorid4);

        oppfolgingsPeriodeRepository.start(aktorid1);
        oppfolgingsPeriodeRepository.start(aktorid2);
        oppfolgingsPeriodeRepository.start(aktorid3);

        veilederTilordningerRepository.upsertVeilederTilordning(aktorid1, "veileder1");
        veilederTilordningerRepository.upsertVeilederTilordning(aktorid3, "veileder2");
        veilederTilordningerRepository.upsertVeilederTilordning(aktorid4, "veileder2");

        veilederTilordningerRepository.markerSomLestAvVeileder(aktorid1);

        kafkaRepubliseringService.republiserEndringPaNyForVeilederForBrukereUnderOppfolging();

        verify(kafkaProducerService).publiserEndringPaNyForVeileder(aktorid1, false);
        verify(kafkaProducerService, never()).publiserEndringPaNyForVeileder(aktorid2, false);
        verify(kafkaProducerService, never()).publiserEndringPaNyForVeileder(aktorid2, true);
        verify(kafkaProducerService).publiserEndringPaNyForVeileder(aktorid3, true);
        verify(kafkaProducerService, never()).publiserEndringPaNyForVeileder(aktorid4, false);
        verify(kafkaProducerService, never()).publiserEndringPaNyForVeileder(aktorid4, true);
    }
}
