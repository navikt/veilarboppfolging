package no.nav.veilarboppfolging.repository;

import no.nav.common.types.identer.AktorId;
import no.nav.veilarboppfolging.repository.entity.ManuellStatusEntity;
import no.nav.veilarboppfolging.repository.entity.OppfolgingEntity;
import no.nav.veilarboppfolging.repository.enums.KodeverkBruker;
import no.nav.veilarboppfolging.test.IsolatedDatabaseTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.ZonedDateTime;

import static java.time.temporal.ChronoUnit.MILLIS;
import static no.nav.veilarboppfolging.test.TestData.TEST_AKTOR_ID;
import static no.nav.veilarboppfolging.test.TestData.TEST_AKTOR_ID_2;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ManuellStatusRepositoryTest extends IsolatedDatabaseTest {

    private OppfolgingsStatusRepository oppfolgingsStatusRepository;

    private ManuellStatusRepository manuellStatusRepository;

    @Before
    public void setup() {
        oppfolgingsStatusRepository = new OppfolgingsStatusRepository(new NamedParameterJdbcTemplate(db));
        manuellStatusRepository = new ManuellStatusRepository(db, transactor);
    }

    @Test
    public void hentManuellStatus__should_return_manuell_status() {
        ManuellStatusEntity manuellStatus = createManuellStatus(TEST_AKTOR_ID, true, ZonedDateTime.now().truncatedTo(MILLIS));

        oppfolgingsStatusRepository.opprettOppfolging(TEST_AKTOR_ID);
        manuellStatusRepository.create(manuellStatus);

        OppfolgingEntity oppfolging = oppfolgingsStatusRepository.hentOppfolging(TEST_AKTOR_ID).orElseThrow();

        var maybeManuellStatus = manuellStatusRepository.hentManuellStatus(oppfolging.getGjeldendeManuellStatusId());

        assertTrue(maybeManuellStatus.isPresent());
        assertEquals(manuellStatus, maybeManuellStatus.get());
    }

    @Test
    public void hentManuellStatus__should_return_null_if_no_manuell_status() {
        assertTrue(manuellStatusRepository.hentManuellStatus(42L).isEmpty());
    }

    @Test
    public void history__should_return_all_manuell_statuser_for_user() {
        ManuellStatusEntity manuellStatus1 = createManuellStatus(TEST_AKTOR_ID, false, ZonedDateTime.now().minusSeconds(10));
        ManuellStatusEntity manuellStatus2 = createManuellStatus(TEST_AKTOR_ID, true, ZonedDateTime.now());
        ManuellStatusEntity manuellStatus3 = createManuellStatus(TEST_AKTOR_ID_2, true, ZonedDateTime.now());


        oppfolgingsStatusRepository.opprettOppfolging(TEST_AKTOR_ID);
        oppfolgingsStatusRepository.opprettOppfolging(TEST_AKTOR_ID_2);

        manuellStatusRepository.create(manuellStatus1);
        manuellStatusRepository.create(manuellStatus2);
        manuellStatusRepository.create(manuellStatus3);

        var statuser = manuellStatusRepository.history(TEST_AKTOR_ID);
        assertEquals(2, statuser.size());
    }

    private ManuellStatusEntity createManuellStatus(AktorId aktorId, boolean manuell, ZonedDateTime dato) {
        return new ManuellStatusEntity(
                null,
                aktorId.get(),
                manuell,
                dato,
                "begrunnelse",
                KodeverkBruker.SYSTEM,
                "test"
        );
    }

}
