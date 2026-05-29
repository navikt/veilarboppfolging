package no.nav.veilarboppfolging.service

import no.nav.veilarboppfolging.client.aap.AapClient
import no.nav.veilarboppfolging.client.arbeidssoekerregisteret.ArbeidssoekerregisteretClient
import no.nav.veilarboppfolging.client.tiltakshistorikk.TiltakshistorikkClient
import no.nav.veilarboppfolging.client.ungdomsprogram.UngdomsprogramClient
import no.nav.veilarboppfolging.eventsLogger.BigQueryClient
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingService
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingsRegistrering
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.ArenaIservKanIkkeReaktiveres
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.KunneAvsluttes
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.KunneIkkeAvsluttes
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import no.nav.veilarboppfolging.repository.entity.OppfolgingEntity
import no.nav.veilarboppfolging.test.TestData.TEST_AKTOR_ID
import no.nav.veilarboppfolging.test.TestData.TEST_FNR
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.transaction.support.TransactionTemplate
import java.util.*

class AvsluttOppfolgingServiceTest {
    private val authService: AuthService = Mockito.mock(AuthService::class.java)
    private val oppfolgingService: OppfolgingService = Mockito.mock(OppfolgingService::class.java)
    private val startOppfolgingService: StartOppfolgingService = Mockito.mock(StartOppfolgingService::class.java)
    private val arenaOppfolgingService: ArenaOppfolgingService = Mockito.mock(ArenaOppfolgingService::class.java)
    private val kvpService: KvpService = Mockito.mock(KvpService::class.java)
    private val oppfolgingsStatusRepository: OppfolgingsStatusRepository = Mockito.mock(OppfolgingsStatusRepository::class.java)
    private val oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository = Mockito.mock(OppfolgingsPeriodeRepository::class.java)
    private val kafkaProducerService: KafkaProducerService = Mockito.mock(KafkaProducerService::class.java)
    private val tiltakshistorikkClient: TiltakshistorikkClient = Mockito.mock(TiltakshistorikkClient::class.java)
    private val ungdomsprogramClient: UngdomsprogramClient = Mockito.mock(UngdomsprogramClient::class.java)
    private val aapClient: AapClient = Mockito.mock(AapClient::class.java)
    private val arbeidssokerRegisterClient: ArbeidssoekerregisteretClient = Mockito.mock(ArbeidssoekerregisteretClient::class.java)
    private val arenaYtelserService: ArenaYtelserService = Mockito.mock(ArenaYtelserService::class.java)
    private val bigQueryClient: BigQueryClient = Mockito.mock(BigQueryClient::class.java)
    private val transactionTemplate: TransactionTemplate = Mockito.mock(TransactionTemplate::class.java)

    private fun <T> any(type: Class<T>): T = Mockito.any<T>(type)

    private val avsluttOppfolgingService: AvsluttOppfolgingService = AvsluttOppfolgingService(
        authService, oppfolgingsStatusRepository, oppfolgingsPeriodeRepository,
        arenaOppfolgingService, kafkaProducerService, kvpService, tiltakshistorikkClient,
        ungdomsprogramClient,
        aapClient = aapClient,
        arbeidssoekerregisteretClient = arbeidssokerRegisterClient,
        arenaYtelserService = arenaYtelserService,
        bigQueryClient = bigQueryClient,
        transactor = transactionTemplate,
    )

    private fun arenaIservAvregistrering(): ArenaIservKanIkkeReaktiveres {
        return ArenaIservKanIkkeReaktiveres(aktorId = TEST_AKTOR_ID)
    }

    private fun brukerErUnderOppfolgingLokalt() {
        `when`(oppfolgingsStatusRepository.hentOppfolging(TEST_AKTOR_ID))
            .thenReturn(Optional.of(OppfolgingEntity()
                .setLocalArenaOppfolging(Optional.empty())
                .setUnderOppfolging(true)))
    }
    private fun kanReaktiveres() {
        `when`(arenaOppfolgingService.kanEnkeltReaktiveres(TEST_FNR)).thenReturn(
            Optional.of<Boolean>(true)
        )
    }
    private fun kanIkkeReaktiveres() {
        `when`(arenaOppfolgingService.kanEnkeltReaktiveres(TEST_FNR)).thenReturn(
            Optional.of<Boolean>(false)
        )
    }

    @BeforeEach
    fun beforeEach() {
        `when`(authService.getAktorIdOrThrow(TEST_FNR)).thenReturn(TEST_AKTOR_ID)
        `when`(authService.getFnrOrThrow(TEST_AKTOR_ID)).thenReturn(TEST_FNR)
    }

