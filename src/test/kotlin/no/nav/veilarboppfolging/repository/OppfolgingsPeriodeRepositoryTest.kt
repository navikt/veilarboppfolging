package no.nav.veilarboppfolging.repository

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NavIdent
import no.nav.veilarboppfolging.LocalDatabaseSingleton
import no.nav.veilarboppfolging.domain.StartetAvType
import no.nav.veilarboppfolging.oppfolgingsbruker.BrukerRegistrant
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingStartBegrunnelse
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingsRegistrering
import no.nav.veilarboppfolging.oppfolgingsbruker.VeilederRegistrant
import no.nav.veilarboppfolging.test.DbTestUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID
import kotlin.test.assertEquals

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
            OppfolgingsRegistrering.Companion.arbeidssokerRegistrering(fnr, aktorId, BrukerRegistrant(fnr))
        oppfolgingsStatusRepository.opprettOppfolging(aktorId)

        oppfolgingsPeriodeRepository.start(oppfolgingsbruker)
        oppfolgingsPeriodeRepository.avslutt(aktorId, "veileder", "derfor")
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
            OppfolgingsRegistrering.arbeidssokerRegistrering(fnr, aktorId, BrukerRegistrant(fnr))
        val maybeOppfolgingsperiodeEntity1 = oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(aktorId)
        Assertions.assertTrue(maybeOppfolgingsperiodeEntity1.isEmpty())
        oppfolgingsStatusRepository.opprettOppfolging(aktorId)

        oppfolgingsPeriodeRepository.start(oppfolgingsbruker)
        oppfolgingsPeriodeRepository.avslutt(aktorId, "veileder", "derfor")

        val maybeOppfolgingsperiodeEntity2 = oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(aktorId)
        Assertions.assertTrue(maybeOppfolgingsperiodeEntity2.isEmpty())
    }

    @Test
    fun `skal returnere startetAv og startetAvType`() {
        val aktorId = AktorId.of("4321")
        val fnr = Fnr.of("1111119999")
        val oppfolgingsbruker = OppfolgingsRegistrering.arbeidssokerRegistrering(fnr, aktorId, BrukerRegistrant(fnr))
        oppfolgingsStatusRepository.opprettOppfolging(aktorId)
        oppfolgingsPeriodeRepository.start(oppfolgingsbruker)

        val perioder = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId)

        assertEquals(perioder.size, 1)
        val periode = perioder[0]
        assertEquals(StartetAvType.BRUKER, periode.startetAvType)
        assertEquals(null, periode.startetAv)
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
        val oppfolgingsbruker = OppfolgingsRegistrering.arbeidssokerRegistrering(fnr, aktorId, VeilederRegistrant(
            NavIdent("Z999999")))
        val avsluttetBegrunnelse = "derfor"
        oppfolgingsStatusRepository.opprettOppfolging(aktorId)
        oppfolgingsPeriodeRepository.start(oppfolgingsbruker)
        oppfolgingsPeriodeRepository.avslutt(aktorId, veilederIdent.get(), avsluttetBegrunnelse)

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
}