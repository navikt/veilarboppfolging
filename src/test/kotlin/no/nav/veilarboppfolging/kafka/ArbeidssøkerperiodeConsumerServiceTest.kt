package no.nav.veilarboppfolging.kafka

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.common.json.JsonUtils
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NavIdent
import no.nav.paw.arbeidssokerregisteret.api.v1.*
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2
import no.nav.veilarboppfolging.IntegrationTest
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolgingsBruker
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClient
import no.nav.veilarboppfolging.config.KafkaProperties
import no.nav.veilarboppfolging.domain.StartetAvType
import no.nav.veilarboppfolging.oppfolgingsbruker.BrukerRegistrant
import no.nav.veilarboppfolging.oppfolgingsbruker.VeilederRegistrant
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingStartBegrunnelse
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingsRegistrering
import no.nav.veilarboppfolging.oppfolgingsperioderHendelser.hendelser.OppfolgingStartetHendelseDto
import no.nav.veilarboppfolging.repository.UtmeldingRepository
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity
import no.nav.veilarboppfolging.service.KafkaConsumerService
import no.nav.veilarboppfolging.service.OppfolgingService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.ActiveProfiles
import java.sql.Timestamp
import java.time.*
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.collections.mapOf
import kotlin.jvm.optionals.getOrNull
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata as MetaData


