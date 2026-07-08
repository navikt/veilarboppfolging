package no.nav.veilarboppfolging.kafka.inngang

import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.test.assertFailsWith
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.IntegrationTest
import no.nav.veilarboppfolging.client.pdl.ForenkletFolkeregisterStatus
import no.nav.veilarboppfolging.client.pdl.FregStatusOgStatsborgerskap
import no.nav.veilarboppfolging.oppfolgingsbruker.StartetAvType
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingStartBegrunnelse
import no.nav.veilarboppfolging.oppfolgingsperioderHendelser.OppfolgingsHendelseDto
import no.nav.veilarboppfolging.oppfolgingsperioderHendelser.hendelser.OppfolgingStartetHendelseDto
import no.nav.veilarboppfolging.service.OppfolgingService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertInstanceOf
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StartOppfolgingConsumerServiceTest(
    @Autowired
    val startOppfolgingConsumerService: StartOppfolgingConsumerService,
    @Autowired
    val oppfølgingService: OppfolgingService,
): IntegrationTest() {
    private val fnr = Fnr.of("01010198765")
    private val aktorId = AktorId.of("123456789012")
    private val norskStatsborgerskap = listOf("NOR")

    @BeforeEach
    fun setUp() {
        `when`(aktorOppslagClient.hentAktorId(fnr)).thenReturn(aktorId)
        `when`(aktorOppslagClient.hentFnr(aktorId)).thenReturn(fnr)
    }

    @Test
    fun `startOppfolgingMelding fra system skal starte ny arbeidsrettet oppfølgingsperiode`() {
        mockPdlFolkeregisterStatus(fnr, FregStatusOgStatsborgerskap(
            fregStatus = ForenkletFolkeregisterStatus.bosattEtterFolkeregisterloven,
            statsborgerskap = norskStatsborgerskap,
            under18 = false,
        ))
        mockArenaOppfolgingServiceRegistrerIkkeArbeidssoker(fnr)
        val startOppfolgingMelding = lagStartOppfolgingMelding(
            fnr = fnr,
            registrant = StartOppfolgingMelding.SystemRegistrant(
                opprettetAv = "ISYFO",
            )
        )
        val melding = ConsumerRecord("topic", 0, 0, fnr.get(), startOppfolgingMelding)

        startOppfolgingConsumerService.consumeStartOppfolging(melding)

        val oppfolgingsperioder = oppfølgingService.hentOppfolgingsperioder(fnr)
        assertThat(oppfolgingsperioder).hasSize(1)
        val oppfolgingsperiode = oppfolgingsperioder.first()
        assertThat(oppfolgingsperiode.startDato).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.SECONDS))
        assertThat(oppfolgingsperiode.sluttDato).isNull()
        assertThat(oppfolgingsperiode.startetBegrunnelse).isEqualTo(OppfolgingStartBegrunnelse.SYKMELDT_UTEN_ARBEIDSGIVER_4_UKER)

        val lagredeMeldingerIUtboks = getRecordsStoredInKafkaOutbox(kafkaProperties.oppfolgingshendelseV1, fnr.get())
        assertThat(lagredeMeldingerIUtboks).hasSize(1)
        assertInstanceOf<OppfolgingsHendelseDto>(lagredeMeldingerIUtboks.first())
        val hendelse = lagredeMeldingerIUtboks.first() as OppfolgingStartetHendelseDto
        assertThat(hendelse.fnr).isEqualTo(fnr.get())
        assertThat(hendelse.startetBegrunnelse).isEqualTo(OppfolgingStartBegrunnelse.SYKMELDT_UTEN_ARBEIDSGIVER_4_UKER)
        assertThat(hendelse.startetAvType).isEqualTo(StartetAvType.SYSTEM)
        assertThat(hendelse.startetAv).isEqualTo("ISYFO")
        assertThat(hendelse.startetTidspunkt).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.SECONDS))
        assertThat(hendelse.foretrukketArbeidsoppfolgingskontor).isNull()
        assertThat(hendelse.oppfolgingsPeriodeId).isEqualTo(oppfolgingsperiode.uuid)
    }

    @Test
    fun `startOppfolgingMelding fra veileder skal starte ny arbeidsrettet oppfølgingsperiode`() {
        mockPdlFolkeregisterStatus(fnr, FregStatusOgStatsborgerskap(
            fregStatus = ForenkletFolkeregisterStatus.bosattEtterFolkeregisterloven,
            statsborgerskap = norskStatsborgerskap,
            under18 = false,
        ))
        mockArenaOppfolgingServiceRegistrerIkkeArbeidssoker(fnr)
        val startOppfolgingMelding = lagStartOppfolgingMelding(
            fnr = fnr,
            registrant = StartOppfolgingMelding.VeilederRegistrant(
                opprettetAv = "N123456",
            )
        )
        val melding = ConsumerRecord("topic", 0, 0, fnr.get(), startOppfolgingMelding)

        startOppfolgingConsumerService.consumeStartOppfolging(melding)

        val oppfolgingsperioder = oppfølgingService.hentOppfolgingsperioder(fnr)
        assertThat(oppfolgingsperioder).hasSize(1)
        val oppfolgingsperiode = oppfolgingsperioder.first()
        assertThat(oppfolgingsperiode.startDato).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.SECONDS))
        assertThat(oppfolgingsperiode.sluttDato).isNull()
        assertThat(oppfolgingsperiode.startetBegrunnelse).isEqualTo(OppfolgingStartBegrunnelse.SYKMELDT_UTEN_ARBEIDSGIVER_4_UKER)

        val lagredeMeldingerIUtboks = getRecordsStoredInKafkaOutbox(kafkaProperties.oppfolgingshendelseV1, fnr.get())
        assertThat(lagredeMeldingerIUtboks).hasSize(1)
        assertInstanceOf<OppfolgingsHendelseDto>(lagredeMeldingerIUtboks.first())
        val hendelse = lagredeMeldingerIUtboks.first() as OppfolgingStartetHendelseDto
        assertThat(hendelse.fnr).isEqualTo(fnr.get())
        assertThat(hendelse.startetBegrunnelse).isEqualTo(OppfolgingStartBegrunnelse.SYKMELDT_UTEN_ARBEIDSGIVER_4_UKER)
        assertThat(hendelse.startetAvType).isEqualTo(StartetAvType.VEILEDER)
        assertThat(hendelse.startetAv).isEqualTo("N123456")
        assertThat(hendelse.startetTidspunkt).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.SECONDS))
        assertThat(hendelse.foretrukketArbeidsoppfolgingskontor).isNull()
        assertThat(hendelse.oppfolgingsPeriodeId).isEqualTo(oppfolgingsperiode.uuid)
    }

    @Test
    fun `startOppfolgingMelding blir ignorert hvis bruker ikke har lovlig opphold`() {
        mockPdlFolkeregisterStatus(fnr, FregStatusOgStatsborgerskap(
            fregStatus = ForenkletFolkeregisterStatus.opphoert,
            statsborgerskap = emptyList(),
            under18 = false,
        ))
        mockArenaOppfolgingServiceRegistrerIkkeArbeidssoker(fnr)
        val startOppfolgingMelding = lagStartOppfolgingMelding(
            fnr = fnr,
            registrant = StartOppfolgingMelding.SystemRegistrant(
                opprettetAv = "ISYFO",
            )
        )
        val melding = ConsumerRecord("topic", 0, 0, fnr.get(), startOppfolgingMelding)

        startOppfolgingConsumerService.consumeStartOppfolging(melding)

        val oppfolgingsperioder = oppfølgingService.hentOppfolgingsperioder(fnr)
        assertThat(oppfolgingsperioder).hasSize(0)

        val lagredeMeldingerIUtboks = getRecordsStoredInKafkaOutbox(kafkaProperties.oppfolgingshendelseV1, fnr.get())
        assertThat(lagredeMeldingerIUtboks).hasSize(0)
    }

    @Test
    fun `startOppfolgingMelding blir ignorert hvis bruker er under 18`() {
        mockPdlFolkeregisterStatus(fnr, FregStatusOgStatsborgerskap(
            fregStatus = ForenkletFolkeregisterStatus.bosattEtterFolkeregisterloven,
            statsborgerskap = norskStatsborgerskap,
            under18 = true,
        ))
        mockArenaOppfolgingServiceRegistrerIkkeArbeidssoker(fnr)
        val startOppfolgingMelding = lagStartOppfolgingMelding(
            fnr = fnr,
            registrant = StartOppfolgingMelding.SystemRegistrant(
                opprettetAv = "ISYFO",
            )
        )
        val melding = ConsumerRecord("topic", 0, 0, fnr.get(), startOppfolgingMelding)

        startOppfolgingConsumerService.consumeStartOppfolging(melding)

        val oppfolgingsperioder = oppfølgingService.hentOppfolgingsperioder(fnr)
        assertThat(oppfolgingsperioder).hasSize(0)

        val lagredeMeldingerIUtboks = getRecordsStoredInKafkaOutbox(kafkaProperties.oppfolgingshendelseV1, fnr.get())
        assertThat(lagredeMeldingerIUtboks).hasSize(0)
    }

    @Test
    fun `startOppfolgingMelding feiler hvis Arena svarer med teknisk feil`() {
        mockPdlFolkeregisterStatus(fnr, FregStatusOgStatsborgerskap(
            fregStatus = ForenkletFolkeregisterStatus.bosattEtterFolkeregisterloven,
            statsborgerskap = norskStatsborgerskap,
            under18 = false,
        ))
        mockArenaOppfolgingServiceRegistrerIkkeArbeidssokerTekniskFeil(fnr)
        val startOppfolgingMelding = lagStartOppfolgingMelding(
            fnr = fnr,
            registrant = StartOppfolgingMelding.SystemRegistrant(
                opprettetAv = "ISYFO",
            )
        )
        val melding = ConsumerRecord("topic", 0, 0, fnr.get(), startOppfolgingMelding)

        assertFailsWith<RuntimeException> {
            startOppfolgingConsumerService.consumeStartOppfolging(melding)
        }
    }

    fun lagStartOppfolgingMelding(fnr: Fnr, registrant: StartOppfolgingMelding.Registrant): StartOppfolgingMelding {
        return StartOppfolgingMelding(
            personident = fnr.get(),
            aarsak = StartOppfolgingMelding.Aarsak.SYKMELDT_UTEN_ARBEIDSGIVER_4_UKER,
            registrant = registrant,
            arbeidsoppfolgingskontor = null,
            kilde = StartOppfolgingMelding.Kilde.ISYFO,
            sendtTidspunkt = ZonedDateTime.now().toLocalDateTime(),
        )
    }
}