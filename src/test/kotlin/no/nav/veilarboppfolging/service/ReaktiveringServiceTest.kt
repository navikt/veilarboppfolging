package no.nav.veilarboppfolging.service

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.LocalDatabaseSingleton
import no.nav.veilarboppfolging.client.veilarbarena.*
import no.nav.veilarboppfolging.domain.StartetAvType
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingService
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import no.nav.veilarboppfolging.repository.ReaktiveringRepository
import no.nav.veilarboppfolging.repository.entity.OppfolgingEntity
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.server.ResponseStatusException
import java.time.ZonedDateTime
import java.util.*

@RunWith(MockitoJUnitRunner::class)
class ReaktiveringServiceTest {
    private var authService = mock(AuthService::class.java)
    private val reaktiveringRepository = ReaktiveringRepository(NamedParameterJdbcTemplate(LocalDatabaseSingleton.jdbcTemplate))
    private var oppfolgingsStatusRepository = mock(OppfolgingsStatusRepository::class.java)
    private var arenaOppfolgingService = mock(ArenaOppfolgingService::class.java)
    private var oppfolgingsPeriodeRepository = mock(OppfolgingsPeriodeRepository::class.java)
    private var transactor = mock(TransactionTemplate::class.java)
    private val reaktiveringService = ReaktiveringService(
        authService,
        oppfolgingsStatusRepository,
        arenaOppfolgingService,
        reaktiveringRepository,
        oppfolgingsPeriodeRepository,
        transactor
    )

    @Before
    fun setup() {
        Mockito.`when`(authService.innloggetVeilederIdent).thenReturn(VEILEDER_IDENT)

        doAnswer { invocation ->
            val callback = invocation.arguments[0] as TransactionCallback<*>
            val dummyStatus = mock(TransactionStatus::class.java)
            callback.doInTransaction(dummyStatus)
        }.`when`(transactor).execute<Any>(any())
    }

    @Test
    fun `skal reaktivere bruker hvis den er inaktivert i Arena og under arbeidsrettet oppfølging`() {
        val FNR = Fnr.of("123")
        val AKTOR_ID = AktorId.of("123")
        Mockito.`when`(authService.getAktorIdOrThrow(FNR)).thenReturn(AKTOR_ID)
        Mockito.`when`<Optional<OppfolgingEntity>?>(oppfolgingsStatusRepository.hentOppfolging(AKTOR_ID))
            .thenReturn(
                Optional.of<OppfolgingEntity>(OppfolgingEntity().setUnderOppfolging(true))
            )

        Mockito.`when`(arenaOppfolgingService.registrerIkkeArbeidssoker(FNR)).thenReturn(
            RegistrerIArenaSuccess(
                RegistrerIkkeArbeidssokerDto(
                    "Bruker reaktivert i Arena",
                    ArenaRegistreringResultat.OK_REGISTRERT_I_ARENA
                )
            )
        )

        Mockito.`when`(oppfolgingsPeriodeRepository.hentOppfolgingsperioder(AKTOR_ID)).thenReturn(
            listOf<OppfolgingsperiodeEntity>(mockStartetOppfolgingsperiode(AKTOR_ID))
        )

        val resultat = reaktiveringService.reaktiverBrukerIArena(FNR)

        Assert.assertTrue(resultat is ReaktiveringSuccess)
        Assert.assertTrue((resultat as ReaktiveringSuccess).reaktiveringResponse.kode == ReaktiveringResultat.OK_REGISTRERT_I_ARENA)
        Assert.assertTrue((resultat).reaktiveringResponse.ok == true)

        val reaktiveringHistorikk = reaktiveringRepository.hentReaktiveringer(AKTOR_ID)
        Assert.assertTrue(reaktiveringHistorikk.size == 1)
    }

    @Test
    fun `skal gi feil hvis bruker ikke er under oppfølging`() {
        val FNR = Fnr.of("321")
        val AKTOR_ID = AktorId.of("321")
        Mockito.`when`(authService.getAktorIdOrThrow(FNR)).thenReturn(AKTOR_ID)
        Mockito.`when`<Optional<OppfolgingEntity>?>(oppfolgingsStatusRepository.hentOppfolging(AKTOR_ID))
            .thenReturn(
                Optional.of<OppfolgingEntity>(OppfolgingEntity().setUnderOppfolging(false))
            )

        val resultat = reaktiveringService.reaktiverBrukerIArena(FNR)

        Assert.assertTrue(resultat is ReaktiveringSuccess)
        Assert.assertTrue((resultat as ReaktiveringSuccess).reaktiveringResponse.kode == ReaktiveringResultat.KAN_IKKE_REAKTIVERES)
        Assert.assertTrue((resultat).reaktiveringResponse.ok == false)

        val reaktiveringHistorikk = reaktiveringRepository.hentReaktiveringer(AKTOR_ID)
        Assert.assertTrue(reaktiveringHistorikk.isEmpty())
    }

    @Test
    fun `skal gi error hvis arena kall feiler`() {
        val FNR = Fnr.of("111")
        val AKTOR_ID = AktorId.of("111")
        Mockito.`when`(authService.getAktorIdOrThrow(FNR)).thenReturn(AKTOR_ID)
        Mockito.`when`<Optional<OppfolgingEntity>?>(oppfolgingsStatusRepository.hentOppfolging(AKTOR_ID))
            .thenReturn(
                Optional.of<OppfolgingEntity>(OppfolgingEntity().setUnderOppfolging(true))
            )

        Mockito.`when`(arenaOppfolgingService.registrerIkkeArbeidssoker(FNR)).thenReturn(
            RegistrerIArenaError(
                "Feil ved registrering av bruker i Arena",
                RuntimeException("Feil ved registrering av bruker i Arena")
            )
        )

        assertThrows<ResponseStatusException> { reaktiveringService.reaktiverBrukerIArena(FNR) }

        val reaktiveringHistorikk = reaktiveringRepository.hentReaktiveringer(AKTOR_ID)
        Assert.assertTrue(reaktiveringHistorikk.isEmpty())
    }

    private fun mockStartetOppfolgingsperiode(aktorId: AktorId): OppfolgingsperiodeEntity {
        return OppfolgingsperiodeEntity(
            UUID.randomUUID(),
            AKTOR_ID.get(),
            null,
            OPPFOLGING_START,
            null,
            null,
            emptyList(),
            null,
            "defaultVeileder",
            StartetAvType.VEILEDER
        )
    }

    companion object {
        private val FNR: Fnr = Fnr.of("fnr")
        private val AKTOR_ID: AktorId = AktorId.of("aktorId")
        private val VEILEDER_IDENT: String = "Z123456"
        private val OPPFOLGING_START: ZonedDateTime = ZonedDateTime.now().minusDays(10)
    }
}