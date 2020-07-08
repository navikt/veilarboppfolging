package no.nav.fo.veilarboppfolging.db;

import lombok.val;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import no.nav.fo.veilarboppfolging.domain.AktorId;
import no.nav.fo.veilarboppfolging.domain.Oppfolgingsperiode;
import no.nav.fo.veilarboppfolging.kafka.OppfolgingStatusKafkaProducer;
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
import java.util.function.Supplier;
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
    public void skal_hente_riktig_antall_brukere_med_oppfolgingstatus() {
        int expectedCount = 10;
        createAntallTestBrukere(expectedCount);

        Optional<Long> actualCount = feedRepository.hentAntallBrukere();
        assertThat(actualCount.isPresent()).isTrue();
        assertThat(actualCount.get()).isEqualTo(expectedCount);
    }

    @Test
    public void skal_hente_kun_en_bruker_siden_offset_i_oracle_er_ekslusiv() {
        createAntallTestBrukere(11);
        List<OppfolgingStatusKafkaProducer.Melding> dto = feedRepository.hentOppfolgingStatus(10);
        assertThat(dto.size()).isEqualTo(1);
    }

    @Test
    public void skal_hente_alle_brukere_selv_om_det_ikke_er_1000_brukere_igjen() {
        createAntallTestBrukere(15);
        List<OppfolgingStatusKafkaProducer.Melding> brukere = feedRepository.hentOppfolgingStatus(10);
        assertThat(brukere.size()).isEqualTo(5);
    }

    @Test
    public void skal_hente_null_brukere_om_offset_er_hoyere_en_antall_brukere() {
        createAntallTestBrukere(10);
        List<OppfolgingStatusKafkaProducer.Melding> brukere = feedRepository.hentOppfolgingStatus(20);
        assertThat(brukere.size()).isEqualTo(0);
    }

    @Test
    public void skal_sortere_elementer_i_paging_basert_paa_aktoer_id() {
        createAntallTestBrukere(2000);

        List<OppfolgingStatusKafkaProducer.Melding> page1 = feedRepository.hentOppfolgingStatus(0);
        assertThat(page1.size()).isEqualTo(1000);

        String sisteElementPage1 = page1.get(999).getAktoerid();

        List<OppfolgingStatusKafkaProducer.Melding> page2 = feedRepository.hentOppfolgingStatus(999);
        String foersteElementPage2 = page2.get(0).getAktoerid();

        assertThat(sisteElementPage1).isEqualTo(foersteElementPage2);

    }

    @Test
    public void skal_returnere_feilende_resultat_om_bruker_ikke_har_oppfolgingsperiode() {
        val sql = "INSERT INTO "
                + "OPPFOLGINGSTATUS "
                + "(AKTOR_ID, UNDER_OPPFOLGING, VEILEDER, NY_FOR_VEILEDER) "
                + "VALUES ('10101010101', 1, 'Z000000', 0)";

        jdbc.execute(sql);

        val man = "INSERT INTO "
                + "MANUELL_STATUS "
                + "(ID, AKTOR_ID, MANUELL, OPPRETTET_AV) "
                + "VALUES (1, '10101010101', 0, 'SYSTEM')";

        jdbc.execute(man);

        val result = feedRepository.hentOppfolgingStatus("10101010101");

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    public void skal_hente_bruker() {
        tilordningerRepository.upsertVeilederTilordning(AKTOR_ID, VEILEDER);
        periodeRepository.start(AKTOR_ID);

        val oppfolgingStatus = feedRepository.hentOppfolgingStatus(AKTOR_ID);

        assertThat(oppfolgingStatus.isSuccess()).isTrue();
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
                .getOrElseThrow((Supplier<IllegalStateException>) IllegalStateException::new);

        assertThat(startDato).isEqualTo(sistePeriode.getStartDato());
        assertThat(startDato).isNotEqualTo(forstePeriode.getStartDato());
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

    private void createAntallTestBrukere(int antall) {
        IntStream
                .range(1, antall+1)
                .forEach(n -> {
                    oppfolgingsStatusRepository.create(String.valueOf(n));
                    periodeRepository.start(String.valueOf(n));
                });
    }
}
