//package no.nav.fo.veilarboppfolging.db;
//
//import com.zaxxer.hikari.HikariDataSource;
//import net.javacrumbs.shedlock.core.LockingTaskExecutor;
//import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingFeedDTO;
//import no.nav.sbl.jdbc.DataSourceFactory;
//import no.nav.sbl.jdbc.Database;
//import org.flywaydb.core.Flyway;
//import org.junit.BeforeClass;
//import org.junit.ClassRule;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.junit.MockitoJUnitRunner;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.testcontainers.containers.OracleContainer;
//
//import java.util.List;
//
//import static no.nav.fo.veilarboppfolging.config.ApplicationConfig.APPLICATION_NAME;
//import static org.hamcrest.Matchers.is;
//import static org.junit.Assert.assertThat;
//import static org.mockito.Mockito.mock;
//
//@RunWith(MockitoJUnitRunner.class)
//public class OppfolgingFeedRepositoryTest {
//
//    private static final String AKTOR_ID = "2222";
//    private static final String VEILEDER = "1234";
//
//    @ClassRule
//    public static OracleContainer oracle = new OracleContainer("wnameless/oracle-xe-11g-r2");
//
//    private static OppfolgingFeedRepository feedRepository;
//    private static OppfolgingsPeriodeRepository periodeRepository;
//    private static VeilederTilordningerRepository tilordningerRepository;
//
//    @BeforeClass
//    public static void setup() {
//
//        HikariDataSource ds = DataSourceFactory.dataSource()
//                .url(oracle.getJdbcUrl())
//                .username(oracle.getUsername())
//                .password(oracle.getPassword())
//                .build();
//
//        Flyway flyway = new Flyway();
//        flyway.setSchemas(APPLICATION_NAME);
//        flyway.setDataSource(ds);
//        flyway.migrate();
//
//        JdbcTemplate jdbc = new JdbcTemplate(ds);
//        Database db = new Database(jdbc);
//
//        tilordningerRepository = new VeilederTilordningerRepository(db);
//        feedRepository = new OppfolgingFeedRepository(jdbc, mock(LockingTaskExecutor.class));
//        periodeRepository = new OppfolgingsPeriodeRepository(db);
//
//    }
//
//    @Test
//    public void skalHenteBrukere() {
//        tilordningerRepository.upsertVeilederTilordning(AKTOR_ID, VEILEDER);
//        assertThat(tilordningerRepository.hentTilordningForAktoer(AKTOR_ID), is(VEILEDER));
//        List<OppfolgingFeedDTO> oppfolgingFeedDTOS = feedRepository.hentEndringerEtterId("0", 2);
//        assertThat(oppfolgingFeedDTOS.size(), is(1));
//        assertThat(oppfolgingFeedDTOS.get(0).isNyForVeileder(), is(true));
//        assertThat(oppfolgingFeedDTOS.get(0).getVeileder(), is(VEILEDER));
//    }
//
//    @Test
//    public void skalHenteMaxBruker() {
//        tilordningerRepository.upsertVeilederTilordning(AKTOR_ID, VEILEDER);
//        tilordningerRepository.upsertVeilederTilordning("1111", VEILEDER);
//        tilordningerRepository.upsertVeilederTilordning("3333", VEILEDER);
//        tilordningerRepository.upsertVeilederTilordning("4444", VEILEDER);
//
//        assertThat(tilordningerRepository.hentTilordningForAktoer(AKTOR_ID), is(VEILEDER));
//        List<OppfolgingFeedDTO> oppfolgingFeedDTOS = feedRepository.hentEndringerEtterId("0", 2);
//        assertThat(oppfolgingFeedDTOS.size(), is(2));
//    }
//
//    @Test
//    public void skal_hente_ut_bruker_med_sluttdato_ut_paa_feed() {
//        tilordningerRepository.upsertVeilederTilordning(AKTOR_ID, VEILEDER);
//        List<OppfolgingFeedDTO> feedElementer = feedRepository.hentEndringerEtterId("0", 2);
//        periodeRepository.avslutt(AKTOR_ID, VEILEDER, "test");
//        assertThat(feedElementer.size(), is(1));
//        assertThat(feedElementer.get(0).getAktoerid(), is(AKTOR_ID));
//    }
//
//}
