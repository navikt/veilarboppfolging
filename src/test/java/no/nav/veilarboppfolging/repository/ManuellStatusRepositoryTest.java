package no.nav.veilarboppfolging.repository;

import no.nav.common.types.identer.AktorId;
import no.nav.veilarboppfolging.domain.KodeverkBruker;
import no.nav.veilarboppfolging.domain.ManuellStatus;
import no.nav.veilarboppfolging.domain.OppfolgingTable;
import no.nav.veilarboppfolging.test.IsolatedDatabaseTest;
import org.junit.Before;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.Optional;

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
        ManuellStatus manuellStatus = createManuellStatus(TEST_AKTOR_ID);

        oppfolgingsStatusRepository.opprettOppfolging(TEST_AKTOR_ID);
        manuellStatusRepository.create(manuellStatus);

        OppfolgingTable oppfolging = oppfolgingsStatusRepository.fetch(TEST_AKTOR_ID);

        assertEquals(manuellStatus, manuellStatusRepository.fetch(oppfolging.getGjeldendeManuellStatusId()));
    }

    @Test
    public void fetch__should_return_null_if_no_manuell_status() {
        assertNull(manuellStatusRepository.fetch(42L));
    }

    @Test
    public void history__should_return_all_manuell_statuser_for_user() {
        ManuellStatus manuellStatus1 = createManuellStatus(TEST_AKTOR_ID)
                .setManuell(false)
                .setDato(ZonedDateTime.now().minusSeconds(10));

        ManuellStatus manuellStatus2 = createManuellStatus(TEST_AKTOR_ID)
                .setManuell(true)
                .setDato(ZonedDateTime.now());

        ManuellStatus manuellStatus3 = createManuellStatus(TEST_AKTOR_ID_2)
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
        ManuellStatus manuellStatus = createManuellStatus(TEST_AKTOR_ID);

        oppfolgingsStatusRepository.opprettOppfolging(TEST_AKTOR_ID);

        manuellStatusRepository.create(manuellStatus);

        Optional<ManuellStatus> maybeManuellStatus = manuellStatusRepository.hentSisteManuellStatus(TEST_AKTOR_ID);

        assertTrue(maybeManuellStatus.isPresent());
        assertEquals(manuellStatus, maybeManuellStatus.get());
    }

    @Test
    public void hentSisteManuellStatus__should_return_last_status() {
        ManuellStatus manuellStatus1 = createManuellStatus(TEST_AKTOR_ID)
                .setManuell(false)
                .setDato(ZonedDateTime.now().minusSeconds(10));

        ManuellStatus manuellStatus2 = createManuellStatus(TEST_AKTOR_ID)
                .setManuell(true)
                .setDato(ZonedDateTime.now());

        oppfolgingsStatusRepository.opprettOppfolging(TEST_AKTOR_ID);

        manuellStatusRepository.create(manuellStatus1);
        manuellStatusRepository.create(manuellStatus2);

        Optional<ManuellStatus> maybeManuellStatus = manuellStatusRepository.hentSisteManuellStatus(TEST_AKTOR_ID);

        assertTrue(maybeManuellStatus.isPresent());
        assertEquals(manuellStatus2, maybeManuellStatus.get());
    }

    private ManuellStatus createManuellStatus(AktorId aktorId) {
        return new ManuellStatus()
                .setAktorId(aktorId.get())
                .setManuell(true)
                .setDato(ZonedDateTime.now())
                .setBegrunnelse("begrunnelse")
                .setOpprettetAv(KodeverkBruker.SYSTEM)
                .setOpprettetAvBrukerId("test");
    }

}
