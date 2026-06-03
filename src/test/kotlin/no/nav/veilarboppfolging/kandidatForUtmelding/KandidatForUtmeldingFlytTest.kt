package no.nav.veilarboppfolging.kandidatForUtmelding

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.paw.arbeidssokerregisteret.api.v1.*
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe
import no.nav.veilarboppfolging.IntegrationTest
import no.nav.veilarboppfolging.kafka.ArbeidssøkerperiodeConsumerService
import no.nav.veilarboppfolging.kafka.TestUtils
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.EndringPaaOppfolgingsBruker
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.OppdateringFraArena_BleIserv
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.UtmeldingsService
import no.nav.veilarboppfolging.repository.UtmeldingRepository
import no.nav.veilarboppfolging.service.KafkaConsumerService
import no.nav.veilarboppfolging.service.OppfolgingsbrukerEndretIArenaService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import java.time.*
import java.util.*
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata as MetaData

@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KandidatForUtmeldingFlytTest(
    @Autowired
    val arbeidssoekerperiodeConsumerService: ArbeidssøkerperiodeConsumerService,
    @Autowired
    val kafkaConsumerService: KafkaConsumerService,
    @Autowired
    val utmeldingRepository: UtmeldingRepository,
    @Autowired
    val kandidatForUtmeldingRepository: KandidatForUtmeldingRepository,
    @Autowired
    val utmeldingsService: UtmeldingsService,
    @Autowired
    val oppfolgingsbrukerEndretIArenaService: OppfolgingsbrukerEndretIArenaService,
): IntegrationTest() {

    private val fnr = "01010198765"
    private val aktorId = AktorId.of("123456789012")

    @BeforeEach
    fun setUp() {
        `when`(aktorOppslagClient.hentAktorId(Fnr.of(fnr))).thenReturn(aktorId)
        `when`(aktorOppslagClient.hentFnr(aktorId)).thenReturn(Fnr.of(fnr))
    }

    @Test
    fun `lagreKandidatForUtmelding blir kalt når arbeidssøkerperiode avsluttes og bruker kan avsluttes`() {
        mockSytemBrukerAuthOk(aktorId, Fnr.of(fnr))
        startOppfolgingSomArbeidsoker(aktorId, Fnr.of(fnr))
        setLocalArenaOppfolging(aktorId, Formidlingsgruppe.ARBS)
        mockTiltakshistorikk(Fnr.of(fnr), harAktiveDeltakelser = false)
        mockUngdomsprogram(Fnr.of(fnr), erDeltaker = false)
        mockArbeidssoekerregisteret(Fnr.of(fnr), erArbeidssoeker = false)
        mockAap(Fnr.of(fnr), harAap = false)

        assertThat(hentKandidatFraDb(aktorId)).isNull()

        val sluttMelding = ConsumerRecord("topic", 0, 0, "dummyKey", arbeidssokerperiode(fnr, periodeAvsluttet = true))
        arbeidssoekerperiodeConsumerService.consumeArbeidssøkerperiode(sluttMelding)

        assertThat(hentKandidatFraDb(aktorId)).isNotNull()
    }

    @Test
    fun `fjernKandidatForUtmelding blir kalt når ny arbeidssøkerperiode starter`() {
        mockSytemBrukerAuthOk(aktorId, Fnr.of(fnr))
        kandidatForUtmeldingRepository.lagreKandidat(ArbeidssøkerPeriodeAvsluttet(aktorId, Fnr.of(fnr)))
        assertThat(hentKandidatFraDb(aktorId)).isNotNull()

        val nyPeriode = arbeidssokerperiode(fnr, periodeAvsluttet = false)
        val melding = ConsumerRecord("topic", 0, 0, "dummyKey", nyPeriode)
        arbeidssoekerperiodeConsumerService.consumeArbeidssøkerperiode(melding)

        assertThat(hentKandidatFraDb(aktorId)).isNull()
    }

    @Test
    fun `fjernKandidatForUtmelding blir kalt når UtmeldingsService avslutter oppfølging etter 28 dager ISERV`() {
        mockSytemBrukerAuthOk(aktorId, Fnr.of(fnr))
        startOppfolgingSomArbeidsoker(aktorId, Fnr.of(fnr))
        setLocalArenaOppfolging(aktorId, Formidlingsgruppe.ISERV)
        mockTiltakshistorikk(Fnr.of(fnr), harAktiveDeltakelser = false)
        mockUngdomsprogram(Fnr.of(fnr), erDeltaker = false)
        mockArbeidssoekerregisteret(Fnr.of(fnr), erArbeidssoeker = false)
        mockAap(Fnr.of(fnr), harAap = false)

        utmeldingRepository.insertUtmeldingTabell(OppdateringFraArena_BleIserv(aktorId, ZonedDateTime.now().minusDays(29)))
        kandidatForUtmeldingRepository.lagreKandidat(ArbeidssøkerPeriodeAvsluttet(aktorId, Fnr.of(fnr)))
        assertThat(hentKandidatFraDb(aktorId)).isNotNull()

        utmeldingsService.avsluttOppfolgingOgFjernFraUtmeldingsTabell(aktorId)

        assertThat(hentKandidatFraDb(aktorId)).isNull()
    }

    @Test
    fun `fjernKandidatForUtmelding blir kalt når bruker blir inaktiv i Arena uten mulighet for reaktivering`() {
        mockSytemBrukerAuthOk(aktorId, Fnr.of(fnr))
        startOppfolgingSomArbeidsoker(aktorId, Fnr.of(fnr))
        setLocalArenaOppfolging(aktorId, Formidlingsgruppe.ARBS)
        mockTiltakshistorikk(Fnr.of(fnr), harAktiveDeltakelser = false)
        mockUngdomsprogram(Fnr.of(fnr), erDeltaker = false)
        mockArbeidssoekerregisteret(Fnr.of(fnr), erArbeidssoeker = false)
        mockAap(Fnr.of(fnr), harAap = false)
        mockVeilarbArenaOppfolgingsStatus(Fnr.of(fnr), kanEnkeltReaktiveres = false)
        kandidatForUtmeldingRepository.lagreKandidat(ArbeidssøkerPeriodeAvsluttet(aktorId, Fnr.of(fnr)))
        assertThat(hentKandidatFraDb(aktorId)).isNotNull()
        val arenaEndring = EndringPaaOppfolgingsBruker(
            aktorId = aktorId,
            fodselsnummer = fnr,
            formidlingsgruppe = Formidlingsgruppe.ISERV,
            kvalifiseringsgruppe = Kvalifiseringsgruppe.BATT,
            oppfolgingsenhet = "1234",
            iservFraDato = LocalDate.now(),
            rettighetsgruppe = null,
            hovedmaal = null,
            sistEndretDato = ZonedDateTime.now()
        )

        oppfolgingsbrukerEndretIArenaService.oppdaterOppfolgingMedStatusFraArena(arenaEndring)

        assertThat(hentKandidatFraDb(aktorId)).isNull()
    }

    @Test
    fun `lagreKandidatForUtmelding blir kalt når bruker blir ISERV etter arbeidssøkerregistrering`() {
        mockSytemBrukerAuthOk(aktorId, Fnr.of(fnr))
        val arbeidsoekerPeriodeStartet = LocalDateTime.of(2024, 10, 1, 23, 59)
        val ISERV_FRA_DATO = LocalDate.of(2024, 10, 2)
        mockVeilarbArenaOppfolgingsBruker(
            Fnr.of(fnr),
            Formidlingsgruppe.ISERV,
            iservFraDato = ISERV_FRA_DATO.atStartOfDay(ZoneId.systemDefault())
        )
        mockTiltakshistorikk(Fnr.of(fnr), harAktiveDeltakelser = false)
        mockUngdomsprogram(Fnr.of(fnr), erDeltaker = false)
        mockArbeidssoekerregisteret(Fnr.of(fnr), erArbeidssoeker = false)
        mockAap(Fnr.of(fnr), harAap = false)

        val nyPeriode = arbeidssokerperiode(fnr, periodeStartet = arbeidsoekerPeriodeStartet.atZone(ZoneId.systemDefault()).toInstant())
        val oppfolginsBrukerEndretTilISERV = ConsumerRecord("topic", 0, 0, "key", TestUtils.oppfølgingsBrukerEndret(
            fnr, iservFraDato = ISERV_FRA_DATO, formidlingsgruppe = Formidlingsgruppe.ISERV))

        kafkaConsumerService.consumeEndringPaOppfolgingBruker(oppfolginsBrukerEndretTilISERV)

        val sluttMelding = ConsumerRecord("topic", 0, 0, "dummyKey", arbeidssokerperiode(fnr, periodeAvsluttet = true, periodeStartet = arbeidsoekerPeriodeStartet.atZone(ZoneId.systemDefault()).toInstant()))
        arbeidssoekerperiodeConsumerService.consumeArbeidssøkerperiode(ConsumerRecord("topic", 0, 0, "dummyKey", nyPeriode))
        arbeidssoekerperiodeConsumerService.consumeArbeidssøkerperiode(sluttMelding)

        assertThat(utmeldingRepository.eksisterendeIservBruker(aktorId).isPresent).isTrue()
    }

    private fun arbeidssokerperiode(fodselsnummer: String, periodeAvsluttet: Boolean = false, periodeStartet: Instant = Instant.now().minusSeconds(1)): Periode {
        val slutt = if (periodeAvsluttet) {
            MetaData().apply {
                tidspunkt = Instant.now()
                utfoertAv = Bruker(BrukerType.VEILEDER, "dummyId")
                kilde = "dummyKilde"
                aarsak = "dummyAarsak"
                tidspunktFraKilde = TidspunktFraKilde(Instant.now(), AvviksType.FORSINKELSE)
            }
        } else { null }

        return Periode().apply {
            id = UUID.randomUUID()
            identitetsnummer = fodselsnummer
            startet = MetaData().apply {
                tidspunkt = periodeStartet
                utfoertAv = Bruker(BrukerType.VEILEDER, "dummyId")
                kilde = "dummyKilde"
                aarsak = "dummyAarsak"
                tidspunktFraKilde = TidspunktFraKilde(periodeStartet, AvviksType.FORSINKELSE)
            }
            avsluttet = slutt
        }
    }

    private fun hentKandidatFraDb(aktorId: AktorId): String? {
        return namedParameterJdbcTemplate.queryForList(
            "SELECT aktor_id FROM kandidat_for_utmelding WHERE aktor_id = :aktorId",
            mapOf("aktorId" to aktorId.get()),
            String::class.java
        ).firstOrNull()
    }
}