@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArbeidssøkerperiodeConsumerServiceTest(
    @Autowired
    val arbeidssøkerperiodeConsumerService: ArbeidssøkerperiodeConsumerService,
    @Autowired
    val kafkaConsumerService: KafkaConsumerService,
    @Autowired
    val oppfølgingService: OppfolgingService,
    @Autowired
    val utmeldingRepository: UtmeldingRepository,
    @Autowired
    val veilarbarenaClient: VeilarbarenaClient,
    @Autowired
    val template: NamedParameterJdbcTemplate,
    @Autowired
    val kafkaProperties: KafkaProperties
): IntegrationTest() {

    private val objectMapper = JsonUtils.getMapper().also {
        it.registerKotlinModule()
    }

    private fun getSavedRecord(topic: String, fnr: String): List<OppfolgingStartetHendelseDto> {
        return template.query("""
            SELECT * FROM kafka_producer_record
            where topic = :topic and key = :fnr
        """.trimIndent(), mapOf("topic" to topic, "fnr" to fnr.toByteArray())) { resultSet, row ->
             val json = resultSet.getBytes("value").toString(Charsets.UTF_8)
             objectMapper.readValue(json, OppfolgingStartetHendelseDto::class.java)
        }
    }

    private val fnr = "01010198765"
    private val aktørId = AktorId.of("123456789012")

    @BeforeEach
    fun setUp() {
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
        assertThat(oppfølgingsperiode.startDato).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.SECONDS))
        assertThat(oppfølgingsperiode.sluttDato).isNull()
        assertThat(oppfølgingsperiode.startetBegrunnelse).isEqualTo(OppfolgingStartBegrunnelse.ARBEIDSSOKER_REGISTRERING)

        val lagreteMeldingerIUtboks = getSavedRecord(kafkaProperties.oppfolgingsperiodehendelseV1, fnr)
        assertThat(lagreteMeldingerIUtboks).hasSize(1)
        assertThat(lagreteMeldingerIUtboks.first().fnr).isEqualTo(fnr)
        assertThat(lagreteMeldingerIUtboks.first().startetBegrunnelse).isEqualTo("ARBEIDSSOKER_REGISTRERING")
        assertThat(lagreteMeldingerIUtboks.first().arenaKontor).isNull()
        assertThat(lagreteMeldingerIUtboks.first().startetAvType).isEqualTo(nyPeriode.startet.utfoertAv.type.name)
        assertThat(lagreteMeldingerIUtboks.first().startetAv).isEqualTo(nyPeriode.startet.utfoertAv.id)
        assertThat(lagreteMeldingerIUtboks.first().startetTidspunkt).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.SECONDS))
        assertThat(lagreteMeldingerIUtboks.first().arbeidsoppfolgingsKontorSattAvVeileder).isNull()
        assertThat(lagreteMeldingerIUtboks.first().oppfolgingsPeriodeId).isEqualTo(oppfølgingsperiode.uuid)
    }

    @Test
    fun `Ny oppfølgingsperiode starter når vi konsumerer meldinga`() {
        val oppfølgingsperiodeStartet = Instant.now().minus(1, ChronoUnit.HOURS)
        val nyPeriode = arbeidssøkerperiode(fnr, periodeStartet = oppfølgingsperiodeStartet)
        val melding = ConsumerRecord("topic", 0, 0, "dummyKey", nyPeriode)

        arbeidssøkerperiodeConsumerService.consumeArbeidssøkerperiode(melding)

        val oppfølgingsperioder = oppfølgingService.hentOppfolgingsperioder(Fnr.of(fnr))
        assertThat(oppfølgingsperioder.first().startDato).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.SECONDS))
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
        lagreOppfølgingsperiode(oppfølgingsperiode(startAlleredeRegistrertOppfølgingsperiode))
        val arbeidssøkerperiodeStartet = startAlleredeRegistrertOppfølgingsperiode.minusMinutes(1)
        val nyPeriode = arbeidssøkerperiode(fnr, periodeStartet = arbeidssøkerperiodeStartet.toInstant())
        val melding = ConsumerRecord("topic", 0, 0, "dummyKey", nyPeriode)

        arbeidssøkerperiodeConsumerService.consumeArbeidssøkerperiode(melding)

        val oppfølgingsperioder = oppfølgingService.hentOppfolgingsperioder(Fnr.of(fnr))
        assertThat(oppfølgingsperioder).hasSize(1)
        assertThat(oppfølgingsperioder.first().startDato).isCloseTo(startAlleredeRegistrertOppfølgingsperiode, within(1, ChronoUnit.SECONDS))
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
        val oppfølgingsbruker = OppfolgingsRegistrering.arbeidssokerRegistrering(Fnr.of(fnr), aktørId, BrukerRegistrant(
            Fnr.of(fnr)))
        startOppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(oppfølgingsbruker)
        val oppfølgingsdataFørMelding = oppfølgingService.hentOppfolgingsperioder(aktørId).first()
        val melding = ConsumerRecord("topic", 0, 0, "dummyKey", arbeidssøkerperiode(fnr))

        arbeidssøkerperiodeConsumerService.consumeArbeidssøkerperiode(melding)

        val oppfølgingsdataEtterMelding = oppfølgingService.hentOppfolgingsperioder(aktørId).first()
        assertThat(oppfølgingsdataEtterMelding).isEqualTo(oppfølgingsdataFørMelding)
    }

    @Test
    fun `Dersom arbeidsrettet oppfølgingsperiode allerede eksisterer for sykmeldt bruker skal melding om arbeidssøkerperiode ikke endre noe`() {
        val oppfølgingsbruker = OppfolgingsRegistrering.manueltRegistrertBruker(Fnr.of(fnr), aktørId, VeilederRegistrant(NavIdent.of("G123123")), "1234")
        startOppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(oppfølgingsbruker)
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

        `when`(authContextHolder.erInternBruker()).thenReturn(true)
        `when`(authContextHolder.getUid()).thenReturn(Optional.of("G123123"))
        aktiverBrukerManueltService.aktiverBrukerManuelt(Fnr.of(fnr), "1234")

        val oppfølgingsdataEtterSykmeldtRegistrering = oppfølgingService.hentOppfolgingsperioder(aktørId).first()
        assertThat(oppfølgingsdataFørSykmeldtRegistrering).isEqualTo(oppfølgingsdataEtterSykmeldtRegistrering)
    }

    @Test
    fun `Skal putte person i utmelding tabell hvis ISERV i Arena og ISERV_FRA_DATO er etter arbeidssøkerregistreringen`() {
        val arbeidsøkerPeriodeStartet = LocalDateTime.of(2024, 10,1,23,59)
        val ISERV_FRA_DATO = LocalDate.of(2024, 10, 2)
        `when`(veilarbarenaClient.hentOppfolgingsbruker(Fnr.of(fnr))).thenReturn(Optional.of(
            VeilarbArenaOppfolgingsBruker()
            .setFodselsnr(fnr)
            .setFormidlingsgruppekode("ISERV")
            .setIserv_fra_dato(ISERV_FRA_DATO.atStartOfDay(ZoneId.systemDefault())))
        )
        val nyPeriode = arbeidssøkerperiode(fnr, periodeStartet = arbeidsøkerPeriodeStartet.atZone(ZoneId.systemDefault()).toInstant())
        val oppfolginsBrukerEndretTilISERV = ConsumerRecord("topic", 0, 0, "key", oppfølgingsBrukerEndret(
            ISERV_FRA_DATO, formidlingsgruppe = Formidlingsgruppe.ISERV))
        val melding = ConsumerRecord("topic", 0, 0, "dummyKey", nyPeriode)

        kafkaConsumerService.consumeEndringPaOppfolgingBruker(oppfolginsBrukerEndretTilISERV)
        arbeidssøkerperiodeConsumerService.consumeArbeidssøkerperiode(melding)

        val oppfølgingsperioder = oppfølgingService.hentOppfolgingsperioder(Fnr.of(fnr))
        assertThat(oppfølgingsperioder).hasSize(1)
        val oppfølgingsperiode = oppfølgingsperioder.first()
        assertThat(oppfølgingsperiode.startDato).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.SECONDS))
        assertThat(oppfølgingsperiode.sluttDato).isNull()
        assertThat(oppfølgingsperiode.startetBegrunnelse).isEqualTo(OppfolgingStartBegrunnelse.ARBEIDSSOKER_REGISTRERING)
        assertThat(utmeldingRepository.eksisterendeIservBruker(aktørId).getOrNull()).isNotNull()
    }

    @Test
    fun `Skal ikke putte person i utmelding tabell hvis ISERV i Arena og ISERV_FRA_DATO er før arbeidssøkerregistreringen`() {
        val arbeidsøkerPeriodeStartet = LocalDateTime.of(2024, 10,1,1,1)
        val ISERV_FRA_DATO = arbeidsøkerPeriodeStartet // Samme tidspunkt
        `when`(veilarbarenaClient.hentOppfolgingsbruker(Fnr.of(fnr))).thenReturn(Optional.of(
            VeilarbArenaOppfolgingsBruker()
            .setFodselsnr(fnr)
            .setFormidlingsgruppekode("ISERV")
            .setIserv_fra_dato(ISERV_FRA_DATO.atZone(ZoneId.systemDefault())))
        )
        val nyPeriode = arbeidssøkerperiode(fnr, periodeStartet = arbeidsøkerPeriodeStartet.atZone(ZoneId.systemDefault()).toInstant())
        val oppfolginsBrukerEndretTilISERV = ConsumerRecord("topic", 0, 0, "key", oppfølgingsBrukerEndret(
            ISERV_FRA_DATO.toLocalDate(), formidlingsgruppe = Formidlingsgruppe.ISERV))
        val melding = ConsumerRecord("topic", 0, 0, "dummyKey", nyPeriode)

        kafkaConsumerService.consumeEndringPaOppfolgingBruker(oppfolginsBrukerEndretTilISERV)
        arbeidssøkerperiodeConsumerService.consumeArbeidssøkerperiode(melding)

        val oppfølgingsperioder = oppfølgingService.hentOppfolgingsperioder(Fnr.of(fnr))
        assertThat(oppfølgingsperioder).hasSize(1)
        val oppfølgingsperiode = oppfølgingsperioder.first()
        assertThat(oppfølgingsperiode.startDato).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.SECONDS))
        assertThat(oppfølgingsperiode.sluttDato).isNull()
        assertThat(oppfølgingsperiode.startetBegrunnelse).isEqualTo(OppfolgingStartBegrunnelse.ARBEIDSSOKER_REGISTRERING)
        assertThat(utmeldingRepository.eksisterendeIservBruker(aktørId).getOrNull()).isNull()
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
            OppfolgingStartBegrunnelse.ARBEIDSSOKER_REGISTRERING,
            "Z123456",
            StartetAvType.VEILEDER

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

    private fun oppfølgingsBrukerEndret(iservFraDato: LocalDate, formidlingsgruppe: Formidlingsgruppe = Formidlingsgruppe.ARBS): EndringPaaOppfoelgingsBrukerV2 {
        return TestUtils.oppfølgingsBrukerEndret(fnr, iservFraDato, formidlingsgruppe)
    }

    fun lagreOppfølgingsperiode(periode: OppfolgingsperiodeEntity) {
        jdbcTemplate.update(
            "" +
                    "INSERT INTO OPPFOLGINGSPERIODE(uuid, aktor_id, startDato, oppdatert, start_begrunnelse, startet_av, startet_av_type) " +
                    "VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?)",
            periode.uuid.toString(),
            periode.aktorId,
            Timestamp.from(periode.startDato.toInstant()),
            periode.startetBegrunnelse.name,
            periode.startetAv,
            periode.startetAvType?.name
        )
    }
}