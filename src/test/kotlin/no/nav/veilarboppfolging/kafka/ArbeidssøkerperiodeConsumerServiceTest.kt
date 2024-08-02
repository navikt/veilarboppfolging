package no.nav.veilarboppfolging.kafka

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.paw.arbeidssokerregisteret.api.v1.*
import no.nav.veilarboppfolging.IntegrationTest
import no.nav.veilarboppfolging.controller.request.Innsatsgruppe
import no.nav.veilarboppfolging.controller.request.SykmeldtBrukerType
import no.nav.veilarboppfolging.domain.Oppfolgingsbruker
import no.nav.veilarboppfolging.repository.entity.OppfolgingStartBegrunnelse
import no.nav.veilarboppfolging.service.AktiverBrukerService
import no.nav.veilarboppfolging.service.OppfolgingService
import no.nav.veilarboppfolging.test.DbTestUtils
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import java.time.*
import java.time.temporal.ChronoUnit
import java.util.*
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata as MetaData


@ActiveProfiles("local")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArbeidssøkerperiodeConsumerServiceTest: IntegrationTest() {

    @Autowired
    private lateinit var arbeidssøkerperiodeConsumerService: ArbeidssøkerperiodeConsumerService

    @Autowired
    private lateinit var oppfølgingService: OppfolgingService

    @Autowired
    private lateinit var aktiverBrukerService: AktiverBrukerService

    private val fnr = "01010198765"
    private val aktørId = AktorId.of("123456789012")

    @BeforeEach
    fun setUp() {
        DbTestUtils.cleanupTestDb()
        `when`(aktorOppslagClient.hentAktorId(Fnr.of(fnr))).thenReturn(aktørId)
        `when`(aktorOppslagClient.hentFnr(aktørId)).thenReturn(Fnr.of(fnr))
    }

    @Test
    fun `Melding om ny arbeidssøkerperiode skal starte ny arbeidsrettet oppfølgingsperiode`() {
        val nyPeriode = arbeidssøkerperiode(fnr)
        val melding = ConsumerRecord("topic", 0, 0, "dummyKey", nyPeriode)

        arbeidssøkerperiodeConsumerService.consumeArbeidssøkerperiode(melding)

        val oppfølgingsperioder = oppfølgingService.hentOppfolgingsperioder(Fnr.of(fnr))
        assertThat(oppfølgingsperioder).hasSize(1)
        val oppfølgingsperiode = oppfølgingsperioder.first()
        assertThat(oppfølgingsperiode.startDato).isEqualToIgnoringNanos(ZonedDateTime.now())
        assertThat(oppfølgingsperiode.sluttDato).isNull()
        assertThat(oppfølgingsperiode.startetBegrunnelse).isEqualTo(OppfolgingStartBegrunnelse.ARBEIDSSOKER_REGISTRERING)
    }

    @Test
    fun `Ny oppfølgingsperiode starter når vi konsumerer meldinga`() {
        val oppfølgingsperiodeStartet = Instant.now().minus(1, ChronoUnit.DAYS)
        val nyPeriode = arbeidssøkerperiode(fnr, periodeStartet = oppfølgingsperiodeStartet)
        val melding = ConsumerRecord("topic", 0, 0, "dummyKey", nyPeriode)

        arbeidssøkerperiodeConsumerService.consumeArbeidssøkerperiode(melding)

        val oppfølgingsperioder = oppfølgingService.hentOppfolgingsperioder(Fnr.of(fnr))
        assertThat(oppfølgingsperioder.first().startDato).isEqualToIgnoringNanos(ZonedDateTime.now())
    }

    @Test
    fun `Skal ikke starte oppfølgingsperiode på arbeidsøkerregistreringer før 1 aug 2024`() {
        val oppfølgingsperiodeStartet = LocalDateTime.of(2024, 7, 31, 0, 0)
            .toInstant(ZoneOffset.UTC)
        val nyPeriode = arbeidssøkerperiode(fnr, periodeStartet = oppfølgingsperiodeStartet)
        val melding = ConsumerRecord("topic", 0, 0, "dummyKey", nyPeriode)

        arbeidssøkerperiodeConsumerService.consumeArbeidssøkerperiode(melding)

        val oppfølgingsperioder = oppfølgingService.hentOppfolgingsperioder(Fnr.of(fnr))
        assertThat(oppfølgingsperioder).isEmpty()
    }

    @Test
    fun `Melding om avsluttet arbeidssøkerperiode skal ignoreres`() {
        val startMelding = ConsumerRecord("topic", 0, 0, "dummyKey", arbeidssøkerperiode(fnr))
        arbeidssøkerperiodeConsumerService.consumeArbeidssøkerperiode(startMelding)
        val sluttMelding = ConsumerRecord("topic", 0, 0, "dummyKey", arbeidssøkerperiode(fnr, periodeAvsluttet = true))

        arbeidssøkerperiodeConsumerService.consumeArbeidssøkerperiode(sluttMelding)

        val oppfølgingsperioder = oppfølgingService.hentOppfolgingsperioder(Fnr.of(fnr))
        assertThat(oppfølgingsperioder).hasSize(1)
        assertThat(oppfølgingService.erUnderOppfolging(Fnr.of(fnr))).isTrue
    }

    @Test
    fun `Dersom arbeidsrettet oppfølgingsperiode allerede eksisterer skal melding om ny arbeidssøkerperiode ikke endre noe`() {
        val oppfølgingsbruker = Oppfolgingsbruker.arbeidssokerOppfolgingsBruker(aktørId, Innsatsgruppe.STANDARD_INNSATS)
        oppfølgingService.startOppfolgingHvisIkkeAlleredeStartet(oppfølgingsbruker)
        val oppfølgingsdataFørMelding = oppfølgingService.hentOppfolgingsperioder(aktørId).first()
        val melding = ConsumerRecord("topic", 0, 0, "dummyKey", arbeidssøkerperiode(fnr))

        arbeidssøkerperiodeConsumerService.consumeArbeidssøkerperiode(melding)

        val oppfølgingsdataEtterMelding = oppfølgingService.hentOppfolgingsperioder(aktørId).first()
        assertThat(oppfølgingsdataEtterMelding).isEqualTo(oppfølgingsdataFørMelding)
    }

    @Test
    fun `Dersom arbeidsrettet oppfølgingsperiode allerede eksisterer for sykmeldt bruker skal melding om arbeidssøkerperiode ikke endre noe`() {
        val oppfølgingsbruker = Oppfolgingsbruker.sykmeldtMerOppfolgingsBruker(aktørId, SykmeldtBrukerType.SKAL_TIL_NY_ARBEIDSGIVER)
        oppfølgingService.startOppfolgingHvisIkkeAlleredeStartet(oppfølgingsbruker)
        val oppfølgingsdataFørMelding = oppfølgingService.hentOppfolgingsperioder(aktørId).first()
        val melding = ConsumerRecord("topic", 0, 0, "dummyKey", arbeidssøkerperiode(fnr))

        arbeidssøkerperiodeConsumerService.consumeArbeidssøkerperiode(melding)

        val oppfølgingsdataEtterMelding = oppfølgingService.hentOppfolgingsperioder(aktørId).first()
        assertThat(oppfølgingsdataEtterMelding).isEqualTo(oppfølgingsdataFørMelding)
    }

    @Test
    fun `Dersom en bruker er under oppfølging pga melding om arbeidssøkerperiode skal senere registrering som sykmeldt ikke få effekt`() {
        val melding = ConsumerRecord("topic", 0, 0, "dummyKey", arbeidssøkerperiode(fnr))
        arbeidssøkerperiodeConsumerService.consumeArbeidssøkerperiode(melding)
        val oppfølgingsdataFørSykmeldtRegistrering = oppfølgingService.hentOppfolgingsperioder(aktørId).first()

        aktiverBrukerService.aktiverSykmeldt(Fnr.of(fnr), SykmeldtBrukerType.SKAL_TIL_NY_ARBEIDSGIVER)

        val oppfølgingsdataEtterSykmeldtRegistrering = oppfølgingService.hentOppfolgingsperioder(aktørId).first()
        assertThat(oppfølgingsdataFørSykmeldtRegistrering).isEqualTo(oppfølgingsdataEtterSykmeldtRegistrering)
    }

    @Test
    fun `Ikke send melding til Arena om brukere som har fått arbeidssøkerperioder`() {
        val melding = ConsumerRecord("topic", 0, 0, "dummyKey", arbeidssøkerperiode(fnr))

        arbeidssøkerperiodeConsumerService.consumeArbeidssøkerperiode(melding)

        verify(behandleArbeidssokerClient, never()).reaktiverBrukerIArena(any())
        verify(behandleArbeidssokerClient, never()).opprettBrukerIArena(any(), any())
    }

    private fun arbeidssøkerperiode(fødselsnummer: String, periodeAvsluttet: Boolean = false, periodeStartet: Instant = Instant.now().minusSeconds(1)): Periode {
        val slutt = if (periodeAvsluttet) {
            MetaData().apply {
                tidspunkt = Instant.now()
                utfoertAv = Bruker(
                    BrukerType.VEILEDER,
                    "dummyId"
                )
                kilde = "dummyKilde"
                aarsak = "dummyAarsak"
                tidspunktFraKilde = TidspunktFraKilde(
                    Instant.now(),
                    AvviksType.FORSINKELSE
                )
            }
        } else { null }

        return Periode().apply {
            id = UUID.randomUUID()
            identitetsnummer = fødselsnummer
            startet = MetaData().apply {
                tidspunkt = periodeStartet
                utfoertAv = Bruker(
                    BrukerType.VEILEDER,
                    "dummyId"
                )
                kilde = "dummyKilde"
                aarsak = "dummyAarsak"
                tidspunktFraKilde = TidspunktFraKilde(
                    periodeStartet,
                    AvviksType.FORSINKELSE
                )
            }
            avsluttet = slutt
        }
    }
}