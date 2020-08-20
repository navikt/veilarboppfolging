package no.nav.veilarboppfolging.repository;

import lombok.val;
import no.nav.veilarboppfolging.domain.AktorId;
import no.nav.veilarboppfolging.domain.Oppfolgingsperiode;
import no.nav.veilarboppfolging.domain.kafka.OppfolgingKafkaDTO;
import no.nav.veilarboppfolging.test.DbTestUtils;
import no.nav.veilarboppfolging.test.LocalH2Database;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

public class OppfolgingFeedRepositoryTest {

    private static final String AKTOR_ID = "2222";
    private static final String VEILEDER = "1234";

    private OppfolgingFeedRepository oppfolgingFeedRepository = new OppfolgingFeedRepository(LocalH2Database.getDb());
    private OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository = new OppfolgingsPeriodeRepository(LocalH2Database.getDb());
    private VeilederTilordningerRepository veilederTilordningerRepository = new VeilederTilordningerRepository(LocalH2Database.getDb());
    private OppfolgingsStatusRepository oppfolgingsStatusRepository = new OppfolgingsStatusRepository(LocalH2Database.getDb());

    @Before
    public void cleanup() {
        DbTestUtils.cleanupTestDb();
    }

    @After
    public void tearDown() {
        JdbcTemplate db = LocalH2Database.getDb();

        db.execute("ALTER TABLE " + OppfolgingsStatusRepository.TABLE_NAME + " SET REFERENTIAL_INTEGRITY FALSE");
        db.execute("TRUNCATE TABLE " + OppfolgingsStatusRepository.TABLE_NAME);
        db.execute("TRUNCATE TABLE OPPFOLGINGSPERIODE");
    }

    @Test
    public void skal_hente_riktig_antall_brukere_med_oppfolgingstatus() {
        int expectedCount = 10;
        createAntallTestBrukere(expectedCount);

        Optional<Long> actualCount = oppfolgingFeedRepository.hentAntallBrukere();
        assertThat(actualCount.isPresent()).isTrue();
        assertThat(actualCount.get()).isEqualTo(expectedCount);
    }

    @Test
    public void skal_hente_kun_en_bruker_siden_offset_i_oracle_er_ekslusiv() {
        createAntallTestBrukere(11);
        List<OppfolgingKafkaDTO> dto = oppfolgingFeedRepository.hentOppfolgingStatus(10);
        assertThat(dto.size()).isEqualTo(1);
    }

    @Test
    public void skal_hente_alle_brukere_selv_om_det_ikke_er_1000_brukere_igjen() {
        createAntallTestBrukere(15);
        List<OppfolgingKafkaDTO> brukere = oppfolgingFeedRepository.hentOppfolgingStatus(10);
        assertThat(brukere.size()).isEqualTo(5);
    }

    @Test
    public void skal_hente_null_brukere_om_offset_er_hoyere_en_antall_brukere() {
        createAntallTestBrukere(10);
        List<OppfolgingKafkaDTO> brukere = oppfolgingFeedRepository.hentOppfolgingStatus(20);
        assertThat(brukere.size()).isEqualTo(0);
    }

    @Test
    public void skal_sortere_elementer_i_paging_basert_paa_aktoer_id() {
        createAntallTestBrukere(2000);

        List<OppfolgingKafkaDTO> page1 = oppfolgingFeedRepository.hentOppfolgingStatus(0);
        assertThat(page1.size()).isEqualTo(1000);

        String sisteElementPage1 = page1.get(999).getAktoerid();

        List<OppfolgingKafkaDTO> page2 = oppfolgingFeedRepository.hentOppfolgingStatus(999);
        String foersteElementPage2 = page2.get(0).getAktoerid();

        assertThat(sisteElementPage1).isEqualTo(foersteElementPage2);

    }

    @Test
    public void skal_returnere_feilende_resultat_om_bruker_ikke_har_oppfolgingsperiode() {
        JdbcTemplate db = LocalH2Database.getDb();

        val sql = "INSERT INTO "
                + "OPPFOLGINGSTATUS "
                + "(AKTOR_ID, UNDER_OPPFOLGING, VEILEDER, NY_FOR_VEILEDER) "
                + "VALUES ('10101010101', 1, 'Z000000', 0)";

        db.execute(sql);

        val man = "INSERT INTO "
                + "MANUELL_STATUS "
                + "(ID, AKTOR_ID, MANUELL, OPPRETTET_AV) "
                + "VALUES (1, '10101010101', 0, 'SYSTEM')";

        db.execute(man);

        val result = oppfolgingFeedRepository.hentOppfolgingStatus("10101010101");

        assertNull(result);
    }

    @Test
    public void skal_hente_bruker() {
        veilederTilordningerRepository.upsertVeilederTilordning(AKTOR_ID, VEILEDER);
        oppfolgingsPeriodeRepository.start(AKTOR_ID);

        val oppfolgingStatus = oppfolgingFeedRepository.hentOppfolgingStatus(AKTOR_ID);

        assertNotNull(oppfolgingStatus);
        assertEquals(AKTOR_ID, oppfolgingStatus.getAktoerid());
    }

    @Test
    public void skal_hente_startdato_for_siste_oppfolgingsperiode() throws InterruptedException {
        veilederTilordningerRepository.upsertVeilederTilordning(AKTOR_ID, VEILEDER);

        oppfolgingsPeriodeRepository.start(AKTOR_ID);
        oppfolgingsPeriodeRepository.avslutt(AKTOR_ID, VEILEDER, "test");

        Thread.sleep(1);

        oppfolgingsPeriodeRepository.start(AKTOR_ID);
        oppfolgingsPeriodeRepository.avslutt(AKTOR_ID, VEILEDER, "test");

        val perioder = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(AKTOR_ID);

        val forstePeriode = perioder.stream()
                .min(comparing(Oppfolgingsperiode::getStartDato))
                .orElseThrow(IllegalStateException::new);

        val sistePeriode = perioder.stream()
                .max(comparing(Oppfolgingsperiode::getStartDato))
                .orElseThrow(IllegalStateException::new);

        val startDato = ofNullable(oppfolgingFeedRepository.hentOppfolgingStatus(AKTOR_ID))
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
                    oppfolgingsStatusRepository.opprettOppfolging(aktoerId);
                    oppfolgingsPeriodeRepository.start(aktoerId);
                });

        // lage 1 bruker som ikke er under oppfølging
        String ikkeUnderOppfolging = "testId";
        oppfolgingsStatusRepository.opprettOppfolging(ikkeUnderOppfolging);
        oppfolgingsPeriodeRepository.start(ikkeUnderOppfolging);
        oppfolgingsPeriodeRepository.avslutt(ikkeUnderOppfolging, "", "");

        List<AktorId> aktorIds = oppfolgingFeedRepository.hentAlleBrukereUnderOppfolging();
        assertThat(aktorIds.size()).isEqualTo(10);
    }

    private void createAntallTestBrukere(int antall) {
        IntStream
                .range(1, antall+1)
                .forEach(n -> {
                    oppfolgingsStatusRepository.opprettOppfolging(String.valueOf(n));
                    oppfolgingsPeriodeRepository.start(String.valueOf(n));
                });
    }
}
