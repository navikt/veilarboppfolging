package no.nav.veilarboppfolging.service

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.client.veilarbarena.ARENA_REGISTRERING_RESULTAT
import no.nav.veilarboppfolging.client.veilarbarena.ReaktiveringResult
import no.nav.veilarboppfolging.client.veilarbarena.ReaktiveringSuccess
import no.nav.veilarboppfolging.client.veilarbarena.RegistrerIArenaSuccess
import no.nav.veilarboppfolging.client.veilarbarena.RegistrerIkkeArbeidssokerDto
import no.nav.veilarboppfolging.domain.StartetAvType
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingService
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import no.nav.veilarboppfolging.repository.ReaktiveringRepository
import no.nav.veilarboppfolging.repository.entity.OppfolgingEntity
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity
import org.checkerframework.checker.initialization.qual.Initialized
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate
import java.time.ZonedDateTime
import java.util.*
import java.util.function.Consumer

@RunWith(MockitoJUnitRunner::class)
class ReaktiveringServiceTest {
    @Mock
    private lateinit var authService: AuthService

    @Mock
    private lateinit var reaktiveringRepository: ReaktiveringRepository

    @Mock
    private lateinit var oppfolgingsStatusRepository: OppfolgingsStatusRepository

    @Mock
    private lateinit var arenaOppfolgingService: ArenaOppfolgingService

    @Mock
    private lateinit var oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository

    @Mock
    private lateinit var transactor: TransactionTemplate

    @InjectMocks
    private lateinit var reaktiveringService: ReaktiveringService

    @Before
    fun setup() {
        Mockito.`when`(authService.innloggetVeilederIdent).thenReturn(VEILEDER_IDENT)
        Mockito.`when`(authService.getAktorIdOrThrow(FNR)).thenReturn(AKTOR_ID)

        doAnswer { invocation ->
            val callback = invocation.arguments[0] as TransactionCallback<*>
            val dummyStatus = mock(TransactionStatus::class.java)
            callback.doInTransaction(dummyStatus)
        }.`when`(transactor).execute<Any>(any())
    }


    @Test
    fun `skal reaktivere bruker hvis den er inaktivert i Arena og under arbeidsrettet oppfølging`() {

        Mockito.`when`<Optional<OppfolgingEntity>?>(oppfolgingsStatusRepository.hentOppfolging(AKTOR_ID))
            .thenReturn(
                Optional.of<OppfolgingEntity>(OppfolgingEntity().setUnderOppfolging(true))
            )

        Mockito.`when`(arenaOppfolgingService.registrerIkkeArbeidssoker(FNR)).thenReturn(
            RegistrerIArenaSuccess(
                RegistrerIkkeArbeidssokerDto(
                    "Bruker reaktivert i Arena",
                    ARENA_REGISTRERING_RESULTAT.OK_REGISTRERT_I_ARENA
                )
            )
        )

        Mockito.`when`(oppfolgingsPeriodeRepository.hentOppfolgingsperioder(AKTOR_ID)).thenReturn(
            listOf<OppfolgingsperiodeEntity>(mockStartetOppfolgingsperiode(AKTOR_ID))
        )

        val resultat = reaktiveringService.reaktiverBrukerIArena(FNR)

        Assert.assertTrue(resultat is ReaktiveringSuccess)

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
        private val OPPFOLGING_END: ZonedDateTime = OPPFOLGING_START.plusDays(5)
        private val OPPFOLGING_REAKTIVERT: ZonedDateTime = OPPFOLGING_START.plusDays(3)
    }
}