package no.nav.veilarboppfolging.kandidatForUtmelding

import java.time.ZonedDateTime
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
import org.junit.jupiter.api.assertInstanceOf
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
    fun `Henter kandidat - skal gi ut kandidater som ikke er forlenget`() {
        val oppfolgingsbruker = arbeidssokerRegistrering(fnr, aktorId, BrukerRegistrant(fnr))
        oppfolgingsStatusRepository.opprettOppfolging(aktorId)
        oppfolgingsPeriodeRepository.start(oppfolgingsbruker)
        val oppfolgingsperiodeUuid = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId).first().uuid
        val avsluttet = arbeidssøkerPeriodeAvsluttet(oppfolgingsperiodeUuid)
        kandidatForUtmeldingRepository.lagreKandidat(avsluttet)

        val kandidat = kandidatForUtmeldingRepository.hentKandidat(oppfolgingsperiodeUuid)

        assertInstanceOf<ArbeidssøkerPeriodeAvsluttet>(kandidat)
        assertThat(kandidat.avslutningsarsak).isEqualTo(avsluttet.avslutningsarsak)
        assertThat(kandidat.oppfolgingsperiodeUuid).isEqualTo(oppfolgingsperiodeUuid)
        assertThat(kandidat.kilde).isEqualTo(avsluttet.kilde)
        assertThat(kandidat.utfortAv).isEqualTo(avsluttet.utfortAv)
        assertThat(kandidat.utfortAvType).isEqualTo(avsluttet.utfortAvType)
    }

    @Test
    fun `Hent kandidat - Skal ikke gi ut kandidater som er forlenget`() {
        val oppfolgingsbruker = arbeidssokerRegistrering(fnr, aktorId, BrukerRegistrant(fnr))
        oppfolgingsStatusRepository.opprettOppfolging(aktorId)
        oppfolgingsPeriodeRepository.start(oppfolgingsbruker)
        val oppfolgingsperiodeUuid = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId).first().uuid
        val avsluttet = arbeidssøkerPeriodeAvsluttet(oppfolgingsperiodeUuid)
        kandidatForUtmeldingRepository.lagreKandidat(avsluttet)
        namedJdbcTemplate.update("""
            UPDATE kandidater_for_utmelding SET forlenget_til = CURRENT_TIMESTAMP + INTERVAL '1 hour' WHERE oppfolgingsperiode_uuid = :oppfolgingsperiodeId
        """.trimIndent(), mapOf("oppfolgingsperiodeId" to oppfolgingsperiodeUuid.toString()))

        val kandidat = kandidatForUtmeldingRepository.hentKandidat(oppfolgingsperiodeUuid)

        assertThat(kandidat).isNull()
    }

    @Test
    fun `Hent kandidat - Skal ikke gi ut kandidater som er forlenget også når forlengelsen er gått ut`() {
        val oppfolgingsbruker = arbeidssokerRegistrering(fnr, aktorId, BrukerRegistrant(fnr))
        oppfolgingsStatusRepository.opprettOppfolging(aktorId)
        oppfolgingsPeriodeRepository.start(oppfolgingsbruker)
        val oppfolgingsperiodeUuid = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId).first().uuid
        val avsluttet = arbeidssøkerPeriodeAvsluttet(oppfolgingsperiodeUuid)
        kandidatForUtmeldingRepository.lagreKandidat(avsluttet)
        namedJdbcTemplate.update("""
            UPDATE kandidater_for_utmelding SET forlenget_til = CURRENT_TIMESTAMP - INTERVAL '1 hour' WHERE oppfolgingsperiode_uuid = :oppfolgingsperiodeId
        """.trimIndent(), mapOf("oppfolgingsperiodeId" to oppfolgingsperiodeUuid.toString()))

        val kandidat = kandidatForUtmeldingRepository.hentKandidat(oppfolgingsperiodeUuid)

        assertThat(kandidat).isNull()
    }

    @Test
    fun `hentEllerOpprettFilterhendelseId - oppretter og returnerer id hvis den ikke finnes`() {
        val oppfolgingsbruker = arbeidssokerRegistrering(fnr, aktorId, BrukerRegistrant(fnr))
        oppfolgingsStatusRepository.opprettOppfolging(aktorId)
        oppfolgingsPeriodeRepository.start(oppfolgingsbruker)
        val oppfolgingsperiodeUuid = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId).first().uuid

        val filterhendelseId = kandidatForUtmeldingRepository.hentEllerOpprettFilterhendelseId(oppfolgingsperiodeUuid)

        assertThat(filterhendelseId).isNotNull
    }

    @Test
    fun `hentEllerOpprettFilterhendelseId - oppretter og returnerer eksisterende id`() {
        val oppfolgingsbruker = arbeidssokerRegistrering(fnr, aktorId, BrukerRegistrant(fnr))
        oppfolgingsStatusRepository.opprettOppfolging(aktorId)
        oppfolgingsPeriodeRepository.start(oppfolgingsbruker)
        val oppfolgingsperiodeUuid = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId).first().uuid
        val opprinneligFilterhendelseId = kandidatForUtmeldingRepository.hentEllerOpprettFilterhendelseId(oppfolgingsperiodeUuid)

        val nyFilterhendelseId = kandidatForUtmeldingRepository.hentEllerOpprettFilterhendelseId(oppfolgingsperiodeUuid)

        assertThat(nyFilterhendelseId).isEqualTo(opprinneligFilterhendelseId)
    }

    @Test
    fun `lagreKafkaPublisering lagrer publiseringslogg for utmeldingshendelse`() {
        val oppfolgingsbruker = arbeidssokerRegistrering(fnr, aktorId, BrukerRegistrant(fnr))
        oppfolgingsStatusRepository.opprettOppfolging(aktorId)
        oppfolgingsPeriodeRepository.start(oppfolgingsbruker)
        val oppfolgingsperiodeUuid = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId).first().uuid
        val utmeldingshendelseId = kandidatForUtmeldingRepository.lagreKandidat(arbeidssøkerPeriodeAvsluttet(oppfolgingsperiodeUuid))

        kandidatForUtmeldingRepository.lagreKafkaPublisering(
            KandidatForUtmeldingKafkaPublisering(
                utmeldingshendelseId = utmeldingshendelseId,
                status = KandidatForUtmeldingKafkaPubliseringStatus.SENDT,
                kafkaTopic = "topic",
                kafkaPartition = 1,
                kafkaOffset = 10L,
                feilmelding = null,
            )
        )

        val kafkaPubliseringer = kandidatForUtmeldingRepository.hentKafkaPubliseringer(utmeldingshendelseId)
        assertThat(kafkaPubliseringer).hasSize(1)
        assertThat(kafkaPubliseringer.first().status).isEqualTo(KandidatForUtmeldingKafkaPubliseringStatus.SENDT)
        assertThat(kafkaPubliseringer.first().kafkaOffset).isEqualTo(10L)
    }

    fun arbeidssøkerPeriodeAvsluttet(oppfolgingsperiodeUuid: UUID) = ArbeidssøkerPeriodeAvsluttet(
        oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
        utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
        utfortAv = "A123123",
        kandidatForUtmeldingHendelseType = KandidatForUtmeldingHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
        avslutningsarsak = AvsluttetAarsakType.BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST.toString(),
        kilde = "kilde",
        hendelseTidspunkt = ZonedDateTime.now().toInstant(),
    )
}