package no.nav.veilarboppfolging.repository;

import no.nav.common.types.identer.AktorId;
import no.nav.veilarboppfolging.repository.entity.ManuellStatusEntity;
import no.nav.veilarboppfolging.repository.entity.OppfolgingEntity;
import no.nav.veilarboppfolging.repository.enums.KodeverkBruker;
import no.nav.veilarboppfolging.test.IsolatedDatabaseTest;
import org.junit.Before;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.Optional;

import static java.time.temporal.ChronoUnit.MILLIS;
import static no.nav.veilarboppfolging.test.TestData.TEST_AKTOR_ID;
import static no.nav.veilarboppfolging.test.TestData.TEST_AKTOR_ID_2;
import static org.junit.Assert.*;

public class ManuellStatusRepositoryTest extends IsolatedDatabaseTest {

    private OppfolgingsStatusRepository oppfolgingsStatusRepository;

    private ManuellStatusRepository manuellStatusRepository;

    @Before
    public void setup() {
        oppfolgingsStatusRepository = new OppfolgingsStatusRepository(db);
        manuellStatusRepository = new ManuellStatusRepository(db, transactor);
    }

    @Test
    public void fetch__should_return_manuell_status() {
        ManuellStatusEntity manuellStatus = createManuellStatus(TEST_AKTOR_ID);

        oppfolgingsStatusRepository.opprettOppfolging(TEST_AKTOR_ID);
        manuellStatusRepository.create(manuellStatus);

        OppfolgingEntity oppfolging = oppfolgingsStatusRepository.fetch(TEST_AKTOR_ID);

        manuellStatus.setDato(manuellStatus.getDato().truncatedTo(MILLIS));
        ManuellStatusEntity fetched = manuellStatusRepository.fetch(oppfolging.getGjeldendeManuellStatusId());
        fetched.setDato(fetched.getDato().truncatedTo(MILLIS));
        assertEquals(manuellStatus, fetched);
    }

    @Test
    public void fetch__should_return_null_if_no_manuell_status() {
        assertNull(manuellStatusRepository.fetch(42L));
    }

    @Test
    public void history__should_return_all_manuell_statuser_for_user() {
        ManuellStatusEntity manuellStatus1 = createManuellStatus(TEST_AKTOR_ID)
                .setManuell(false)
                .setDato(ZonedDateTime.now().minusSeconds(10));

        ManuellStatusEntity manuellStatus2 = createManuellStatus(TEST_AKTOR_ID)
                .setManuell(true)
                .setDato(ZonedDateTime.now());

        ManuellStatusEntity manuellStatus3 = createManuellStatus(TEST_AKTOR_ID_2)
                .setManuell(true)
                .setDato(ZonedDateTime.now());


        oppfolgingsStatusRepository.opprettOppfolging(TEST_AKTOR_ID);
        oppfolgingsStatusRepository.opprettOppfolging(TEST_AKTOR_ID_2);

        manuellStatusRepository.create(manuellStatus1);
        manuellStatusRepository.create(manuellStatus2);
        manuellStatusRepository.create(manuellStatus3);

        var statuser = manuellStatusRepository.history(TEST_AKTOR_ID);
        assertEquals(2, statuser.size());
    }

    @Test
    public void hentSisteManuellStatus__should_return_empty_if_no_status() {
        assertTrue(manuellStatusRepository.hentSisteManuellStatus(TEST_AKTOR_ID).isEmpty());
    }

    @Test
    public void hentSisteManuellStatus__should_return_status() {
        ManuellStatusEntity manuellStatus = createManuellStatus(TEST_AKTOR_ID);

        oppfolgingsStatusRepository.opprettOppfolging(TEST_AKTOR_ID);

        manuellStatusRepository.create(manuellStatus);

        Optional<ManuellStatusEntity> maybeManuellStatus = manuellStatusRepository.hentSisteManuellStatus(TEST_AKTOR_ID);

        assertTrue(maybeManuellStatus.isPresent());
        manuellStatus.setDato(manuellStatus.getDato().truncatedTo(MILLIS));
        ManuellStatusEntity gotten = maybeManuellStatus.get();
        gotten.setDato(gotten.getDato().truncatedTo(MILLIS));
        assertEquals(manuellStatus, maybeManuellStatus.get());
    }

    @Test
    public void hentSisteManuellStatus__should_return_last_status() {
        ManuellStatusEntity manuellStatus1 = createManuellStatus(TEST_AKTOR_ID)
                .setManuell(false)
                .setDato(ZonedDateTime.now().minusSeconds(10));

        ManuellStatusEntity manuellStatus2 = createManuellStatus(TEST_AKTOR_ID)
                .setManuell(true)
                .setDato(ZonedDateTime.now());

        oppfolgingsStatusRepository.opprettOppfolging(TEST_AKTOR_ID);

        manuellStatusRepository.create(manuellStatus1);
        manuellStatusRepository.create(manuellStatus2);

        Optional<ManuellStatusEntity> maybeManuellStatus = manuellStatusRepository.hentSisteManuellStatus(TEST_AKTOR_ID);

        assertTrue(maybeManuellStatus.isPresent());
        manuellStatus2.setDato(manuellStatus2.getDato().truncatedTo(MILLIS));
        ManuellStatusEntity gotten = maybeManuellStatus.get();
        gotten.setDato(gotten.getDato().truncatedTo(MILLIS));
        assertEquals(manuellStatus2, gotten);
    }

    private ManuellStatusEntity createManuellStatus(AktorId aktorId) {
        return new ManuellStatusEntity()
                .setAktorId(aktorId.get())
                .setManuell(true)
                .setDato(ZonedDateTime.now())
                .setBegrunnelse("begrunnelse")
                .setOpprettetAv(KodeverkBruker.SYSTEM)
                .setOpprettetAvBrukerId("test");
    }

}
