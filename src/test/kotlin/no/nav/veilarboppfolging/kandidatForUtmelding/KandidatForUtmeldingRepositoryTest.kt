package no.nav.veilarboppfolging.kandidatForUtmelding

import java.time.ZonedDateTime
import java.util.UUID
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.paw.arbeidssokerregisteret.api.v1.AvsluttetAarsakType
import no.nav.veilarboppfolging.LocalDatabaseSingleton
import no.nav.veilarboppfolging.oppfolgingsbruker.BrukerRegistrant
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingsRegistrering.Companion.arbeidssokerRegistrering
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.AvregistreringsType
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
    fun `hentKandidaterMedUtloptForlengelse - returnerer kandidat med utløpt forlengelse`() {
        val oppfolgingsbruker = arbeidssokerRegistrering(fnr, aktorId, BrukerRegistrant(fnr))
        oppfolgingsStatusRepository.opprettOppfolging(aktorId)
        oppfolgingsPeriodeRepository.start(oppfolgingsbruker)
        val oppfolgingsperiodeUuid = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId).first().uuid
        val avsluttet = arbeidssøkerPeriodeAvsluttet(oppfolgingsperiodeUuid)
        kandidatForUtmeldingRepository.lagreKandidat(avsluttet)
        namedJdbcTemplate.update("""
            UPDATE kandidater_for_utmelding SET forlenget_til = CURRENT_TIMESTAMP - INTERVAL '1 hour' WHERE oppfolgingsperiode_uuid = :oppfolgingsperiodeId
        """.trimIndent(), mapOf("oppfolgingsperiodeId" to oppfolgingsperiodeUuid.toString()))

        val kandidater = kandidatForUtmeldingRepository.hentKandidaterMedUtloptForlengelse()

        assertThat(kandidater.size).isEqualTo(1)
        assertThat(kandidater.first().oppfolgingsperiodeUuid).isEqualTo(oppfolgingsperiodeUuid)
    }

    @Test
    fun `hentKandidaterMedUtloptForlengelse - returnerer ikke kandidater uten utløpt forlengelse`() {
        val oppfolgingsbruker = arbeidssokerRegistrering(fnr, aktorId, BrukerRegistrant(fnr))
        oppfolgingsStatusRepository.opprettOppfolging(aktorId)
        oppfolgingsPeriodeRepository.start(oppfolgingsbruker)
        val oppfolgingsperiodeUuid = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId).first().uuid
        val avsluttet = arbeidssøkerPeriodeAvsluttet(oppfolgingsperiodeUuid)
        kandidatForUtmeldingRepository.lagreKandidat(avsluttet)
        namedJdbcTemplate.update("""
            UPDATE kandidater_for_utmelding SET forlenget_til = CURRENT_TIMESTAMP + INTERVAL '1 hour' WHERE oppfolgingsperiode_uuid = :oppfolgingsperiodeId
        """.trimIndent(), mapOf("oppfolgingsperiodeId" to oppfolgingsperiodeUuid.toString()))

        val aktorId2 = AktorId.of("5678")
        val fnr2 = Fnr.of("1111118899")
        val oppfolgingsbruker2 = arbeidssokerRegistrering(fnr2, aktorId2, BrukerRegistrant(fnr2))
        oppfolgingsStatusRepository.opprettOppfolging(aktorId2)
        oppfolgingsPeriodeRepository.start(oppfolgingsbruker2)
        val oppfolgingsperiodeUuid2 = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId2).first().uuid
        val avsluttet2 = arbeidssøkerPeriodeAvsluttet(oppfolgingsperiodeUuid2)
        kandidatForUtmeldingRepository.lagreKandidat(avsluttet2)

        val kandidater = kandidatForUtmeldingRepository.hentKandidaterMedUtloptForlengelse()

        assertThat(kandidater.size).isEqualTo(0)
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
    fun `finnAntallKandidaterForUtmelding - returnerer antall kandidater`() {
        val oppfolgingsbruker = arbeidssokerRegistrering(fnr, aktorId, BrukerRegistrant(fnr))
        oppfolgingsStatusRepository.opprettOppfolging(aktorId)
        oppfolgingsPeriodeRepository.start(oppfolgingsbruker)
        val oppfolgingsperiodeUuid = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId).first().uuid

        kandidatForUtmeldingRepository.lagreKandidat(arbeidssøkerPeriodeAvsluttet(oppfolgingsperiodeUuid))

        assertThat(kandidatForUtmeldingRepository.hentAntallKandidaterForUtmelding()).isEqualTo(1)
    }

    @Test
    fun `hentAntallKandidaterForUtmeldingForlenget - returnerer antall forlengede kandidater`() {
        val oppfolgingsbruker = arbeidssokerRegistrering(fnr, aktorId, BrukerRegistrant(fnr))
        oppfolgingsStatusRepository.opprettOppfolging(aktorId)
        oppfolgingsPeriodeRepository.start(oppfolgingsbruker)
        val oppfolgingsperiodeUuid = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId).first().uuid

        kandidatForUtmeldingRepository.lagreKandidat(arbeidssøkerPeriodeAvsluttet(oppfolgingsperiodeUuid))
        namedJdbcTemplate.update(
            """
            UPDATE kandidater_for_utmelding
            SET forlenget_til = CURRENT_TIMESTAMP + INTERVAL '1 day'
            WHERE oppfolgingsperiode_uuid = :oppfolgingsperiodeId
            """.trimIndent(),
            mapOf("oppfolgingsperiodeId" to oppfolgingsperiodeUuid.toString())
        )

        assertThat(kandidatForUtmeldingRepository.hentAntallKandidaterForUtmeldingForlenget()).isEqualTo(1)
    }

    @Test
    fun `hentAntallKandidaterForUtmeldingIkkeForlenget - returnerer antall kandidater uten forlenget_til`() {
        val oppfolgingsbruker = arbeidssokerRegistrering(fnr, aktorId, BrukerRegistrant(fnr))
        val aktorId2 = AktorId.of("9876")
        val fnr2 = Fnr.of("2222229999")
        val oppfolgingsbruker2 = arbeidssokerRegistrering(fnr2, aktorId2, BrukerRegistrant(fnr2))

        oppfolgingsStatusRepository.opprettOppfolging(aktorId)
        oppfolgingsStatusRepository.opprettOppfolging(aktorId2)
        oppfolgingsPeriodeRepository.start(oppfolgingsbruker)
        oppfolgingsPeriodeRepository.start(oppfolgingsbruker2)

        val oppfolgingsperiodeUuid = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId).first().uuid
        val oppfolgingsperiodeUuid2 = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId2).first().uuid

        kandidatForUtmeldingRepository.lagreKandidat(arbeidssøkerPeriodeAvsluttet(oppfolgingsperiodeUuid))
        kandidatForUtmeldingRepository.lagreKandidat(arbeidssøkerPeriodeAvsluttet(oppfolgingsperiodeUuid2))
        namedJdbcTemplate.update(
            """
            UPDATE kandidater_for_utmelding
            SET forlenget_til = CURRENT_TIMESTAMP + INTERVAL '1 day'
            WHERE oppfolgingsperiode_uuid = :oppfolgingsperiodeId
            """.trimIndent(),
            mapOf("oppfolgingsperiodeId" to oppfolgingsperiodeUuid2.toString())
        )

        assertThat(kandidatForUtmeldingRepository.hentAntallKandidaterForUtmeldingIkkeForlenget()).isEqualTo(1)
    }

    @Test
    fun `hentAlleKandidatForUtmeldingHendelser - returnerer hendelser for alle oppfølgingsperioder`() {
        val oppfolgingsbruker = arbeidssokerRegistrering(fnr, aktorId, BrukerRegistrant(fnr))
        oppfolgingsStatusRepository.opprettOppfolging(aktorId)
        oppfolgingsPeriodeRepository.start(oppfolgingsbruker)
        val oppfolgingsperiodeUuid = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId).first().uuid
        val avsluttet = arbeidssøkerPeriodeAvsluttet(oppfolgingsperiodeUuid)
        kandidatForUtmeldingRepository.lagreKandidat(avsluttet)

        assertThat(kandidatForUtmeldingRepository.hentAlleKandidatForUtmeldingHendelser(aktorId).size).isEqualTo(1)

        oppfolgingsPeriodeRepository.avsluttSistePeriodeOgAvsluttOppfolging(
            aktorId,
            "Z123456",
            "begrunnelse",
            AvregistreringsType.AdminAvregistrering,
        )
        kandidatForUtmeldingRepository.fjernKandidat(oppfolgingsperiodeUuid)
        assertThat(kandidatForUtmeldingRepository.hentKandidat(oppfolgingsperiodeUuid)).isNull()

        oppfolgingsPeriodeRepository.start(oppfolgingsbruker)
        val nesteOppfolgingsperiodeUuid = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId).find { it.sluttDato == null }!!.uuid
        assertThat(oppfolgingsperiodeUuid).isNotEqualTo(nesteOppfolgingsperiodeUuid)
        val nesteAvsluttet = arbeidssøkerPeriodeAvsluttet(nesteOppfolgingsperiodeUuid)
        kandidatForUtmeldingRepository.lagreKandidat(nesteAvsluttet)
        assertThat(kandidatForUtmeldingRepository.hentKandidat(nesteOppfolgingsperiodeUuid)).isNotNull()

        val hendelser = kandidatForUtmeldingRepository.hentAlleKandidatForUtmeldingHendelser(aktorId)

        assertThat(hendelser.size).isEqualTo(2)
        assertThat { hendelser.first { it.oppfolgingsperiodeUuid == oppfolgingsperiodeUuid } }.isNotNull
        assertThat { hendelser.first { it.oppfolgingsperiodeUuid == nesteOppfolgingsperiodeUuid } }.isNotNull
    }

    fun arbeidssøkerPeriodeAvsluttet(oppfolgingsperiodeUuid: UUID) = ArbeidssøkerPeriodeAvsluttet(
        oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
        utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
        utfortAv = "A123123",
        arbeidssokerperiodeAvsluttetHendelseType = ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
        avslutningsarsak = AvsluttetAarsakType.BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST.toString(),
        kilde = "kilde",
        hendelseTidspunkt = ZonedDateTime.now().toInstant(),
    )
}