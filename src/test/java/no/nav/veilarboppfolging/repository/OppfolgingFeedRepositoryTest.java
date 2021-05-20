package no.nav.veilarboppfolging.repository;

import no.nav.veilarboppfolging.domain.AktorId;
import no.nav.veilarboppfolging.domain.kafka.OppfolgingKafkaDTO;
import no.nav.veilarboppfolging.test.DbTestUtils;
import no.nav.veilarboppfolging.test.LocalH2Database;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OppfolgingFeedRepositoryTest {

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

    @Test
    public void hentOppfolgingsperiode_ingenTreff_skalKasteException() {
        assertThrows(EmptyResultDataAccessException.class, () -> oppfolgingsPeriodeRepository.hentOppfolgingsperiode("123"));
    }

    @Test
    public void hentOppfolgingsperiode_periodeFinnes_skalKasteException() {
        String aktorId = randomNumeric(10);
        oppfolgingsStatusRepository.opprettOppfolging(aktorId);
        oppfolgingsPeriodeRepository.start(aktorId);
        var perioder = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId);

        Assert.assertEquals(1, perioder.size());

        var periode = oppfolgingsPeriodeRepository.hentOppfolgingsperiode(perioder.get(0).getUuid().toString());

        assertNotNull(periode);
        assertEquals(perioder.get(0).getStartDato(), periode.getStartDato());
        assertEquals(perioder.get(0).getSluttDato(), periode.getSluttDato());

    }

    @Test
    public void hentOppfolgingsperiode_flerePerioder_skalKasteException() {
        String aktorId = randomNumeric(10);
        oppfolgingsStatusRepository.opprettOppfolging(aktorId);
        oppfolgingsPeriodeRepository.start(aktorId);
        oppfolgingsPeriodeRepository.avslutt(aktorId, "V123", "Fordi atte");
        oppfolgingsPeriodeRepository.start(aktorId);

        var ikkeAvsluttetPeriode = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId).stream().filter(x -> x.getSluttDato() != null);

        var eldstePeriode = ikkeAvsluttetPeriode.findFirst().orElse(null);

        var periode = oppfolgingsPeriodeRepository.hentOppfolgingsperiode(eldstePeriode.getUuid().toString());

        assertNotNull(periode);
        assertEquals(eldstePeriode.getStartDato(), periode.getStartDato());
        assertEquals(eldstePeriode.getSluttDato(), periode.getSluttDato());

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
