package no.nav.veilarboppfolging.kandidatForUtmelding

import java.util.UUID
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.paw.arbeidssokerregisteret.api.v1.AvsluttetAarsakType
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

class KandidatForUtmeldingRepositoryTest {
    private val jdbcTemplate = LocalDatabaseSingleton.jdbcTemplate
    private val namedJdbcTemplate = NamedParameterJdbcTemplate(jdbcTemplate)
    private val transactor: TransactionTemplate = DbTestUtils.createTransactor(jdbcTemplate)
    val oppfolgingsPeriodeRepository = OppfolgingsPeriodeRepository(jdbcTemplate, transactor)
    val kandidatForUtmeldingRepository = KandidatForUtmeldingRepository(namedJdbcTemplate)
    val oppfolgingsStatusRepository = OppfolgingsStatusRepository(NamedParameterJdbcTemplate(jdbcTemplate))
    val aktorId = AktorId.of("4321")
    val fnr = Fnr.of("1111119999")

    @BeforeEach
    fun setUp() {
        DbTestUtils.cleanupTestDb()
    }

    @Test
    fun `Henter kandidat`() {
        val oppfolgingsbruker = arbeidssokerRegistrering(fnr, aktorId, BrukerRegistrant(fnr))
        oppfolgingsStatusRepository.opprettOppfolging(aktorId)
        oppfolgingsPeriodeRepository.start(oppfolgingsbruker)
        val oppfolgingsperiodeUuid = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId).first().uuid
        kandidatForUtmeldingRepository.lagreKandidat(arbeidssøkerPeriodeAvsluttet(oppfolgingsperiodeUuid))

        val kandidat = kandidatForUtmeldingRepository.hentKandidat(aktorId)

        assertThat(kandidat).isNotNull()
    }

    fun arbeidssøkerPeriodeAvsluttet(oppfolgingsperiodeUuid: UUID) = ArbeidssøkerPeriodeAvsluttet(
        fnr = fnr,
        aktorId = aktorId,
        oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
        avsluttetAv = KandidatForUtmeldingHendelseAvsluttetAv.VEILEDER,
        kandidatForUtmeldingHendelseType = KandidatForUtmeldingHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
        detaljer = AvsluttetAarsakType.BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST.toString(),
        kilde = "kilde"
    )
}