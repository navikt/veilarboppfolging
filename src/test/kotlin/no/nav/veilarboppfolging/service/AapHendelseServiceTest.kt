package no.nav.veilarboppfolging.service

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.veilarboppfolging.client.aap.AapClient
import no.nav.veilarboppfolging.domain.AvslutningStatusData
import no.nav.veilarboppfolging.kafka.AapHendelseRecord
import no.nav.veilarboppfolging.kafka.Hendelse
import no.nav.veilarboppfolging.kandidatForUtmelding.FjernKandidatForUtmeldingService
import no.nav.veilarboppfolging.kandidatForUtmelding.FilterkategoriRepository
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingRepository
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingService
import no.nav.veilarboppfolging.kandidatForUtmelding.hendelser.AvsluttetAapSak
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.AvslutningsBegrunnelse
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity
import no.nav.veilarboppfolging.test.TestData.TEST_AKTOR_ID
import no.nav.veilarboppfolging.test.TestData.TEST_FNR
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import java.time.ZonedDateTime
import java.util.Optional
import java.util.UUID
import java.util.function.Consumer

class AapHendelseServiceTest {
    private val aapClient: AapClient = mock()
    private val avsluttOppfolgingService: AvsluttOppfolgingService = mock()
    private val filterkategoriRepository: FilterkategoriRepository = mock()
    private val transactor: TransactionTemplate = mock()
    private val aktorOppslagClient: AktorOppslagClient = mock()
    private val fjernKandidatForUtmeldingService: FjernKandidatForUtmeldingService = mock()
    private val kandidatForUtmeldingRepository: KandidatForUtmeldingRepository = mock()
    private val oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository = mock()

    private val kandidatForUtmeldingService = spy(
        KandidatForUtmeldingService(
            avsluttOppfolgingService,
            kandidatForUtmeldingRepository,
            filterkategoriRepository,
            fjernKandidatForUtmeldingService,
            oppfolgingsPeriodeRepository,
            aktorOppslagClient,
            transactor,
            mock(),
            mock(),
            mock(),
        )
    )

    private val service = AapHendelseService(
        aapClient,
        kandidatForUtmeldingService,
        aktorOppslagClient,
        fjernKandidatForUtmeldingService,
        kandidatForUtmeldingRepository,
        oppfolgingsPeriodeRepository,
    )

    @BeforeEach
    fun settOppTransaksjoner() {
        doAnswer { invocation ->
            invocation.getArgument<Consumer<TransactionStatus>>(0).accept(mock())
            null
        }.whenever(transactor).executeWithoutResult(any())
    }

    @Test
    fun `ignorerer hendelse når bruker ikke har gjeldende oppfolgingsperiode`() {
        whenever(aktorOppslagClient.hentAktorId(TEST_FNR)).thenReturn(TEST_AKTOR_ID)
        whenever(oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(TEST_AKTOR_ID))
            .thenReturn(Optional.empty())

        håndterHendelse()

        verify(aktorOppslagClient).hentAktorId(TEST_FNR)
        verify(oppfolgingsPeriodeRepository).hentGjeldendeOppfolgingsperiode(TEST_AKTOR_ID)
        verifyNoInteractions(
            aapClient,
            kandidatForUtmeldingService,
            fjernKandidatForUtmeldingService,
            kandidatForUtmeldingRepository,
        )
    }

    @Test
    fun `når bruker ikke er kandidat og hendelsen viser at hen ikke mottar AAP og kan avsluttes skal hen legges til som kandidat og fjernes fra kandidater som ikke kunne avsluttes dersom den ligger der`() {
        val oppfolgingsperiodeUuid = settOppGjeldendeOppfolgingsperiode()
        whenever(aapClient.harAap(TEST_FNR.get())).thenReturn(false)

        håndterHendelse()

        verifiserAvsluttetAapHendelse(oppfolgingsperiodeUuid)
        verify(kandidatForUtmeldingRepository).lagreKandidat(any())
        verify(kandidatForUtmeldingRepository, never()).lagreKandidatSomIkkeKunneAvsluttesOgHendelse(any(), any(), anyOrNull())
        verify(kandidatForUtmeldingRepository).fjernKandidatSomIkkeKunneAvsluttes(oppfolgingsperiodeUuid)
        verifyNoInteractions(fjernKandidatForUtmeldingService)
    }

    @Test
    fun `når bruker er kandidat og endring viser at hen nå mottar AAP skal hen fjernes som kandidat`() {
        val oppfolgingsperiodeUuid = settOppGjeldendeOppfolgingsperiode()
        whenever(aapClient.harAap(TEST_FNR.get())).thenReturn(true)

        håndterHendelse()

        verify(fjernKandidatForUtmeldingService).fjernKandidatForUtmelding(oppfolgingsperiodeUuid)
        verify(kandidatForUtmeldingRepository).fjernKandidatSomIkkeKunneAvsluttes(oppfolgingsperiodeUuid)
        verifyNoInteractions(kandidatForUtmeldingService)
    }

