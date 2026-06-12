package no.nav.veilarboppfolging.kandidatForUtmelding

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.LocalDatabaseSingleton
import no.nav.veilarboppfolging.oppfolgingsbruker.BrukerRegistrant
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingsRegistrering.Companion.arbeidssokerRegistrering
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import no.nav.veilarboppfolging.repository.ReaktiveringRepository
import no.nav.veilarboppfolging.service.ReaktiverOppfolgingDto
import no.nav.veilarboppfolging.test.DbTestUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.jupiter.api.BeforeEach
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID

class KandidatForUtmeldingRepositoryTest {
    private val jdbcTemplate = LocalDatabaseSingleton.jdbcTemplate
    private val namedJdbcTemplate = NamedParameterJdbcTemplate(jdbcTemplate)
    private val transactor: TransactionTemplate = DbTestUtils.createTransactor(jdbcTemplate)
    val oppfolgingsPeriodeRepository = OppfolgingsPeriodeRepository(jdbcTemplate, transactor)
    val reaktiveringRepository = ReaktiveringRepository(namedJdbcTemplate)
    val kandidatForUtmeldingRepository = KandidatForUtmeldingRepository(namedJdbcTemplate)
    val oppfolgingsStatusRepository = OppfolgingsStatusRepository(NamedParameterJdbcTemplate(jdbcTemplate))
    val aktorId = AktorId.of("4321")
    val fnr = Fnr.of("1111119999")

    @BeforeEach
    fun setUp() {
        DbTestUtils.cleanupTestDb()
    }

    @Test
    fun `Skal ikke hente ut kandidat som har blitt reaktivert`() {
        val oppfolgingsbruker = arbeidssokerRegistrering(fnr, aktorId, BrukerRegistrant(fnr))
        oppfolgingsStatusRepository.opprettOppfolging(aktorId)
        oppfolgingsPeriodeRepository.start(oppfolgingsbruker)
        val oppfolgingsperiodeUuid = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId).first().getUuid()
        kandidatForUtmeldingRepository.lagreKandidat(arbeidssøkerPeriodeAvsluttet(oppfolgingsperiodeUuid))
        reaktiveringRepository.insertReaktivering(reaktiverOppfolging(oppfolgingsperiodeUuid))

        val kandidat = kandidatForUtmeldingRepository.hentKandidat(aktorId)

        assertThat(kandidat).isNull()
    }

    @Test
    fun `Skal hente ut kandidat som har blitt reaktivert tidligere`() {
        val oppfolgingsbruker = arbeidssokerRegistrering(fnr, aktorId, BrukerRegistrant(fnr))
        oppfolgingsStatusRepository.opprettOppfolging(aktorId)
        oppfolgingsPeriodeRepository.start(oppfolgingsbruker)
        val oppfolgingsperiodeUuid = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId).first().getUuid()
        reaktiveringRepository.insertReaktivering(reaktiverOppfolging(oppfolgingsperiodeUuid))
        kandidatForUtmeldingRepository.lagreKandidat(arbeidssøkerPeriodeAvsluttet(oppfolgingsperiodeUuid))

        val kandidat = kandidatForUtmeldingRepository.hentKandidat(aktorId)

        assertThat(kandidat).isNotNull()
    }

    fun arbeidssøkerPeriodeAvsluttet(oppfolgingsperiodeUuid: UUID) = ArbeidssøkerPeriodeAvsluttet(
        fnr = fnr,
        aktorId = aktorId,
        oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
        avsluttetAv = KandidatForUtmeldingHendelseAvsluttetAv.VEILEDER,
        aarsak = "årsak",
        kilde = "kilde"
    )

    fun reaktiverOppfolging(oppfolgingsperiodeUuid: UUID) = ReaktiverOppfolgingDto(
        aktorId = aktorId,
        oppfolgingsperiode = oppfolgingsperiodeUuid.toString(),
        veilederIdent = "A111111"
    )
}