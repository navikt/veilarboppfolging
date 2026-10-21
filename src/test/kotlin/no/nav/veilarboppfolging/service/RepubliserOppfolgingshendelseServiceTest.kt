package no.nav.veilarboppfolging.service

import no.nav.common.types.identer.NavIdent
import no.nav.veilarboppfolging.config.ApplicationConfig.ADMIN_USER_NAME
import no.nav.veilarboppfolging.config.ApplicationConfig.SYSTEM_USER_NAME
import no.nav.veilarboppfolging.oppfolgingsbruker.AdminRegistrant
import no.nav.veilarboppfolging.oppfolgingsbruker.BrukerRegistrant
import no.nav.veilarboppfolging.oppfolgingsbruker.RegistrantType
import no.nav.veilarboppfolging.oppfolgingsbruker.SystemRegistrant
import no.nav.veilarboppfolging.oppfolgingsbruker.VeilederRegistrant
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.AvregistreringsType
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.ManuellAvregistrering
import no.nav.veilarboppfolging.oppfolgingsperioderHendelser.hendelser.OppfolgingsAvsluttetHendelseDto
import no.nav.veilarboppfolging.repository.ArbeidsoppfolgingskontorRepository
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity
import no.nav.veilarboppfolging.test.TestData.TEST_AKTOR_ID
import no.nav.veilarboppfolging.test.TestData.TEST_FNR
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.ZonedDateTime
import java.util.UUID

/** Verifiserer avslutterinformasjon ved direkte publisering og republisering. */
class RepubliserOppfolgingshendelseServiceTest {
    private val authService = mock<AuthService>()
    private val repository = mock<OppfolgingsPeriodeRepository>()
    private val kafkaProducerService = mock<KafkaProducerService>()
    private val service = RepubliserOppfolgingshendelseService(
        authService, repository, kafkaProducerService, mock<ArbeidsoppfolgingskontorRepository>()
    )
    private val periode = OppfolgingsperiodeEntity(
        uuid = UUID.randomUUID(),
        aktorId = TEST_AKTOR_ID.get(),
        startDato = ZonedDateTime.parse("2026-01-01T12:00:00Z"),
        sluttDato = ZonedDateTime.parse("2026-01-02T12:00:00Z"),
        kvpPerioder = emptyList(),
        avregistreringsType = AvregistreringsType.ManuellAvregistrering,
    )

    @BeforeEach
    fun setup() {
        whenever(authService.getFnrOrThrow(TEST_AKTOR_ID)).thenReturn(TEST_FNR)
    }

    @ParameterizedTest
    @EnumSource(RegistrantType::class)
    fun `republisering bevarer avslutterinformasjonen fra direkte publisering`(type: RegistrantType) {
        val (registrant, forventetIdent) = when (type) {
            RegistrantType.BRUKER -> BrukerRegistrant(TEST_FNR) to "Bruker"
            RegistrantType.VEILEDER -> VeilederRegistrant(NavIdent("Z999999")) to "Z999999"
            RegistrantType.SYSTEM -> SystemRegistrant() to SYSTEM_USER_NAME
            RegistrantType.ADMIN -> AdminRegistrant() to ADMIN_USER_NAME
        }
        val avregistrering = ManuellAvregistrering(TEST_AKTOR_ID, registrant, "")
        val avsluttetPeriode = periode.copy(avsluttetAv = forventetIdent, avsluttetAvType = type)
        val direkteHendelse = OppfolgingsAvsluttetHendelseDto.of(avregistrering, avsluttetPeriode, TEST_FNR)
        whenever(repository.hentOppfolgingsperioder(TEST_AKTOR_ID)).thenReturn(listOf(avsluttetPeriode))

        service.republiserOppfolgingshendelseForBruker(TEST_AKTOR_ID)

        val captor = argumentCaptor<OppfolgingsAvsluttetHendelseDto>()
        verify(kafkaProducerService).publiserOppfolgingsAvsluttet(captor.capture())
        assertThat(direkteHendelse.avsluttetAv).isEqualTo(forventetIdent)
        assertThat(direkteHendelse.avsluttetAvType).isEqualTo(type)
        assertThat(captor.firstValue).usingRecursiveComparison()
            .ignoringFields("producerTimestamp")
            .isEqualTo(direkteHendelse)
    }

    @ParameterizedTest
    @CsvSource(",SYSTEM", "System,SYSTEM", "Z999999,VEILEDER", "z999999,VEILEDER")
    fun `republisering tolker avsluttertype for historiske perioder`(
        avsluttetAv: String?,
        forventetType: RegistrantType,
    ) {
        whenever(repository.hentOppfolgingsperioder(TEST_AKTOR_ID))
            .thenReturn(listOf(periode.copy(avsluttetAv = avsluttetAv)))

        service.republiserOppfolgingshendelseForBruker(TEST_AKTOR_ID)

        val captor = argumentCaptor<OppfolgingsAvsluttetHendelseDto>()
        verify(kafkaProducerService).publiserOppfolgingsAvsluttet(captor.capture())
        assertThat(captor.firstValue.avsluttetAvType).isEqualTo(forventetType)
        assertThat(captor.firstValue.avsluttetAv).isEqualTo(avsluttetAv ?: SYSTEM_USER_NAME)
    }
}
