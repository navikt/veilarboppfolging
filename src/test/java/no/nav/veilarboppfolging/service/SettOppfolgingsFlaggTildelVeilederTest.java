package no.nav.veilarboppfolging.service;

import no.nav.veilarboppfolging.controller.VeilederTilordningController;
import no.nav.veilarboppfolging.feed.cjm.producer.FeedProducer;
import no.nav.veilarboppfolging.feed.domain.OppfolgingFeedDTO;
import no.nav.veilarboppfolging.repository.VeilederTilordningerRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.jdbc.core.JdbcTemplate;

public class SettOppfolgingsFlaggTildelVeilederTest {

    private JdbcTemplate db;

    private VeilederTilordningerRepository veilederTilordningerRepository;

    @Mock
    private FeedProducer<OppfolgingFeedDTO> feed;

    @Mock
    private AuthService authService;

    private VeilederTilordningController veilederTilordningController;

    @Before
    public void setup() {
//        db = new JdbcTemplate(setupInMemoryDatabase());
//        Database database = new Database(db);
//        VeilederHistorikkRepository veilederHistorikkRepository = new VeilederHistorikkRepository(db);
//        veilederTilordningerRepository = new VeilederTilordningerRepository(database);
//        OppfolgingRepository oppfolgingRepository = new OppfolgingRepository(
//                pepClient,
//                new OppfolgingsStatusRepository(database),
//                new OppfolgingsPeriodeRepository(database),
//                mock(MaalRepository.class),
//                mock(ManuellStatusRepository.class),
//                mock(EskaleringsvarselRepository.class),
//                mock(KvpRepository.class),
//                mock(NyeBrukereFeedRepository.class)
//        );
//        veilederTilordningController = new VeilederTilordningController(
//                aktorServiceMock,
//                veilederTilordningerRepository,
//                pepClient,
//                feed,
//                authService,
//                oppfolgingRepository,
//                veilederHistorikkRepository,
//                new TestTransactor(),
//                mock(OppfolgingStatusKafkaProducer.class)
//        );

    }

    @Test
    public void skalSetteOppfolgingsflaggVedOPpdaterering() {
//        db.execute("INSERT INTO OPPFOLGINGSTATUS (aktor_id, oppdatert, under_oppfolging) " +
//                "VALUES ('1111111', CURRENT_TIMESTAMP, 0)");
//
//        assertTrue(db.queryForList("SELECT * FROM OPPFOLGINGSTATUS WHERE aktor_id = '1111111'").get(0).get("under_oppfolging").toString().equals("0"));
//        veilederTilordningController.skrivTilDatabase("1111111", "VEILEDER1");
//        assertTrue(db.queryForList("SELECT * FROM OPPFOLGINGSTATUS WHERE aktor_id = '1111111'").get(0).get("under_oppfolging").toString().equals("1"));

    }
}
