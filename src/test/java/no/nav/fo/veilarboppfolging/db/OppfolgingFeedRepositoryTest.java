package no.nav.fo.veilarboppfolging.db;

import lombok.val;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import no.nav.fo.veilarboppfolging.domain.AktorId;
import no.nav.fo.veilarboppfolging.domain.Oppfolgingsperiode;
import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingKafkaDTO;
import no.nav.sbl.jdbc.Database;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.AbstractDataSource;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static java.util.Comparator.comparing;
import static no.nav.fo.veilarboppfolging.config.JndiLocalContextConfig.setupInMemoryDatabase;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class OppfolgingFeedRepositoryTest {

    private static final String AKTOR_ID = "2222";
    private static final String VEILEDER = "1234";

    private static OppfolgingFeedRepository feedRepository;
    private static OppfolgingsPeriodeRepository periodeRepository;
    private static VeilederTilordningerRepository tilordningerRepository;
    private static OppfolgingsStatusRepository oppfolgingsStatusRepository;
    private static JdbcTemplate jdbc;

    @BeforeClass
    public static void setup() {

        AbstractDataSource ds = setupInMemoryDatabase();
        jdbc = new JdbcTemplate(ds);
        Database db = new Database(jdbc);

        tilordningerRepository = new VeilederTilordningerRepository(db);
        feedRepository = new OppfolgingFeedRepository(jdbc, mock(LockingTaskExecutor.class));
        periodeRepository = new OppfolgingsPeriodeRepository(db);
        oppfolgingsStatusRepository = new OppfolgingsStatusRepository(db);
    }

    @After
    public void tearDown() {
        jdbc.execute("ALTER TABLE " + OppfolgingsStatusRepository.TABLE_NAME + " SET REFERENTIAL_INTEGRITY FALSE");
        jdbc.execute("TRUNCATE TABLE " + OppfolgingsStatusRepository.TABLE_NAME);
        jdbc.execute("TRUNCATE TABLE " + OppfolgingsPeriodeRepository.TABLE_NAME);
    }

    @Test
    public void skal_hente_bruker() {
        tilordningerRepository.upsertVeilederTilordning(AKTOR_ID, VEILEDER);
        periodeRepository.start(AKTOR_ID);

        Optional<OppfolgingKafkaDTO> oppfolgingStatus = feedRepository.hentOppfolgingStatus(AKTOR_ID);

        assertThat(oppfolgingStatus.isPresent()).isTrue();
        assertThat(oppfolgingStatus.get().getAktoerid()).isEqualTo(AKTOR_ID);
    }

    @Test
    public void skal_hente_startdato_for_siste_oppfolgingsperiode() throws InterruptedException {
        tilordningerRepository.upsertVeilederTilordning(AKTOR_ID, VEILEDER);

        periodeRepository.start(AKTOR_ID);
        periodeRepository.avslutt(AKTOR_ID, VEILEDER, "test");

        Thread.sleep(1);

        periodeRepository.start(AKTOR_ID);
        periodeRepository.avslutt(AKTOR_ID, VEILEDER, "test");

        val perioder = periodeRepository.hentOppfolgingsperioder(AKTOR_ID);

        val forstePeriode = perioder.stream()
                                   .min(comparing(Oppfolgingsperiode::getStartDato))
                                   .orElseThrow(IllegalStateException::new);

        val sistePeriode = perioder.stream()
                                   .max(comparing(Oppfolgingsperiode::getStartDato))
                                   .orElseThrow(IllegalStateException::new);

        val startDato = feedRepository.hentOppfolgingStatus(AKTOR_ID)
                                      .map(dto -> dto.getStartDato().getTime())
                                      .map(Date::new)
                                      .orElseThrow(IllegalStateException::new);

        assertThat(startDato.getTime()).isEqualTo(sistePeriode.getStartDato().getTime());
        assertThat(startDato.getTime()).isNotEqualTo(forstePeriode.getStartDato().getTime());
    }

    @Test
    public void skal_hente_alle_brukere_under_oppfolging() {
        // lag 10 brukere under oppfølging
        IntStream
                .range(1, 11)
                .mapToObj(String::valueOf)
                .forEach(aktoerId -> {
                    oppfolgingsStatusRepository.create(aktoerId);
                    periodeRepository.start(aktoerId);
                });

        // lage 1 bruker som ikke er under oppfølging
        String ikkeUnderOppfolging = "testId";
        oppfolgingsStatusRepository.create(ikkeUnderOppfolging);
        periodeRepository.start(ikkeUnderOppfolging);
        periodeRepository.avslutt(ikkeUnderOppfolging, "", "");

        List<AktorId> aktorIds = feedRepository.hentAlleBrukereUnderOppfolging();
        assertThat(aktorIds.size()).isEqualTo(10);
    }
}
