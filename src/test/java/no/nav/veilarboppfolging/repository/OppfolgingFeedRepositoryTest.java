package no.nav.veilarboppfolging.repository;

import no.nav.common.types.identer.AktorId;
import no.nav.veilarboppfolging.test.DbTestUtils;
import no.nav.veilarboppfolging.test.LocalH2Database;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OppfolgingFeedRepositoryTest {

    private final JdbcTemplate db = LocalH2Database.getDb();
    private final TransactionTemplate transactor = DbTestUtils.createTransactor(db);


    private OppfolgingFeedRepository oppfolgingFeedRepository = new OppfolgingFeedRepository(db);
    private OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository = new OppfolgingsPeriodeRepository(db, transactor);
    private OppfolgingsStatusRepository oppfolgingsStatusRepository = new OppfolgingsStatusRepository(db);

    @Before
    public void cleanup() {
        DbTestUtils.cleanupTestDb();
    }

    @After
    public void tearDown() {
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
    public void skal_hente_alle_brukere_under_oppfolging() {
        // lag 10 brukere under oppfølging
        IntStream
                .range(1, 11)
                .mapToObj(String::valueOf)
                .map(AktorId::of)
                .forEach(aktorId -> {
                    oppfolgingsStatusRepository.opprettOppfolging(aktorId);
                    oppfolgingsPeriodeRepository.start(aktorId);
                });

        // lage 1 bruker som ikke er under oppfølging
        AktorId ikkeUnderOppfolging = AktorId.of("testId");
        oppfolgingsStatusRepository.opprettOppfolging(ikkeUnderOppfolging);
        oppfolgingsPeriodeRepository.start(ikkeUnderOppfolging);
        oppfolgingsPeriodeRepository.avslutt(ikkeUnderOppfolging, "", "");

        List<String> aktorIds = hentAlleBrukereUnderOppfolging(LocalH2Database.getDb());
        assertThat(aktorIds.size()).isEqualTo(10);
    }

    @Test
    public void hentOppfolgingsperiode_ingenTreff_skalVereEmpty() {
        assertTrue(oppfolgingsPeriodeRepository.hentOppfolgingsperiode("123").isEmpty());
    }

    @Test
    public void hentOppfolgingsperiode_periodeFinnes() {
        AktorId aktorId = AktorId.of(randomNumeric(10));
        oppfolgingsStatusRepository.opprettOppfolging(aktorId);
        oppfolgingsPeriodeRepository.start(aktorId);
        var perioder = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId);
        var forstePeriode = perioder.get(0);

        Assert.assertEquals(1, perioder.size());

        var maybePeriode = oppfolgingsPeriodeRepository.hentOppfolgingsperiode(perioder.get(0).getUuid().toString());

        assertTrue(maybePeriode.isPresent());

        var periode = maybePeriode.get();

        assertEquals(forstePeriode.getStartDato(), periode.getStartDato());
        assertEquals(forstePeriode.getSluttDato(), periode.getSluttDato());

    }

    @Test
    public void hentOppfolgingsperiode_flerePerioder() {
        AktorId aktorId = AktorId.of(randomNumeric(10));
        oppfolgingsStatusRepository.opprettOppfolging(aktorId);
        oppfolgingsPeriodeRepository.start(aktorId);
        oppfolgingsPeriodeRepository.avslutt(aktorId, "V123", "Fordi atte");
        oppfolgingsPeriodeRepository.start(aktorId);

        var avsluttetPeriode = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId).stream().filter(x -> x.getSluttDato() != null);

        var eldstePeriode = avsluttetPeriode.findFirst().orElse(null);

        var maybePeriode = oppfolgingsPeriodeRepository.hentOppfolgingsperiode(eldstePeriode.getUuid().toString());

        assertTrue(maybePeriode.isPresent());

        var periode = maybePeriode.get();

        assertEquals(eldstePeriode.getStartDato(), periode.getStartDato());
        assertEquals(eldstePeriode.getSluttDato(), periode.getSluttDato());

    }

    private List<String> hentAlleBrukereUnderOppfolging(JdbcTemplate db) {
        String sql = "SELECT * FROM OPPFOLGINGSTATUS WHERE UNDER_OPPFOLGING = 1";
        return db.query(sql, (rs, row) -> rs.getString("AKTOR_ID"));
    }

    private void createAntallTestBrukere(int antall) {
        IntStream
                .range(1, antall+1)
                .mapToObj(String::valueOf)
                .map(AktorId::of)
                .forEach(aktorId -> {
                    oppfolgingsStatusRepository.opprettOppfolging(aktorId);
                    oppfolgingsPeriodeRepository.start(aktorId);
                });
    }
}
