package no.nav.fo.veilarboppfolging.db;

import lombok.val;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import no.nav.fo.veilarboppfolging.domain.Oppfolgingsperiode;
import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingFeedDTO;
import no.nav.sbl.jdbc.Database;
import org.flywaydb.core.Flyway;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.AbstractDataSource;

import java.util.Date;
import java.util.Optional;

import static java.util.Comparator.comparing;
import static no.nav.fo.veilarboppfolging.config.ApplicationConfig.APPLICATION_NAME;
import static no.nav.fo.veilarboppfolging.config.JndiLocalContextConfig.setupInMemoryDatabase;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class OppfolgingFeedRepositoryTest {

    private static final String AKTOR_ID = "2222";
    private static final String VEILEDER = "1234";

    private static OppfolgingFeedRepository feedRepository;
    private static OppfolgingsPeriodeRepository periodeRepository;
    private static VeilederTilordningerRepository tilordningerRepository;

    @BeforeClass
    public static void setup() {

        AbstractDataSource ds = setupInMemoryDatabase();

        Flyway flyway = new Flyway();
        flyway.setSchemas(APPLICATION_NAME);
        flyway.setDataSource(ds);
        flyway.migrate();

        JdbcTemplate jdbc = new JdbcTemplate(ds);
        Database db = new Database(jdbc);

        tilordningerRepository = new VeilederTilordningerRepository(db);
        feedRepository = new OppfolgingFeedRepository(jdbc, mock(LockingTaskExecutor.class));
        periodeRepository = new OppfolgingsPeriodeRepository(db);
    }

    @Test
    public void skal_hente_bruker() {
        tilordningerRepository.upsertVeilederTilordning(AKTOR_ID, VEILEDER);
        periodeRepository.start(AKTOR_ID);

        Optional<OppfolgingFeedDTO> oppfolgingStatus = feedRepository.hentOppfolgingStatus(AKTOR_ID);

        assertThat(oppfolgingStatus.isPresent()).isTrue();
        assertThat(oppfolgingStatus.get().getAktoerid()).isEqualTo(AKTOR_ID);
    }

    @Test
    public void skal_hente_startdato_for_siste_oppfolgingsperiode() {
        tilordningerRepository.upsertVeilederTilordning(AKTOR_ID, VEILEDER);

        periodeRepository.start(AKTOR_ID);
        periodeRepository.avslutt(AKTOR_ID, VEILEDER, "test");

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

        assertThat(startDato).isEqualTo(sistePeriode.getStartDato());
        assertThat(startDato).isNotEqualTo(forstePeriode.getStartDato());
    }
}