    @Test
    fun `når bruker er kandidat og endring viser at hen ikke mottar AAP skal hendelsen ignoreres`() {
        val oppfolgingsperiodeUuid = settOppGjeldendeOppfolgingsperiode()
        whenever(aapClient.harAap(TEST_FNR.get())).thenReturn(false)

        håndterHendelse()

        verifiserAvsluttetAapHendelse(oppfolgingsperiodeUuid)
        verify(fjernKandidatForUtmeldingService, never()).fjernKandidatForUtmelding(oppfolgingsperiodeUuid)
    }

    @Test
    fun `når bruker ikke er kandidat og endring viser at hen ikke mottar AAP og ikke kan avsluttes skal hendelsen ignoreres`() {
        val oppfolgingsperiodeUuid = settOppGjeldendeOppfolgingsperiode(kanAvslutte = false)
        whenever(aapClient.harAap(TEST_FNR.get())).thenReturn(false)

        håndterHendelse()

        verifiserAvsluttetAapHendelse(oppfolgingsperiodeUuid)
        verify(kandidatForUtmeldingRepository, never()).lagreKandidat(any())
        verify(kandidatForUtmeldingRepository).lagreKandidatSomIkkeKunneAvsluttesOgHendelse(
            any<AvsluttetAapSak>(),
            eq(oppfolgingsperiodeUuid),
            eq(AvslutningsBegrunnelse.BRUKER_VAR_UNDER_KVP),
        )
        verify(fjernKandidatForUtmeldingService, never()).fjernKandidatForUtmelding(oppfolgingsperiodeUuid)
    }

    @Test
    fun `når bruker ikke er kandidat og endring viser at hen mottar AAP skal hendelsen ignoreres`() {
        val oppfolgingsperiodeUuid = settOppGjeldendeOppfolgingsperiode()
        whenever(aapClient.harAap(TEST_FNR.get())).thenReturn(true)

        håndterHendelse()

        verify(fjernKandidatForUtmeldingService).fjernKandidatForUtmelding(oppfolgingsperiodeUuid)
        verify(kandidatForUtmeldingRepository).fjernKandidatSomIkkeKunneAvsluttes(oppfolgingsperiodeUuid)
        verifyNoInteractions(kandidatForUtmeldingService)
    }

    @Test
    fun `når bruker er kandidat som ikke kunne avsluttes og endring viser at hen har AAP skal bruker fjernes fra kandidat som ikke kunne avsluttes`() {
        val oppfolgingsperiodeUuid = settOppGjeldendeOppfolgingsperiode()
        whenever(aapClient.harAap(TEST_FNR.get())).thenReturn(true)

        håndterHendelse()

        verify(kandidatForUtmeldingRepository).fjernKandidatSomIkkeKunneAvsluttes(oppfolgingsperiodeUuid)
        verifyNoInteractions(kandidatForUtmeldingService)
    }

    private fun settOppGjeldendeOppfolgingsperiode(kanAvslutte: Boolean = true): UUID {
        val oppfolgingsperiodeUuid = UUID.randomUUID()
        whenever(avsluttOppfolgingService.hentAvslutningstatusForManuellAvslutning(TEST_FNR))
            .thenReturn(
                AvslutningStatusData(
                    kanAvslutte = kanAvslutte,
                    underOppfolging = true,
                    underKvp = !kanAvslutte,
                    inaktiveringsDato = null,
                    erIserv = false,
                    harAktiveTiltaksdeltakelser = false,
                    erDeltakerIUngdomsprogrammet = false,
                    erArbeidssoeker = false,
                    harAap = false,
                    begrunnelse = if (kanAvslutte) null else AvslutningsBegrunnelse.BRUKER_VAR_UNDER_KVP,
                )
            )
        whenever(filterkategoriRepository.hentEllerOpprettFilterhendelseId(oppfolgingsperiodeUuid))
            .thenReturn(UUID.randomUUID())
        whenever(aktorOppslagClient.hentAktorId(TEST_FNR)).thenReturn(TEST_AKTOR_ID)
        whenever(oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(TEST_AKTOR_ID))
            .thenReturn(
                Optional.of(
                    OppfolgingsperiodeEntity(
                        uuid = oppfolgingsperiodeUuid,
                        aktorId = TEST_AKTOR_ID.get(),
                        startDato = ZonedDateTime.now(),
                        kvpPerioder = emptyList(),
                    )
                )
            )
        return oppfolgingsperiodeUuid
    }

    private fun håndterHendelse() {
        service.håndterAapHendelse(TEST_FNR.get(), AapHendelseRecord(TEST_FNR.get(), Hendelse.VEDTAK))
    }

    private fun verifiserAvsluttetAapHendelse(oppfolgingsperiodeUuid: UUID) {
        val hendelseCaptor = argumentCaptor<AvsluttetAapSak>()
        verify(kandidatForUtmeldingService).handterUtmeldingsHendelse(eq(TEST_FNR), hendelseCaptor.capture())
        assertEquals(oppfolgingsperiodeUuid, hendelseCaptor.firstValue.oppfolgingsperiodeUuid)
        assertEquals("Avsluttet AAP-sak", hendelseCaptor.firstValue.avslutningsarsak)
    }
}
