package no.nav.veilarboppfolging.kafka

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.paw.arbeidssokerregisteret.api.v1.*
import no.nav.veilarboppfolging.IntegrationTest
import no.nav.veilarboppfolging.controller.request.Innsatsgruppe
import no.nav.veilarboppfolging.controller.request.SykmeldtBrukerType
import no.nav.veilarboppfolging.domain.Oppfolgingsbruker
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
import java.time.Instant
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

    @BeforeEach
    fun setUp() {
        DbTestUtils.cleanupTestDb()
    }

    @Test
    fun `Melding om ny arbeidssøkerperiode skal starte ny arbeidsrettet oppfølgingsperiode`() {
        val aktørId = AktorId.of("123456789012")
        val fødselsnummer = "01010198765"
        `when`(aktorOppslagClient.hentAktorId(Fnr.of(fødselsnummer))).thenReturn(aktørId)
        val melding = ConsumerRecord("topic", 0, 0, "dummyKey", periode(fødselsnummer))

        arbeidssøkerperiodeConsumerService.consumeArbeidssøkerperiode(melding)

        val oppfølgingsperioder = oppfølgingService.hentOppfolgingsperioder(Fnr.of(fødselsnummer))
        assertThat(oppfølgingsperioder).hasSize(1)
        // TODO: Assert innhold
    }

    @Test
    fun `Melding om avsluttet arbeidssøkerperiode skal ignoreres`() {
        val aktørId = AktorId.of("123456789012")
        val fødselsnummer = "01010198765"
        `when`(aktorOppslagClient.hentAktorId(Fnr.of(fødselsnummer))).thenReturn(aktørId)
        val startMelding = ConsumerRecord("topic", 0, 0, "dummyKey", periode(fødselsnummer))
        arbeidssøkerperiodeConsumerService.consumeArbeidssøkerperiode(startMelding)
        val sluttMelding = ConsumerRecord("topic", 0, 0, "dummyKey", periode(fødselsnummer, periodeAvsluttet = true))

        arbeidssøkerperiodeConsumerService.consumeArbeidssøkerperiode(sluttMelding)

        val oppfølgingsperioder = oppfølgingService.hentOppfolgingsperioder(Fnr.of(fødselsnummer))
        assertThat(oppfølgingsperioder).hasSize(1)
        assertThat(oppfølgingService.erUnderOppfolging(Fnr.of(fødselsnummer))).isTrue
    }

    @Test
    fun `Dersom arbeidsrettet oppfølgingsperiode allerede eksisterer skal melding om ny arbeidssøkerperiode ikke endre noe`() {
        val aktørId = AktorId.of("123456789012")
        val fødselsnummer = "01010198765"
        `when`(aktorOppslagClient.hentAktorId(Fnr.of(fødselsnummer))).thenReturn(aktørId)
        val oppfølgingsbruker = Oppfolgingsbruker.arbeidssokerOppfolgingsBruker(aktørId, Innsatsgruppe.STANDARD_INNSATS)
        oppfølgingService.startOppfolgingHvisIkkeAlleredeStartet(oppfølgingsbruker, Fnr.of(fødselsnummer))
        val oppfølgingsdataFørMelding = oppfølgingService.hentOppfolgingsperioder(aktørId).first()
        val melding = ConsumerRecord("topic", 0, 0, "dummyKey", periode(fødselsnummer))

        arbeidssøkerperiodeConsumerService.consumeArbeidssøkerperiode(melding)

        val oppfølgingsdataEtterMelding = oppfølgingService.hentOppfolgingsperioder(aktørId).first()
        assertThat(oppfølgingsdataEtterMelding).isEqualTo(oppfølgingsdataFørMelding)
    }

    @Test
    fun `Dersom arbeidsrettet oppfølgingsperiode allerede eksisterer for sykmeldt bruker skal melding om arbeidssøkerperiode ikke endre noe`() {
        val aktørId = AktorId.of("123456789012")
        val fødselsnummer = "01010198765"
        `when`(aktorOppslagClient.hentAktorId(Fnr.of(fødselsnummer))).thenReturn(aktørId)
        val oppfølgingsbruker = Oppfolgingsbruker.sykmeldtMerOppfolgingsBruker(aktørId, SykmeldtBrukerType.SKAL_TIL_NY_ARBEIDSGIVER)
        oppfølgingService.startOppfolgingHvisIkkeAlleredeStartet(oppfølgingsbruker, Fnr.of(fødselsnummer))
        val oppfølgingsdataFørMelding = oppfølgingService.hentOppfolgingsperioder(aktørId).first()
        val melding = ConsumerRecord("topic", 0, 0, "dummyKey", periode(fødselsnummer))

        arbeidssøkerperiodeConsumerService.consumeArbeidssøkerperiode(melding)

        val oppfølgingsdataEtterMelding = oppfølgingService.hentOppfolgingsperioder(aktørId).first()
        assertThat(oppfølgingsdataEtterMelding).isEqualTo(oppfølgingsdataFørMelding)
    }

    @Test
    fun `Dersom en bruker er under oppfølging pga melding om arbeidssøkerperiode skal senere registrering som sykmeldt ikke få effekt`() {
        val aktørId = AktorId.of("123456789012")
        val fødselsnummer = "01010198765"
        `when`(aktorOppslagClient.hentAktorId(Fnr.of(fødselsnummer))).thenReturn(aktørId)
        val melding = ConsumerRecord("topic", 0, 0, "dummyKey", periode(fødselsnummer))
        arbeidssøkerperiodeConsumerService.consumeArbeidssøkerperiode(melding)
        val oppfølgingsdataFørSykmeldtRegistrering = oppfølgingService.hentOppfolgingsperioder(aktørId).first()

        aktiverBrukerService.aktiverSykmeldt(Fnr.of(fødselsnummer), SykmeldtBrukerType.SKAL_TIL_NY_ARBEIDSGIVER)

        val oppfølgingsdataEtterSykmeldtRegistrering = oppfølgingService.hentOppfolgingsperioder(aktørId).first()
        assertThat(oppfølgingsdataFørSykmeldtRegistrering).isEqualTo(oppfølgingsdataEtterSykmeldtRegistrering)
    }

    @Test
    fun `Ikke send melding til Arena om brukere som har fått arbeidssøkerperioder`() {
        val aktørId = AktorId.of("123456789012")
        val fødselsnummer = "01010198765"
        `when`(aktorOppslagClient.hentAktorId(Fnr.of(fødselsnummer))).thenReturn(aktørId)
        val melding = ConsumerRecord("topic", 0, 0, "dummyKey", periode(fødselsnummer))

        arbeidssøkerperiodeConsumerService.consumeArbeidssøkerperiode(melding)

        verify(behandleArbeidssokerClient, never()).reaktiverBrukerIArena(any())
        verify(behandleArbeidssokerClient, never()).opprettBrukerIArena(any(), any())
    }

    private fun periode(fødselsnummer: String, periodeAvsluttet: Boolean = false): Periode {
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
                tidspunkt = Instant.now().minusSeconds(1)
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
            avsluttet = slutt
        }
    }
}