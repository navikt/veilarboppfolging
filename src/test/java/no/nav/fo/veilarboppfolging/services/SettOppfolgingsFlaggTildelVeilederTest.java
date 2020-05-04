package no.nav.fo.veilarboppfolging.services;

import no.nav.apiapp.security.PepClient;
import no.nav.brukerdialog.security.context.SubjectRule;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.feed.producer.FeedProducer;
import no.nav.fo.veilarboppfolging.TestTransactor;
import no.nav.fo.veilarboppfolging.db.*;
import no.nav.fo.veilarboppfolging.kafka.OppfolgingKafkaProducer;
import no.nav.fo.veilarboppfolging.rest.AutorisasjonService;
import no.nav.fo.veilarboppfolging.rest.VeilederTilordningRessurs;
import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingFeedDTO;
import no.nav.sbl.jdbc.Database;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.jdbc.core.JdbcTemplate;

import static no.nav.fo.veilarboppfolging.config.JndiLocalContextConfig.setupInMemoryDatabase;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class SettOppfolgingsFlaggTildelVeilederTest {

    private JdbcTemplate db;

    private VeilederTilordningerRepository veilederTilordningerRepository;

    @Mock
    private PepClient pepClient;

    @Mock
    private AktorService aktorServiceMock;

    @Mock
    private FeedProducer<OppfolgingFeedDTO> feed;

    @Mock
    private AutorisasjonService autorisasjonService;

    private VeilederTilordningRessurs veilederTilordningRessurs;

    @Rule
    public SubjectRule subjectRule = new SubjectRule();


    @Before
    public void setup() {
        db = new JdbcTemplate(setupInMemoryDatabase());
        Database database = new Database(db);
        VeilederHistorikkRepository veilederHistorikkRepository = new VeilederHistorikkRepository(db);
        veilederTilordningerRepository = new VeilederTilordningerRepository(database);
        OppfolgingRepository oppfolgingRepository = new OppfolgingRepository(
                pepClient,
                new OppfolgingsStatusRepository(database),
                new OppfolgingsPeriodeRepository(database),
                mock(MaalRepository.class),
                mock(ManuellStatusRepository.class),
                mock(EskaleringsvarselRepository.class),
                mock(KvpRepository.class),
                mock(NyeBrukereFeedRepository.class)
        );
        veilederTilordningRessurs = new VeilederTilordningRessurs(
                aktorServiceMock,
                veilederTilordningerRepository,
                pepClient,
                feed,
                autorisasjonService,
                oppfolgingRepository,
                veilederHistorikkRepository,
                new TestTransactor(),
                mock(OppfolgingKafkaProducer.class)
        );

    }

    @Test
    public void skalSetteOppfolgingsflaggVedOPpdaterering() {
        db.execute("INSERT INTO OPPFOLGINGSTATUS (aktor_id, oppdatert, under_oppfolging) " +
                "VALUES ('1111111', CURRENT_TIMESTAMP, 0)");

        assertTrue(db.queryForList("SELECT * FROM OPPFOLGINGSTATUS WHERE aktor_id = '1111111'").get(0).get("under_oppfolging").toString().equals("0"));
        veilederTilordningRessurs.skrivTilDatabase("1111111", "VEILEDER1");
        assertTrue(db.queryForList("SELECT * FROM OPPFOLGINGSTATUS WHERE aktor_id = '1111111'").get(0).get("under_oppfolging").toString().equals("1"));

    }
}
