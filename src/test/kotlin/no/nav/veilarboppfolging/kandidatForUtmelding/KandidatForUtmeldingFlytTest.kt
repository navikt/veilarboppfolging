package no.nav.veilarboppfolging.kandidatForUtmelding

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NavIdent
import no.nav.paw.arbeidssokerregisteret.api.v1.AvviksType
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.TidspunktFraKilde
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe
import no.nav.veilarboppfolging.IntegrationTest
import no.nav.veilarboppfolging.kafka.ArbeidssøkerperiodeConsumerService
import no.nav.veilarboppfolging.kafka.TestUtils
import no.nav.veilarboppfolging.oppfolgingsbruker.VeilederRegistrant
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingsRegistrering
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.UtmeldingsService
import no.nav.veilarboppfolging.repository.UtmeldingRepository
import no.nav.veilarboppfolging.service.KafkaConsumerService
import no.nav.veilarboppfolging.service.OppfolgingsbrukerEndretIArenaService
import no.nav.veilarboppfolging.service.ReaktiveringService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata as MetaData

class KandidatForUtmeldingFlytTest(
    @Autowired
    val arbeidssoekerperiodeConsumerService: ArbeidssøkerperiodeConsumerService,
    @Autowired
    val kafkaConsumerService: KafkaConsumerService,
    @Autowired
    val utmeldingRepository: UtmeldingRepository,
    @Autowired
    val utmeldingsService: UtmeldingsService,
    @Autowired
    val oppfolgingsbrukerEndretIArenaService: OppfolgingsbrukerEndretIArenaService,
    @Autowired
    val reaktiveringService: ReaktiveringService,
) : IntegrationTest() {

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

        assertThat(kandidatForUtmeldingRepository.hentKandidat(aktorId)).isNull()

        val sluttMelding = ConsumerRecord("topic", 0, 0, "dummyKey", arbeidssokerperiode(fnr, periodeAvsluttet = true))
        arbeidssoekerperiodeConsumerService.consumeArbeidssøkerperiode(sluttMelding)

        assertThat(kandidatForUtmeldingRepository.hentKandidat(aktorId)).isNotNull()
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

        val nyPeriode = arbeidssokerperiode(
            fnr,
            periodeStartet = arbeidsoekerPeriodeStartet.atZone(ZoneId.systemDefault()).toInstant()
        )
        val oppfolginsBrukerEndretTilISERV = ConsumerRecord(
            "topic", 0, 0, "key", TestUtils.oppfølgingsBrukerEndret(
                fnr, iservFraDato = ISERV_FRA_DATO, formidlingsgruppe = Formidlingsgruppe.ISERV
            )
        )

        kafkaConsumerService.consumeEndringPaOppfolgingBruker(oppfolginsBrukerEndretTilISERV)

        val sluttMelding = ConsumerRecord(
            "topic",
            0,
            0,
            "dummyKey",
            arbeidssokerperiode(
                fnr,
                periodeAvsluttet = true,
                periodeStartet = arbeidsoekerPeriodeStartet.atZone(ZoneId.systemDefault()).toInstant()
            )
        )
        arbeidssoekerperiodeConsumerService.consumeArbeidssøkerperiode(
            ConsumerRecord(
                "topic",
                0,
                0,
                "dummyKey",
                nyPeriode
            )
        )
        arbeidssoekerperiodeConsumerService.consumeArbeidssøkerperiode(sluttMelding)

        assertThat(utmeldingRepository.eksisterendeIservBruker(aktorId).isPresent).isTrue()
    }

    @Test
    fun `Sletter kandidat-for-utmelding når ny oppfølgingsperiode startes manuelt av veileder`() {
        mockVeilarbArenaOppfolgingsBruker(Fnr.of(fnr), Formidlingsgruppe.ISERV)
        startOppfolgingSomArbeidsoker(aktorId, Fnr.of(fnr))
        val oppfolgingsperiodeUuid = oppfolgingService.hentGjeldendeOppfolgingsperiode(Fnr.of(fnr)).get().uuid
        kandidatForUtmeldingRepository.lagreKandidat(ArbeidssøkerPeriodeAvsluttet(
            aktorId = aktorId,
            fnr = Fnr.of(fnr),
            oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
            avsluttetAv = KandidatForUtmeldingHendelseAvsluttetAv.VEILEDER,
            kilde ="kilde",
            avsluttetAarsakType = "aarsak")
        )
        avsluttOppfolgingManueltSomVeileder(aktorId)

        val registrering = OppfolgingsRegistrering.manuellRegistreringVeileder(Fnr.of(fnr), aktorId, VeilederRegistrant(NavIdent("veileder")), null, true)
        startOppfolging(aktorId, registrering)

        assertThat(kandidatForUtmeldingRepository.hentKandidat(aktorId)).isNull()
    }

    @Test
    fun `Sletter kandidat-for-utmelding når ny oppfølgingsperiode startes manuelt av bruker`() {
        mockVeilarbArenaOppfolgingsBruker(Fnr.of(fnr), Formidlingsgruppe.ISERV)
        startOppfolgingSomArbeidsoker(aktorId, Fnr.of(fnr))
        val oppfolgingsperiodeUuid = oppfolgingService.hentGjeldendeOppfolgingsperiode(Fnr.of(fnr)).get().uuid
        kandidatForUtmeldingRepository.lagreKandidat(ArbeidssøkerPeriodeAvsluttet(
            aktorId = aktorId,
            fnr = Fnr.of(fnr),
            oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
            avsluttetAv = KandidatForUtmeldingHendelseAvsluttetAv.VEILEDER,
            kilde ="kilde",
            avsluttetAarsakType = "aarsak")
        )
        avsluttOppfolgingManueltSomVeileder(aktorId)

        val registrering = OppfolgingsRegistrering.manuellRegistreringBruker(Fnr.of(fnr), aktorId)
        startOppfolging(aktorId, registrering)

        assertThat(kandidatForUtmeldingRepository.hentKandidat(aktorId)).isNull()
    }

    @Test
    fun `Sletter kandidat-for-utmelding når ny oppfølgingsperiode startes via arbeidssøkerregisteret`() {
        mockVeilarbArenaOppfolgingsBruker(Fnr.of(fnr), Formidlingsgruppe.ISERV)
        startOppfolgingSomArbeidsoker(aktorId, Fnr.of(fnr))
        val oppfolgingsperiodeUuid = oppfolgingService.hentGjeldendeOppfolgingsperiode(Fnr.of(fnr)).get().uuid
        kandidatForUtmeldingRepository.lagreKandidat(ArbeidssøkerPeriodeAvsluttet(
            aktorId = aktorId,
            fnr = Fnr.of(fnr),
            oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
            avsluttetAv = KandidatForUtmeldingHendelseAvsluttetAv.VEILEDER,
            kilde ="kilde",
            avsluttetAarsakType = "aarsak")
        )
        avsluttOppfolgingManueltSomVeileder(aktorId)
        val registrering = OppfolgingsRegistrering.arbeidssokerRegistrering(Fnr.of(fnr), aktorId, VeilederRegistrant(NavIdent("veileder")))
        startOppfolging(aktorId, registrering)

        assertThat(kandidatForUtmeldingRepository.hentKandidat(aktorId)).isNull()
    }

    @Test
    fun `Sletter kandidat-for-utmelding hvis bruker er under oppfølging og starter ny arbeidssøkerperiode`() {
        mockVeilarbArenaOppfolgingsBruker(Fnr.of(fnr), Formidlingsgruppe.ISERV)
        startOppfolgingSomArbeidsoker(aktorId, Fnr.of(fnr))
        val oppfolgingsperiodeUuid = oppfolgingService.hentGjeldendeOppfolgingsperiode(Fnr.of(fnr)).get().uuid
        kandidatForUtmeldingRepository.lagreKandidat(ArbeidssøkerPeriodeAvsluttet(
            aktorId = aktorId,
            fnr = Fnr.of(fnr),
            oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
            avsluttetAv = KandidatForUtmeldingHendelseAvsluttetAv.VEILEDER,
            kilde ="kilde",
            avsluttetAarsakType = "aarsak")
        )
        assertThat(kandidatForUtmeldingRepository.hentKandidat(aktorId)).isNotNull()

        val nyPeriode = arbeidssokerperiode(
            fnr,
            periodeStartet = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()
        )
        arbeidssoekerperiodeConsumerService.consumeArbeidssøkerperiode(
            ConsumerRecord(
                "topic",
                0,
                0,
                "dummyKey",
                nyPeriode
            )
        )

        assertThat(kandidatForUtmeldingRepository.hentKandidat(aktorId)).isNull()
    }

    @Test
    fun `Sletter kandidat-for-utmelding når ny oppfølgingsperiode startes via melding fra Arena`() {
        mockVeilarbArenaOppfolgingsBruker(Fnr.of(fnr), Formidlingsgruppe.ISERV)
        startOppfolgingSomArbeidsoker(aktorId, Fnr.of(fnr))
        val oppfolgingsperiodeUuid = oppfolgingService.hentGjeldendeOppfolgingsperiode(Fnr.of(fnr)).get().uuid
        kandidatForUtmeldingRepository.lagreKandidat(ArbeidssøkerPeriodeAvsluttet(
            aktorId = aktorId,
            fnr = Fnr.of(fnr),
            oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
            avsluttetAv = KandidatForUtmeldingHendelseAvsluttetAv.VEILEDER,
            kilde ="kilde",
            avsluttetAarsakType = "aarsak")
        )
        avsluttOppfolgingManueltSomVeileder(aktorId)

        val registrering = OppfolgingsRegistrering.arenaSyncOppfolgingBrukerRegistrering(Fnr.of(fnr), aktorId,
            Formidlingsgruppe.IARBS, Kvalifiseringsgruppe.VURDU, EnhetId("0318"))
        startOppfolging(aktorId, registrering)

        assertThat(kandidatForUtmeldingRepository.hentKandidat(aktorId)).isNull()
    }

    @Test
    fun `Sletter kandidat-for-utmelding når ny oppfølgingsperiode reaktiveres`() {
        mockVeilarbArenaOppfolgingsBruker(Fnr.of(fnr), Formidlingsgruppe.ISERV)
        startOppfolgingSomArbeidsoker(aktorId, Fnr.of(fnr))
        mockInternBrukerAuthOk(UUID.randomUUID(), aktorId, Fnr.of(fnr))
        mockArenaOppfolgingServiceRegistrerIkkeArbeidssoker(Fnr.of(fnr))
        val oppfolgingsperiodeUuid = oppfolgingService.hentGjeldendeOppfolgingsperiode(Fnr.of(fnr)).get().uuid
        kandidatForUtmeldingRepository.lagreKandidat(ArbeidssøkerPeriodeAvsluttet(
            aktorId = aktorId,
            fnr = Fnr.of(fnr),
            oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
            avsluttetAv = KandidatForUtmeldingHendelseAvsluttetAv.VEILEDER,
            kilde ="kilde",
            avsluttetAarsakType = "aarsak")
        )
        assertThat(kandidatForUtmeldingRepository.hentKandidat(aktorId)).isNotNull()

        reaktiveringService.reaktiverBrukerIArena(Fnr.of(fnr))

        assertThat(kandidatForUtmeldingRepository.hentKandidat(aktorId)).isNull()
    }

    private fun arbeidssokerperiode(
        fodselsnummer: String,
        periodeAvsluttet: Boolean = false,
        periodeStartet: Instant = Instant.now().minusSeconds(1)
    ): Periode {
        val slutt = if (periodeAvsluttet) {
            MetaData().apply {
                tidspunkt = Instant.now()
                utfoertAv = Bruker(BrukerType.VEILEDER, "dummyId", "tokenx:Level4")
                kilde = "dummyKilde"
                aarsak = "dummyAarsak"
                tidspunktFraKilde = TidspunktFraKilde(Instant.now(), AvviksType.FORSINKELSE)
            }
        } else {
            null
        }

        return Periode().apply {
            id = UUID.randomUUID()
            identitetsnummer = fodselsnummer
            startet = MetaData().apply {
                tidspunkt = periodeStartet
                utfoertAv = Bruker(BrukerType.VEILEDER, "dummyId", "tokenx:Level4")
                kilde = "dummyKilde"
                aarsak = "dummyAarsak"
                tidspunktFraKilde = TidspunktFraKilde(periodeStartet, AvviksType.FORSINKELSE)
            }
            avsluttet = slutt
        }
    }
}