    @Test
    fun `skal avslutte oppfolging pa bruker som er under oppfolging i veilarboppfolging men er ISERV i arena og ikke kan reaktiveres`() {
        brukerErUnderOppfolgingLokalt()
        kanIkkeReaktiveres()
        `when`(kvpService.erUnderKvp(TEST_AKTOR_ID)).thenReturn(false)

        val brukverV2 = arenaIservAvregistrering()

        val result = avsluttOppfolgingService.avsluttOppfolgingHvisKanAvsluttes(brukverV2)

        verify(startOppfolgingService, never()).startOppfolgingHvisIkkeAlleredeStartet(any())
        assertInstanceOf<KunneAvsluttes>(result)
    }

    @Test
    fun `skal ikke avslutte oppfolging pa bruker som er under kvp`() {
        brukerErUnderOppfolgingLokalt()
        kanIkkeReaktiveres()
        `when`(kvpService.erUnderKvp(TEST_AKTOR_ID)).thenReturn(true)
        `when`(tiltakshistorikkClient.harAktiveTiltaksdeltakelser(TEST_FNR.get())).thenReturn(false)
        `when`(ungdomsprogramClient.erDeltakerIUngdomsprogrammet(TEST_FNR.get())).thenReturn(false)
        `when`(arbeidssokerRegisterClient.erArbeidssoeker(TEST_FNR.get())).thenReturn(false)
        `when`(aapClient.harAap(TEST_FNR.get())).thenReturn(false)

        val brukverV2 = arenaIservAvregistrering()

        val result = avsluttOppfolgingService.avsluttOppfolgingHvisKanAvsluttes(brukverV2)

        verify(startOppfolgingService, never()).startOppfolgingHvisIkkeAlleredeStartet(
            any(OppfolgingsRegistrering::class.java)
        )
        assertInstanceOf<KunneIkkeAvsluttes>(result)
    }

    @Test
    fun `skal ikke avslutte oppfolging pa bruker som har aktive tiltaksdeltakelser`() {
        brukerErUnderOppfolgingLokalt()
        kanIkkeReaktiveres()
        `when`(kvpService.erUnderKvp(TEST_AKTOR_ID)).thenReturn(false)
        `when`(tiltakshistorikkClient.harAktiveTiltaksdeltakelser(TEST_FNR.get())).thenReturn(true)
        `when`(ungdomsprogramClient.erDeltakerIUngdomsprogrammet(TEST_FNR.get())).thenReturn(false)
        `when`(arbeidssokerRegisterClient.erArbeidssoeker(TEST_FNR.get())).thenReturn(false)
        `when`(aapClient.harAap(TEST_FNR.get())).thenReturn(false)

        val brukverV2 = arenaIservAvregistrering()

        val result = avsluttOppfolgingService.avsluttOppfolgingHvisKanAvsluttes(brukverV2)

        verify(startOppfolgingService, never()).startOppfolgingHvisIkkeAlleredeStartet(any())
        assertInstanceOf<KunneIkkeAvsluttes>(result)
    }

    @Test
    fun `skal ikke avslutte oppfolging pa bruker som er deltaker i ungdomsprogrammet`() {
        brukerErUnderOppfolgingLokalt()
        kanIkkeReaktiveres()
        `when`(kvpService.erUnderKvp(TEST_AKTOR_ID)).thenReturn(false)
        `when`(tiltakshistorikkClient.harAktiveTiltaksdeltakelser(TEST_FNR.get())).thenReturn(false)
        `when`(ungdomsprogramClient.erDeltakerIUngdomsprogrammet(TEST_FNR.get())).thenReturn(true)
        `when`(arbeidssokerRegisterClient.erArbeidssoeker(TEST_FNR.get())).thenReturn(false)
        `when`(aapClient.harAap(TEST_FNR.get())).thenReturn(false)
        val brukverV2 = arenaIservAvregistrering()

        val result = avsluttOppfolgingService.avsluttOppfolgingHvisKanAvsluttes(brukverV2)

        assertInstanceOf<KunneIkkeAvsluttes>(result)
    }

    @Test
    fun `skal ikke avslutte oppfolging pa bruker som er arbeidssoeker`() {
        brukerErUnderOppfolgingLokalt()
        `when`(kvpService.erUnderKvp(TEST_AKTOR_ID)).thenReturn(false)
        `when`(tiltakshistorikkClient.harAktiveTiltaksdeltakelser(TEST_FNR.get())).thenReturn(false)
        `when`(ungdomsprogramClient.erDeltakerIUngdomsprogrammet(TEST_FNR.get())).thenReturn(false)
        `when`(arbeidssokerRegisterClient.erArbeidssoeker(TEST_FNR.get())).thenReturn(true)

        val brukverV2 = arenaIservAvregistrering()

        val result = avsluttOppfolgingService.avsluttOppfolgingHvisKanAvsluttes(brukverV2)

        verify(startOppfolgingService, never()).startOppfolgingHvisIkkeAlleredeStartet(any())
        assertInstanceOf<KunneIkkeAvsluttes>(result)
    }
}