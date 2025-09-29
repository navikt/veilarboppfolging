package no.nav.veilarboppfolging.repository

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NavIdent
import no.nav.veilarboppfolging.LocalDatabaseSingleton
import no.nav.veilarboppfolging.oppfolgingsbruker.StartetAvType
import no.nav.veilarboppfolging.oppfolgingsbruker.BrukerRegistrant
import no.nav.veilarboppfolging.oppfolgingsbruker.VeilederRegistrant
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingStartBegrunnelse
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingsRegistrering
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingsRegistrering.Companion.arbeidssokerRegistrering
import no.nav.veilarboppfolging.test.DbTestUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.transaction.support.TransactionTemplate
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class OppfolgingsPeriodeRepositoryTest {
    private val jdbcTemplate = LocalDatabaseSingleton.jdbcTemplate
    private val transactor: TransactionTemplate = DbTestUtils.createTransactor(jdbcTemplate)

    var oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository =
        OppfolgingsPeriodeRepository(jdbcTemplate, transactor)
    private val oppfolgingsStatusRepository = OppfolgingsStatusRepository(NamedParameterJdbcTemplate(jdbcTemplate))

    @BeforeEach
    fun setUp() {
        DbTestUtils.cleanupTestDb()
    }

    @Test
    fun skal_hente_gjeldende_oppfolgingsperiode() {
        val aktorId = AktorId.of("4321")
        val fnr = Fnr.of("1111119999")
        val oppfolgingsbruker =
            arbeidssokerRegistrering(fnr, aktorId, BrukerRegistrant(fnr))
        oppfolgingsStatusRepository.opprettOppfolging(aktorId)

        oppfolgingsPeriodeRepository.start(oppfolgingsbruker)
        oppfolgingsPeriodeRepository.avsluttSistePeriodeOgAvsluttOppfolging(aktorId, "veileder", "derfor")
        oppfolgingsPeriodeRepository.start(oppfolgingsbruker)
        val maybeOppfolgingsperiodeEntity = oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(aktorId)
        Assertions.assertFalse(maybeOppfolgingsperiodeEntity.isEmpty())
        val oppfolgingsperiodeEntity = maybeOppfolgingsperiodeEntity.get()
        Assertions.assertEquals(aktorId.get(), oppfolgingsperiodeEntity.getAktorId())
        Assertions.assertNull(oppfolgingsperiodeEntity.getSluttDato())
        Assertions.assertNotNull(oppfolgingsperiodeEntity.getStartDato())
    }

    @Test
    fun skal_returnere_empty_hvis_ingen_oppfolging() {
        val aktorId = AktorId.of("4321")
        val fnr = Fnr.of("1111119999")
        val oppfolgingsbruker =
            arbeidssokerRegistrering(fnr, aktorId, BrukerRegistrant(fnr))
        val maybeOppfolgingsperiodeEntity1 = oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(aktorId)
        Assertions.assertTrue(maybeOppfolgingsperiodeEntity1.isEmpty())
        oppfolgingsStatusRepository.opprettOppfolging(aktorId)

        oppfolgingsPeriodeRepository.start(oppfolgingsbruker)
        oppfolgingsPeriodeRepository.avsluttSistePeriodeOgAvsluttOppfolging(aktorId, "veileder", "derfor")

        val maybeOppfolgingsperiodeEntity2 = oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(aktorId)
        Assertions.assertTrue(maybeOppfolgingsperiodeEntity2.isEmpty())
    }

    @Test
    fun `skal returnere startetAv og startetAvType`() {
        val aktorId = AktorId.of("4321")
        val fnr = Fnr.of("1111119999")
        val oppfolgingsbruker = arbeidssokerRegistrering(fnr, aktorId, BrukerRegistrant(fnr))
        oppfolgingsStatusRepository.opprettOppfolging(aktorId)
        oppfolgingsPeriodeRepository.start(oppfolgingsbruker)

        val perioder = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId)

        assertEquals(perioder.size, 1)
        val periode = perioder[0]
        assertEquals(StartetAvType.BRUKER, periode.startetAvType)
        assertEquals(fnr.get(), periode.startetAv)
    }

    @Test
    fun `skal håndtere av startetAv og startetAvType begge er null`() {
        val aktorId = AktorId.of("4321")
        oppfolgingsStatusRepository.opprettOppfolging(aktorId)
        val id = UUID.randomUUID()
        NamedParameterJdbcTemplate(LocalDatabaseSingleton.jdbcTemplate)
            .update("""
                INSERT INTO OPPFOLGINGSPERIODE(uuid, aktor_id, startDato, oppdatert, start_begrunnelse, startet_av, startet_av_type) 
                VALUES (:id, :aktor_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :start_begrunnelse, :startet_av, :startet_av_type)
            """.trimIndent(), mapOf(
                "id" to id.toString(),
                "aktor_id" to aktorId.get(),
                "start_begrunnelse" to OppfolgingStartBegrunnelse.ARENA_SYNC_IARBS.name,
                "startet_av" to null,
                "startet_av_type" to null,
            ))

        val perioder = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId)

        assertEquals(perioder.size, 1)
        val periode = perioder[0]
        assertEquals(null, periode.startetAvType)
        assertEquals(null, periode.startetAv)
        assertEquals(aktorId.get(), periode.aktorId)
        assertEquals(OppfolgingStartBegrunnelse.ARENA_SYNC_IARBS, periode.startetBegrunnelse)
        assertEquals(null, periode.begrunnelse) // Avsluttet begrunnelse (fritekst)
        assertEquals(null, periode.avsluttetAv) // Avsluttet veileder
    }

    @Test
    fun `skal returnere riktige felt på en avsluttet periode`() {
        val aktorId = AktorId.of("4321")
        val fnr = Fnr.of("1111119999")
        val veilederIdent = NavIdent("Z999999")
        val oppfolgingsbruker = arbeidssokerRegistrering(fnr, aktorId, VeilederRegistrant(
            NavIdent("Z999999")))
        val avsluttetBegrunnelse = "derfor"
        oppfolgingsStatusRepository.opprettOppfolging(aktorId)
        oppfolgingsPeriodeRepository.start(oppfolgingsbruker)
        oppfolgingsPeriodeRepository.avsluttSistePeriodeOgAvsluttOppfolging(aktorId, veilederIdent.get(), avsluttetBegrunnelse)

        val perioder = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId)

        assertEquals(perioder.size, 1)
        val periode = perioder[0]
        assertEquals(StartetAvType.VEILEDER, periode.startetAvType)
        assertEquals(veilederIdent.get(), periode.startetAv)
        assertEquals(aktorId.get(), periode.aktorId)
        assertEquals(OppfolgingStartBegrunnelse.ARBEIDSSOKER_REGISTRERING, periode.startetBegrunnelse)
        assertEquals(avsluttetBegrunnelse, periode.begrunnelse) // Avsluttet begrunnelse (fritekst)
        assertEquals(veilederIdent.get(), periode.avsluttetAv) // Avsluttet veileder
    }

    @Test
    fun `Skal ikke være mulig å lagre to gjeldende oppfølgingsperioder på samme person`() {
        val aktorId = AktorId.of("4321")
        val fnr = Fnr.of("1111119999")
        val oppfolgingsbruker = arbeidssokerRegistrering(fnr, aktorId, VeilederRegistrant(
            NavIdent("Z999999")))
        oppfolgingsStatusRepository.opprettOppfolging(aktorId)
        oppfolgingsPeriodeRepository.start(oppfolgingsbruker)

        assertThrows<DuplicateKeyException> {
            oppfolgingsPeriodeRepository.start(oppfolgingsbruker)
        }
    }

    @Test
    fun `Kan lagre to åpne oppfølgingsperioder på to forskjellige personer`() {
        val aktorIdBruker1 = AktorId.of("4321")
        val fnrBruker1 = Fnr.of("1111119999")
        val oppfolgingsbruker1 = arbeidssokerRegistrering(fnrBruker1, aktorIdBruker1, VeilederRegistrant(
            NavIdent("Z999999")))
        oppfolgingsStatusRepository.opprettOppfolging(aktorIdBruker1)
        val aktorIdBruker2 = AktorId.of("1234")
        val fnrBruker2 = Fnr.of("2211119999")
        val oppfolgingsbruker2 = arbeidssokerRegistrering(fnrBruker2, aktorIdBruker2, VeilederRegistrant(
            NavIdent("Z999999")))
        oppfolgingsStatusRepository.opprettOppfolging(aktorIdBruker2)

        oppfolgingsPeriodeRepository.start(oppfolgingsbruker1)
        oppfolgingsPeriodeRepository.start(oppfolgingsbruker2)

        assertNotNull(oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(aktorIdBruker1))
        assertNotNull(oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(aktorIdBruker2))
    }
}