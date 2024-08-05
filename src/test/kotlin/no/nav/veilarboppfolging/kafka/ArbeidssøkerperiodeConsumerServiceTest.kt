package no.nav.veilarboppfolging.kafka

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.paw.arbeidssokerregisteret.api.v1.*
import no.nav.pto_schema.enums.arena.*
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2
import no.nav.veilarboppfolging.IntegrationTest
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolging
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClient
import no.nav.veilarboppfolging.controller.request.Innsatsgruppe
import no.nav.veilarboppfolging.controller.request.SykmeldtBrukerType
import no.nav.veilarboppfolging.domain.Oppfolgingsbruker
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import no.nav.veilarboppfolging.repository.UtmeldingRepository
import no.nav.veilarboppfolging.repository.entity.OppfolgingStartBegrunnelse
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity
import no.nav.veilarboppfolging.service.AktiverBrukerService
import no.nav.veilarboppfolging.service.KafkaConsumerService
import no.nav.veilarboppfolging.service.OppfolgingService
import no.nav.veilarboppfolging.test.DbTestUtils
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import java.time.*
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.jvm.optionals.getOrNull
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata as MetaData


@ActiveProfiles("local")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArbeidssøkerperiodeConsumerServiceTest: IntegrationTest() {

    @Autowired
    private lateinit var arbeidssøkerperiodeConsumerService: ArbeidssøkerperiodeConsumerService

    @Autowired
    private lateinit var kafkaConsumerService: KafkaConsumerService

    @Autowired
    private lateinit var oppfølgingService: OppfolgingService

    @Autowired
    private lateinit var aktiverBrukerService: AktiverBrukerService

    @Autowired
    private lateinit var oppfolgingsStatusRepository: OppfolgingsStatusRepository

    private val fnr = "01010198765"
    private val aktørId = AktorId.of("123456789012")

    @Autowired
    private lateinit var utmeldingRepository: UtmeldingRepository

    @MockBean
    lateinit var veilarbarenaClient: VeilarbarenaClient


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
        val oppfølgingsperiodeStartet = Instant.now().minus(1, ChronoUnit.HOURS)
        val nyPeriode = arbeidssøkerperiode(fnr, periodeStartet = oppfølgingsperiodeStartet)
        val melding = ConsumerRecord("topic", 0, 0, "dummyKey", nyPeriode)

        arbeidssøkerperiodeConsumerService.consumeArbeidssøkerperiode(melding)

        val oppfølgingsperioder = oppfølgingService.hentOppfolgingsperioder(Fnr.of(fnr))
        assertThat(oppfølgingsperioder.first().startDato).isEqualToIgnoringNanos(ZonedDateTime.now())
    }

    @Test
    fun `Skal ikke starte oppfølgingsperiode på arbeidsøkerregistreringer før 5 aug 2024 kl 10`() {
        val oppfølgingsperiodeStartet = LocalDateTime.of(2024, 8, 5, 7, 59)
            .toInstant(ZoneOffset.UTC)
        val nyPeriode = arbeidssøkerperiode(fnr, periodeStartet = oppfølgingsperiodeStartet)
        val melding = ConsumerRecord("topic", 0, 0, "dummyKey", nyPeriode)

        arbeidssøkerperiodeConsumerService.consumeArbeidssøkerperiode(melding)

        val oppfølgingsperioder = oppfølgingService.hentOppfolgingsperioder(Fnr.of(fnr))
        assertThat(oppfølgingsperioder).isEmpty()
    }

    @Test
    fun `Skal ikke starte ny oppfølgingsperiode hvis vi har lagret periode med startdato etter arbeidssøkerregistrering`() {
        val startAlleredeRegistrertOppfølgingsperiode = ZonedDateTime.now().minusMinutes(30)
        // Denne blir alltid laget via startOppfolgingHvisIkkeAlleredeStartet men ikke i testene siden det opprettes manuelt
        oppfolgingsStatusRepository.opprettOppfolging(aktørId)
        DbTestUtils.lagreOppfølgingsperiode(oppfølgingsperiode(startAlleredeRegistrertOppfølgingsperiode))
        val arbeidssøkerperiodeStartet = startAlleredeRegistrertOppfølgingsperiode.minusMinutes(1)
        val nyPeriode = arbeidssøkerperiode(fnr, periodeStartet = arbeidssøkerperiodeStartet.toInstant())
        val melding = ConsumerRecord("topic", 0, 0, "dummyKey", nyPeriode)

        arbeidssøkerperiodeConsumerService.consumeArbeidssøkerperiode(melding)

        val oppfølgingsperioder = oppfølgingService.hentOppfolgingsperioder(Fnr.of(fnr))
        assertThat(oppfølgingsperioder).hasSize(1)
        assertThat(oppfølgingsperioder.first().startDato).isEqualTo(startAlleredeRegistrertOppfølgingsperiode)
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

    @Test
    fun `Skal putte person i utmelding tabell hvis ISERV i Arena og ISERV_FRA_DATO er etter arbeidssøkerregistreringen`() {
        val arbeidsøkerPeriodeStartet = LocalDateTime.of(2024, 10,1,1,1)
        val ISERV_FRA_DATO = arbeidsøkerPeriodeStartet.plusSeconds(1)
        `when`(veilarbarenaClient.hentOppfolgingsbruker(Fnr.of(fnr))).thenReturn(Optional.of(VeilarbArenaOppfolging()
            .setFodselsnr(fnr)
            .setFormidlingsgruppekode("ISERV")
            .setIserv_fra_dato(ISERV_FRA_DATO.atZone(ZoneId.systemDefault())))
        )
        val nyPeriode = arbeidssøkerperiode(fnr, periodeStartet = arbeidsøkerPeriodeStartet.atZone(ZoneId.systemDefault()).toInstant())
        val oppfolginsBrukerEndretTilISERV = ConsumerRecord("topic", 0, 0, "key", oppfølgingsBrukerEndret(ISERV_FRA_DATO.toLocalDate()))
        val melding = ConsumerRecord("topic", 0, 0, "dummyKey", nyPeriode)

        kafkaConsumerService.consumeEndringPaOppfolgingBruker(oppfolginsBrukerEndretTilISERV)
        arbeidssøkerperiodeConsumerService.consumeArbeidssøkerperiode(melding)

        val oppfølgingsperioder = oppfølgingService.hentOppfolgingsperioder(Fnr.of(fnr))
        assertThat(oppfølgingsperioder).hasSize(1)
        val oppfølgingsperiode = oppfølgingsperioder.first()
        assertThat(oppfølgingsperiode.startDato).isEqualToIgnoringSeconds(ZonedDateTime.now())
        assertThat(oppfølgingsperiode.sluttDato).isNull()
        assertThat(oppfølgingsperiode.startetBegrunnelse).isEqualTo(OppfolgingStartBegrunnelse.ARBEIDSSOKER_REGISTRERING)
        assertThat(utmeldingRepository.eksisterendeIservBruker(aktørId).getOrNull()).isNotNull()
    }

    private fun oppfølgingsperiode(startet: ZonedDateTime = ZonedDateTime.now()) =
        OppfolgingsperiodeEntity(
            UUID.randomUUID(),
            aktørId.toString(),
            "veileder",
            startet,
            null,
            "begrunnelse",
            emptyList(),
            OppfolgingStartBegrunnelse.ARBEIDSSOKER_REGISTRERING
        )

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

    private fun oppfølgingsBrukerEndret(iservFraDato: LocalDate): EndringPaaOppfoelgingsBrukerV2 {
        return EndringPaaOppfoelgingsBrukerV2(
            fnr,
            Formidlingsgruppe.ARBS,
            iservFraDato,
            "Sig",
            ":)",
            "enhet",
            Kvalifiseringsgruppe.IVURD,
            Rettighetsgruppe.INDS,
            Hovedmaal.BEHOLDEA,
            SikkerhetstiltakType.TFUS,
            null,
            true,
            false,
            false,
            null,
            ZonedDateTime.now()
        )
    }
}