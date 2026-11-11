package no.nav.veilarboppfolging.sykmeldingstatus

import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.LocalDatabaseSingleton
import no.nav.veilarboppfolging.oppfolgingsbruker.BrukerRegistrant
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingsRegistrering.Companion.arbeidssokerRegistrering
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import no.nav.veilarboppfolging.test.DbTestUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.transaction.support.TransactionTemplate

class SisteSykmeldingstatusRepositoryTest {
    private val jdbcTemplate = LocalDatabaseSingleton.jdbcTemplate
    private val namedJdbcTemplate = NamedParameterJdbcTemplate(jdbcTemplate)
    private val sisteSykmeldingstatusRepository = SisteSykmeldingstatusRepository(namedJdbcTemplate)
    private val transactor: TransactionTemplate = DbTestUtils.createTransactor(jdbcTemplate)
    private val oppfolgingsPeriodeRepository = OppfolgingsPeriodeRepository(jdbcTemplate, transactor)
    val oppfolgingsStatusRepository = OppfolgingsStatusRepository(NamedParameterJdbcTemplate(jdbcTemplate))

    private val aktorId = AktorId.of("4321")
    private val fnr = Fnr.of("1111119999")

    @BeforeEach
    fun setUp() {
        DbTestUtils.cleanupTestDb()
    }

    @Test
    fun `lagreSisteSykmeldingstatus - finnes ikke fra før - blir lagret`() {
        val oppfolgingsperiodeUuid = lagreOppfolgingsperiode(aktorId, fnr)
        val sisteSykmeldingstatus = SisteSykmeldingstatus(
            oppfolgingsperiodeId = oppfolgingsperiodeUuid,
            fnr = fnr.get(),
            startdato = LocalDate.now(),
            sluttdato = LocalDate.now().plusDays(10),
            harArbeidsgiver = true,
        )

        sisteSykmeldingstatusRepository.lagreSisteSykmeldingstatus(sisteSykmeldingstatus)

        val sisteSykmeldingstatusFraDb = sisteSykmeldingstatusRepository.hentSisteSykmeldingstatus(oppfolgingsperiodeUuid)
        assertThat(sisteSykmeldingstatusFraDb).isNotNull
        assertThat(sisteSykmeldingstatusFraDb).isEqualTo(sisteSykmeldingstatus)
    }

    @Test
    fun `lagreSisteSykmeldingstatus - finnes fra før - blir oppdatert`() {
        val oppfolgingsperiodeUuid = lagreOppfolgingsperiode(aktorId, fnr)
        val sisteSykmeldingstatus = SisteSykmeldingstatus(
            oppfolgingsperiodeId = oppfolgingsperiodeUuid,
            fnr = fnr.get(),
            startdato = LocalDate.now(),
            sluttdato = LocalDate.now().plusDays(10),
            harArbeidsgiver = true,
        )
        sisteSykmeldingstatusRepository.lagreSisteSykmeldingstatus(sisteSykmeldingstatus)

        val oppdatertSisteSykmeldingstatus = sisteSykmeldingstatus.copy(
            sluttdato = LocalDate.now().plusDays(20),
            harArbeidsgiver = false,
        )

        sisteSykmeldingstatusRepository.lagreSisteSykmeldingstatus(oppdatertSisteSykmeldingstatus)

        val sisteSykmeldingstatusFraDb = sisteSykmeldingstatusRepository.hentSisteSykmeldingstatus(oppfolgingsperiodeUuid)
        assertThat(sisteSykmeldingstatusFraDb).isNotNull
        assertThat(sisteSykmeldingstatusFraDb).isEqualTo(oppdatertSisteSykmeldingstatus)
    }

    @Test
    fun `slettSisteSykmeldingstatus - sykmeldingstatus finnes - blir slettet`() {
        val oppfolgingsperiodeUuid = lagreOppfolgingsperiode(aktorId, fnr)
        val sisteSykmeldingstatus = SisteSykmeldingstatus(
            oppfolgingsperiodeId = oppfolgingsperiodeUuid,
            fnr = fnr.get(),
            startdato = LocalDate.now(),
            sluttdato = LocalDate.now().plusDays(10),
            harArbeidsgiver = true,
        )
        sisteSykmeldingstatusRepository.lagreSisteSykmeldingstatus(sisteSykmeldingstatus)
        assertThat(sisteSykmeldingstatusRepository.hentSisteSykmeldingstatus(oppfolgingsperiodeUuid)).isNotNull

        sisteSykmeldingstatusRepository.slettSisteSykmeldingstatus(oppfolgingsperiodeUuid)

        assertThat(sisteSykmeldingstatusRepository.hentSisteSykmeldingstatus(oppfolgingsperiodeUuid)).isNull()
    }

    private fun lagreOppfolgingsperiode(aktorId: AktorId, fnr: Fnr): UUID {
        val oppfolgingsbruker = arbeidssokerRegistrering(fnr, aktorId, BrukerRegistrant(fnr))
        oppfolgingsStatusRepository.opprettOppfolging(aktorId)
        oppfolgingsPeriodeRepository.start(oppfolgingsbruker)
        return oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId).first().uuid
    }
}