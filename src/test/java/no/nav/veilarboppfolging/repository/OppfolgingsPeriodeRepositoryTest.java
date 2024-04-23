package no.nav.veilarboppfolging.repository;

import no.nav.common.types.identer.AktorId;
import no.nav.veilarboppfolging.controller.request.Innsatsgruppe;
import no.nav.veilarboppfolging.domain.Oppfolgingsbruker;
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity;
import no.nav.veilarboppfolging.test.DbTestUtils;
import no.nav.veilarboppfolging.test.LocalH2Database;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class OppfolgingsPeriodeRepositoryTest {

    private final JdbcTemplate jdbcTemplate = LocalH2Database.getDb();
    private final TransactionTemplate transactor = DbTestUtils.createTransactor(jdbcTemplate);

    OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository = new OppfolgingsPeriodeRepository(jdbcTemplate, transactor);
    private final OppfolgingsStatusRepository oppfolgingsStatusRepository = new OppfolgingsStatusRepository(jdbcTemplate);

    @BeforeEach
    void setUp() {
        DbTestUtils.cleanupTestDb();
    }

    @Test
    void skal_hente_gjeldende_oppfolgingsperiode() {
        var oppfolgingsbruker = Oppfolgingsbruker.builder().innsatsgruppe(Innsatsgruppe.STANDARD_INNSATS).build();
        AktorId aktorId = AktorId.of("4321");
        oppfolgingsStatusRepository.opprettOppfolging(aktorId);

        oppfolgingsPeriodeRepository.start(aktorId, oppfolgingsbruker.getOppfolgingStartAarsak());
        oppfolgingsPeriodeRepository.avslutt(aktorId, "veileder", "derfor");
        oppfolgingsPeriodeRepository.start(aktorId, oppfolgingsbruker.getOppfolgingStartAarsak());
        Optional<OppfolgingsperiodeEntity> maybeOppfolgingsperiodeEntity = oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(aktorId);
        assertFalse(maybeOppfolgingsperiodeEntity.isEmpty());
        OppfolgingsperiodeEntity oppfolgingsperiodeEntity = maybeOppfolgingsperiodeEntity.get();
        assertEquals(aktorId.get(), oppfolgingsperiodeEntity.getAktorId());
        assertNull(oppfolgingsperiodeEntity.getSluttDato());
        assertNotNull(oppfolgingsperiodeEntity.getStartDato());
    }

    @Test
    void skal_returnere_empty_hvis_ingen_oppfolging() {
        var oppfolgingsbruker = Oppfolgingsbruker.builder().innsatsgruppe(Innsatsgruppe.STANDARD_INNSATS).build();
        AktorId aktorId = AktorId.of("4321");
        Optional<OppfolgingsperiodeEntity> maybeOppfolgingsperiodeEntity1 = oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(aktorId);
        assertTrue(maybeOppfolgingsperiodeEntity1.isEmpty());
        oppfolgingsStatusRepository.opprettOppfolging(aktorId);

        oppfolgingsPeriodeRepository.start(aktorId, oppfolgingsbruker.getOppfolgingStartAarsak());
        oppfolgingsPeriodeRepository.avslutt(aktorId, "veileder", "derfor");

        Optional<OppfolgingsperiodeEntity> maybeOppfolgingsperiodeEntity2 = oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(aktorId);
        assertTrue(maybeOppfolgingsperiodeEntity2.isEmpty());
    }
}