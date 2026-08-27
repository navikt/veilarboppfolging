package no.nav.veilarboppfolging.kandidatForUtmelding

import java.time.LocalDate
import java.time.LocalDateTime
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
    fun `lagreKandidat - setter avsluttes_automatisk_dato til 28 dager etter hendelsestidspunkt ved første gangs lagring`() {
        val oppfolgingsbruker = arbeidssokerRegistrering(fnr, aktorId, BrukerRegistrant(fnr))
        oppfolgingsStatusRepository.opprettOppfolging(aktorId)
        oppfolgingsPeriodeRepository.start(oppfolgingsbruker)
        val oppfolgingsperiodeUuid = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId).first().uuid
        val avsluttet = arbeidssøkerPeriodeAvsluttet(oppfolgingsperiodeUuid)

        kandidatForUtmeldingRepository.lagreKandidat(avsluttet)

        val forventetDato = avsluttet.beregnAvsluttesAutomatiskDato()
        assertThat(kandidatForUtmeldingRepository.hentAvsluttesAutomatiskDato(oppfolgingsperiodeUuid)?.toLocalDateTime())
            .isEqualTo(forventetDato)
    }

    @Test
    fun `lagreKandidat - nullstiller avsluttes_automatisk_dato når forlengelse opprettes`() {
        val oppfolgingsbruker = arbeidssokerRegistrering(fnr, aktorId, BrukerRegistrant(fnr))
        oppfolgingsStatusRepository.opprettOppfolging(aktorId)
        oppfolgingsPeriodeRepository.start(oppfolgingsbruker)
        val oppfolgingsperiodeUuid = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId).first().uuid
        kandidatForUtmeldingRepository.lagreKandidat(arbeidssøkerPeriodeAvsluttet(oppfolgingsperiodeUuid))

        val forlengelse = ForlengelseHendelse(
            oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
            utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
            utfortAv = "A123123",
            kilde = "kilde",
            forlengelseHendelseType = ForlengelseHendelseType.FORLENGELSE_OPPRETTET,
            hendelseTidspunkt = ZonedDateTime.now().toInstant(),
            forlengetTil = LocalDate.now().plusDays(14),
        )
        kandidatForUtmeldingRepository.lagreKandidat(forlengelse)

        assertThat(kandidatForUtmeldingRepository.hentAvsluttesAutomatiskDato(oppfolgingsperiodeUuid)).isNull()
    }

    @Test
    fun `lagreKandidat - setter avsluttes_automatisk_dato på nytt når forlengelse utloper`() {
        val oppfolgingsbruker = arbeidssokerRegistrering(fnr, aktorId, BrukerRegistrant(fnr))
        oppfolgingsStatusRepository.opprettOppfolging(aktorId)
        oppfolgingsPeriodeRepository.start(oppfolgingsbruker)
        val oppfolgingsperiodeUuid = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId).first().uuid
        kandidatForUtmeldingRepository.lagreKandidat(arbeidssøkerPeriodeAvsluttet(oppfolgingsperiodeUuid))
        kandidatForUtmeldingRepository.lagreKandidat(
            ForlengelseHendelse(
                oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
                utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
                utfortAv = "A123123",
                kilde = "kilde",
                forlengelseHendelseType = ForlengelseHendelseType.FORLENGELSE_OPPRETTET,
                hendelseTidspunkt = ZonedDateTime.now().toInstant(),
                forlengetTil = LocalDate.now().plusDays(14),
            )
        )

        val utloptHendelseTidspunkt = ZonedDateTime.now().toInstant()
        val forlengelseUtlopt = ForlengelseHendelse(
            oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
            utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.SYSTEM,
            utfortAv = "SYSTEM",
            kilde = "kilde",
            forlengelseHendelseType = ForlengelseHendelseType.FORLENGELSE_UTLOPT,
            hendelseTidspunkt = utloptHendelseTidspunkt,
            forlengetTil = null,
        )
        kandidatForUtmeldingRepository.lagreKandidat(forlengelseUtlopt)

        val forventetDato = forlengelseUtlopt.beregnAvsluttesAutomatiskDato()
        assertThat(kandidatForUtmeldingRepository.hentAvsluttesAutomatiskDato(oppfolgingsperiodeUuid)?.toLocalDateTime())
            .isEqualTo(forventetDato)
    }

    @Test
    fun `settAvsluttesAutomatiskDatoHvisMangler - setter dato kun hvis den mangler`() {
        val oppfolgingsbruker = arbeidssokerRegistrering(fnr, aktorId, BrukerRegistrant(fnr))
        oppfolgingsStatusRepository.opprettOppfolging(aktorId)
        oppfolgingsPeriodeRepository.start(oppfolgingsbruker)
        val oppfolgingsperiodeUuid = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId).first().uuid
        kandidatForUtmeldingRepository.lagreKandidat(arbeidssøkerPeriodeAvsluttet(oppfolgingsperiodeUuid))
        // Simulerer en eksisterende kandidat som mangler feltet (opprettet før feltet ble innført)
        namedJdbcTemplate.update(
            "UPDATE kandidater_for_utmelding SET avsluttes_automatisk_dato = NULL WHERE oppfolgingsperiode_uuid = :id",
            mapOf("id" to oppfolgingsperiodeUuid.toString())
        )

        val nyDato = LocalDateTime.now().plusDays(28)
        kandidatForUtmeldingRepository.settAvsluttesAutomatiskDatoHvisMangler(oppfolgingsperiodeUuid, nyDato)
        assertThat(kandidatForUtmeldingRepository.hentAvsluttesAutomatiskDato(oppfolgingsperiodeUuid)?.toLocalDateTime())
            .isEqualTo(nyDato)

        // Skal ikke overskrive en allerede satt dato
        val forsokPaOverskriving = LocalDateTime.now().plusDays(100)
        kandidatForUtmeldingRepository.settAvsluttesAutomatiskDatoHvisMangler(oppfolgingsperiodeUuid, forsokPaOverskriving)
        assertThat(kandidatForUtmeldingRepository.hentAvsluttesAutomatiskDato(oppfolgingsperiodeUuid)?.toLocalDateTime())
            .isEqualTo(nyDato)
    }

    @Test
    fun `hentAktiveKandidater - returnerer ikke kandidater som er forlenget`() {
        val fnr2 = Fnr.of("2222229999")
        val aktorId2 = AktorId.of("8765")
        oppfolgingsStatusRepository.opprettOppfolging(aktorId)
        oppfolgingsStatusRepository.opprettOppfolging(aktorId2)
        oppfolgingsPeriodeRepository.start(arbeidssokerRegistrering(fnr, aktorId, BrukerRegistrant(fnr)))
        oppfolgingsPeriodeRepository.start(arbeidssokerRegistrering(fnr2, aktorId2, BrukerRegistrant(fnr2)))
        val aktivPeriode = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId).first().uuid
        val forlengetPeriode = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId2).first().uuid

        kandidatForUtmeldingRepository.lagreKandidat(arbeidssøkerPeriodeAvsluttet(aktivPeriode))
        kandidatForUtmeldingRepository.lagreKandidat(
            ForlengelseHendelse(
                oppfolgingsperiodeUuid = forlengetPeriode,
                utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
                utfortAv = "A123123",
                kilde = "kilde",
                forlengelseHendelseType = ForlengelseHendelseType.FORLENGELSE_OPPRETTET,
                hendelseTidspunkt = ZonedDateTime.now().toInstant(),
                forlengetTil = LocalDate.now().plusDays(10),
            )
        )

        val kandidater = kandidatForUtmeldingRepository.hentAktiveKandidater(offset = 0, batchSize = 10)

        assertThat(kandidater).hasSize(1)
        assertThat(kandidater.first().oppfolgingsperiodeUuid).isEqualTo(aktivPeriode)
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