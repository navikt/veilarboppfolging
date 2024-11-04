package no.nav.veilarboppfolging.repository;

import no.nav.common.types.identer.AktorId;
import no.nav.veilarboppfolging.LocalDatabaseSingleton;
import no.nav.veilarboppfolging.controller.request.Innsatsgruppe;
import no.nav.veilarboppfolging.domain.Oppfolgingsbruker;
import no.nav.veilarboppfolging.domain.StartetAvType;
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity;
import no.nav.veilarboppfolging.test.DbTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class OppfolgingsPeriodeRepositoryTest {

    private final JdbcTemplate jdbcTemplate = LocalDatabaseSingleton.INSTANCE.getJdbcTemplate();
    private final TransactionTemplate transactor = DbTestUtils.createTransactor(jdbcTemplate);

    OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository = new OppfolgingsPeriodeRepository(jdbcTemplate, transactor);
    private final OppfolgingsStatusRepository oppfolgingsStatusRepository = new OppfolgingsStatusRepository(jdbcTemplate);

    @BeforeEach
    void setUp() {
        DbTestUtils.cleanupTestDb();
    }

    @Test
    void skal_hente_gjeldende_oppfolgingsperiode() {
        AktorId aktorId = AktorId.of("4321");
        var oppfolgingsbruker = Oppfolgingsbruker.arbeidssokerOppfolgingsBruker(aktorId, Innsatsgruppe.STANDARD_INNSATS, StartetAvType.Bruker);
        oppfolgingsStatusRepository.opprettOppfolging(aktorId);

        oppfolgingsPeriodeRepository.start(aktorId, oppfolgingsbruker.getOppfolgingStartBegrunnelse());
        oppfolgingsPeriodeRepository.avslutt(aktorId, "veileder", "derfor");
        oppfolgingsPeriodeRepository.start(aktorId, oppfolgingsbruker.getOppfolgingStartBegrunnelse());
        Optional<OppfolgingsperiodeEntity> maybeOppfolgingsperiodeEntity = oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(aktorId);
        assertFalse(maybeOppfolgingsperiodeEntity.isEmpty());
        OppfolgingsperiodeEntity oppfolgingsperiodeEntity = maybeOppfolgingsperiodeEntity.get();
        assertEquals(aktorId.get(), oppfolgingsperiodeEntity.getAktorId());
        assertNull(oppfolgingsperiodeEntity.getSluttDato());
        assertNotNull(oppfolgingsperiodeEntity.getStartDato());
    }

    @Test
    void skal_returnere_empty_hvis_ingen_oppfolging() {
        AktorId aktorId = AktorId.of("4321");
        var oppfolgingsbruker = Oppfolgingsbruker.arbeidssokerOppfolgingsBruker(aktorId, Innsatsgruppe.STANDARD_INNSATS, StartetAvType.Bruker);
        Optional<OppfolgingsperiodeEntity> maybeOppfolgingsperiodeEntity1 = oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(aktorId);
        assertTrue(maybeOppfolgingsperiodeEntity1.isEmpty());
        oppfolgingsStatusRepository.opprettOppfolging(aktorId);

        oppfolgingsPeriodeRepository.start(aktorId, oppfolgingsbruker.getOppfolgingStartBegrunnelse());
        oppfolgingsPeriodeRepository.avslutt(aktorId, "veileder", "derfor");

        Optional<OppfolgingsperiodeEntity> maybeOppfolgingsperiodeEntity2 = oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(aktorId);
        assertTrue(maybeOppfolgingsperiodeEntity2.isEmpty());
    }
}