package no.nav.veilarboppfolging.kandidatForUtmelding

import no.nav.common.client.aktoroppslag.BrukerIdenter
import no.nav.common.json.JsonUtils
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NavIdent
import no.nav.paw.arbeidssokerregisteret.api.v1.*
import no.nav.paw.arbeidssokerregisteret.api.v1.AvsluttetAarsakType.BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST
import no.nav.poao_tilgang.client.Decision
import no.nav.poao_tilgang.client.TilgangType
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe
import no.nav.veilarboppfolging.IntegrationTest
import no.nav.veilarboppfolging.kafka.ArbeidssøkerperiodeConsumerService
import no.nav.veilarboppfolging.kafka.TestUtils
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingHendelse.Companion.KARENSTID_DAGER
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.BeskrivelseEnum
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.Kategori
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.Operasjon
import no.nav.veilarboppfolging.oppfolgingsbruker.VeilederRegistrant
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingsRegistrering
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingsRegistrering.Companion.arbeidssokerRegistrering
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
import java.time.*
import java.util.*
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

        assertThat(kandidatForUtmeldingService.hentKandidatForUtmeldingTag(aktorId)).isNull()

        val sluttMelding = ConsumerRecord("topic", 0, 0, "dummyKey", arbeidssokerperiode(fnr, periodeAvsluttet = true))
        arbeidssoekerperiodeConsumerService.consumeArbeidssøkerperiode(sluttMelding)

        assertThat(kandidatForUtmeldingService.hentKandidatForUtmeldingTag(aktorId)).isNotNull()
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
        kandidatForUtmeldingRepository.lagreKandidat(
            ArbeidssøkerPeriodeAvsluttet(
                oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
                utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
                utfortAv = "A123123",
                kilde = "arbeidssøkerregisteret",
                hendelseTidspunkt = ZonedDateTime.now().toInstant(),
                arbeidssokerperiodeAvsluttetHendelseType = ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
                avslutningsarsak = AvsluttetAarsakType.BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST.toString()
            )
        )
        avsluttOppfolgingManueltSomVeileder(aktorId)

        val registrering = OppfolgingsRegistrering.manuellRegistreringVeileder(
            Fnr.of(fnr),
            aktorId,
            VeilederRegistrant(NavIdent("veileder")),
            null,
            true
        )
        startOppfolging(aktorId, registrering)

        assertThat(kandidatForUtmeldingService.hentKandidatForUtmeldingTag(aktorId)).isNull()
    }

    @Test
    fun `Sletter kandidat-for-utmelding når ny oppfølgingsperiode startes manuelt av bruker`() {
        `when`(aktorOppslagClient.hentIdenter(Fnr(fnr))).thenReturn(
            BrukerIdenter(
                Fnr.of(fnr),
                aktorId,
                emptyList(),
                emptyList()
            )
        )
        mockVeilarbArenaOppfolgingsBruker(Fnr.of(fnr), Formidlingsgruppe.ISERV)
        startOppfolgingSomArbeidsoker(aktorId, Fnr.of(fnr))
        val oppfolgingsperiodeUuid = oppfolgingService.hentGjeldendeOppfolgingsperiode(Fnr.of(fnr)).get().uuid
        kandidatForUtmeldingRepository.lagreKandidat(
            ArbeidssøkerPeriodeAvsluttet(
                oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
                utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
                utfortAv = "A123123",
                kilde = "kilde",
                hendelseTidspunkt = ZonedDateTime.now().toInstant(),
                arbeidssokerperiodeAvsluttetHendelseType = ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
                avslutningsarsak = AvsluttetAarsakType.BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST.toString()
            )
        )
        avsluttOppfolgingManueltSomVeileder(aktorId)

        val registrering = OppfolgingsRegistrering.manuellRegistreringBruker(Fnr.of(fnr), aktorId)
        startOppfolging(aktorId, registrering)

        assertThat(kandidatForUtmeldingService.hentKandidatForUtmeldingTag(aktorId)).isNull()
    }

    @Test
    fun `Sletter kandidat-for-utmelding når ny oppfølgingsperiode avsluttes manuelt av veileder`() {
        `when`(aktorOppslagClient.hentIdenter(Fnr(fnr))).thenReturn(
            BrukerIdenter(
                Fnr.of(fnr),
                aktorId,
                emptyList(),
                emptyList()
            )
        )
        mockVeilarbArenaOppfolgingsBruker(Fnr.of(fnr), Formidlingsgruppe.ISERV)
        startOppfolgingSomArbeidsoker(aktorId, Fnr.of(fnr))
        val oppfolgingsperiodeUuid = oppfolgingService.hentGjeldendeOppfolgingsperiode(Fnr.of(fnr)).get().uuid
        kandidatForUtmeldingRepository.lagreKandidat(
            ArbeidssøkerPeriodeAvsluttet(
                oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
                utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
                utfortAv = "A123123",
                kilde = "kilde",
                hendelseTidspunkt = ZonedDateTime.now().toInstant(),
                arbeidssokerperiodeAvsluttetHendelseType = ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
                avslutningsarsak = AvsluttetAarsakType.BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST.toString()
            )
        )

        avsluttOppfolgingManueltSomVeileder(aktorId)
        val registrering = arbeidssokerRegistrering(Fnr.of(fnr), aktorId, VeilederRegistrant(NavIdent("veileder")))
        startOppfolging(aktorId, registrering)

        assertThat(kandidatForUtmeldingService.hentKandidatForUtmeldingTag(aktorId)).isNull()
    }

    @Test
    fun `Sletter kandidat-for-utmelding hvis bruker er under oppfølging og starter ny arbeidssøkerperiode`() {
        mockVeilarbArenaOppfolgingsBruker(Fnr.of(fnr), Formidlingsgruppe.ISERV)
        startOppfolgingSomArbeidsoker(aktorId, Fnr.of(fnr))
        val oppfolgingsperiodeUuid = oppfolgingService.hentGjeldendeOppfolgingsperiode(Fnr.of(fnr)).get().uuid
        kandidatForUtmeldingRepository.lagreKandidat(
            ArbeidssøkerPeriodeAvsluttet(
                oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
                utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
                utfortAv = "A123123",
                kilde = "kilde",
                hendelseTidspunkt = ZonedDateTime.now().toInstant(),
                arbeidssokerperiodeAvsluttetHendelseType = ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
                avslutningsarsak = AvsluttetAarsakType.BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST.toString()
            )
        )
        assertThat(kandidatForUtmeldingService.hentKandidatForUtmeldingTag(aktorId)).isNotNull()

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

        assertThat(kandidatForUtmeldingService.hentKandidatForUtmeldingTag(aktorId)).isNull()
    }

    @Test
    fun `Sletter kandidat-for-utmelding når ny oppfølgingsperiode startes via melding fra Arena`() {
        mockVeilarbArenaOppfolgingsBruker(Fnr.of(fnr), Formidlingsgruppe.ISERV)
        startOppfolgingSomArbeidsoker(aktorId, Fnr.of(fnr))
        val oppfolgingsperiodeUuid = oppfolgingService.hentGjeldendeOppfolgingsperiode(Fnr.of(fnr)).get().uuid
        kandidatForUtmeldingRepository.lagreKandidat(
            ArbeidssøkerPeriodeAvsluttet(
                oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
                utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
                utfortAv = "A123123",
                kilde = "kilde",
                hendelseTidspunkt = ZonedDateTime.now().toInstant(),
                arbeidssokerperiodeAvsluttetHendelseType = ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
                avslutningsarsak = BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST.toString()
            )
        )
        avsluttOppfolgingManueltSomVeileder(aktorId)

        val registrering = OppfolgingsRegistrering.arenaSyncOppfolgingBrukerRegistrering(
            Fnr.of(fnr), aktorId,
            Formidlingsgruppe.IARBS, Kvalifiseringsgruppe.VURDU
        )
        startOppfolging(aktorId, registrering)

        assertThat(kandidatForUtmeldingService.hentKandidatForUtmeldingTag(aktorId)).isNull()
    }

    @Test
    fun `Sletter kandidat-for-utmelding når ny oppfølgingsperiode reaktiveres`() {
        mockVeilarbArenaOppfolgingsBruker(Fnr.of(fnr), Formidlingsgruppe.ISERV)
        startOppfolgingSomArbeidsoker(aktorId, Fnr.of(fnr))
        mockInternBrukerAuthOk(UUID.randomUUID(), aktorId, Fnr.of(fnr))
        mockArenaOppfolgingServiceRegistrerIkkeArbeidssoker(Fnr.of(fnr))
        val oppfolgingsperiodeUuid = oppfolgingService.hentGjeldendeOppfolgingsperiode(Fnr.of(fnr)).get().uuid
        kandidatForUtmeldingRepository.lagreKandidat(
            ArbeidssøkerPeriodeAvsluttet(
                oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
                utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
                utfortAv = "A123123",
                kilde = "kilde",
                hendelseTidspunkt = ZonedDateTime.now().toInstant(),
                arbeidssokerperiodeAvsluttetHendelseType = ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
                avslutningsarsak = AvsluttetAarsakType.BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST.toString()
            )
        )
        assertThat(kandidatForUtmeldingService.hentKandidatForUtmeldingTag(aktorId)).isNotNull()

        reaktiveringService.reaktiverBrukerIArena(Fnr.of(fnr))

        assertThat(kandidatForUtmeldingService.hentKandidatForUtmeldingTag(aktorId)).isNull()
    }

    @Test
    fun `AvsluttAarsakType SVARTE_NEI_I_BEKREFTELSE mappes til riktig KandidatForUtmeldingHendelseType ARBEIDSSOKERPERIODE_AVSLUTTET_SVARTE_NEI_I_BEKREFTELSE`() {
        mockSytemBrukerAuthOk(aktorId, Fnr.of(fnr))
        startOppfolgingSomArbeidsoker(aktorId, Fnr.of(fnr))
        setLocalArenaOppfolging(aktorId, Formidlingsgruppe.ARBS)
        mockTiltakshistorikk(Fnr.of(fnr), harAktiveDeltakelser = false)
        mockUngdomsprogram(Fnr.of(fnr), erDeltaker = false)
        mockArbeidssoekerregisteret(Fnr.of(fnr), erArbeidssoeker = false)
        mockAap(Fnr.of(fnr), harAap = false)

        assertThat(kandidatForUtmeldingService.hentKandidatForUtmeldingTag(aktorId)).isNull()

        val sluttMelding = ConsumerRecord(
            "topic",
            0,
            0,
            "dummyKey",
            arbeidssokerperiode(
                fnr,
                periodeAvsluttet = true,
                avsluttetAarsakType = AvsluttetAarsakType.SVARTE_NEI_I_BEKREFTELSE
            )
        )
        arbeidssoekerperiodeConsumerService.consumeArbeidssøkerperiode(sluttMelding)

        assertThat(kandidatForUtmeldingService.hentKandidatForUtmeldingTag(aktorId))!!.isEqualTo(
            KandidatForUtmeldingTag.ARBEIDSSOKERPERIODE_AVSLUTTET_SVARTE_NEI_I_BEKREFTELSE
        )
    }

    @Test
    fun `AvsluttAarsakType BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST mappes til riktig KandidatForUtmeldingHendelseType ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT`() {
        mockSytemBrukerAuthOk(aktorId, Fnr.of(fnr))
        startOppfolgingSomArbeidsoker(aktorId, Fnr.of(fnr))
        setLocalArenaOppfolging(aktorId, Formidlingsgruppe.ARBS)
        mockTiltakshistorikk(Fnr.of(fnr), harAktiveDeltakelser = false)
        mockUngdomsprogram(Fnr.of(fnr), erDeltaker = false)
        mockArbeidssoekerregisteret(Fnr.of(fnr), erArbeidssoeker = false)
        mockAap(Fnr.of(fnr), harAap = false)

        assertThat(kandidatForUtmeldingService.hentKandidatForUtmeldingTag(aktorId)).isNull()

        val sluttMelding = ConsumerRecord(
            "topic",
            0,
            0,
            "dummyKey",
            arbeidssokerperiode(
                fnr,
                periodeAvsluttet = true,
                avsluttetAarsakType = AvsluttetAarsakType.BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST
            )
        )
        arbeidssoekerperiodeConsumerService.consumeArbeidssøkerperiode(sluttMelding)

        assertThat(kandidatForUtmeldingService.hentKandidatForUtmeldingTag(aktorId))!!.isEqualTo(
            KandidatForUtmeldingTag.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT
        )
    }

    @Test
    fun `skal kunne opprette forlengelse`() {
        val veilederId = UUID.randomUUID()
        val enhetId = EnhetId("1234")
        mockInternBrukerAuthOk(veilederId, aktorId, Fnr.of(fnr))
        mockPoaoTilgangHarTilgangTilBruker(veilederId, Fnr.of(fnr), Decision.Permit, TilgangType.SKRIVE)
        mockPoaoTilgangHarTilgangTilEnhet(veilederId, enhetId)
        startOppfolgingSomArbeidsoker(aktorId, Fnr.of(fnr))
        setLocalArenaOppfolging(aktorId, Formidlingsgruppe.ARBS)
        setAoKontor(Fnr.of(fnr), aktorId, enhetId.get())
        mockTiltakshistorikk(Fnr.of(fnr), harAktiveDeltakelser = false)
        mockUngdomsprogram(Fnr.of(fnr), erDeltaker = false)
        mockArbeidssoekerregisteret(Fnr.of(fnr), erArbeidssoeker = false)
        mockAap(Fnr.of(fnr), harAap = false)
        val oppfolgingsperiodeUuid = oppfolgingService.hentGjeldendeOppfolgingsperiode(Fnr.of(fnr)).get().uuid
        kandidatForUtmeldingRepository.lagreKandidat(
            ArbeidssøkerPeriodeAvsluttet(
                oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
                utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
                utfortAv = "A123123",
                kilde = "kilde",
                hendelseTidspunkt = ZonedDateTime.now().toInstant(),
                arbeidssokerperiodeAvsluttetHendelseType = ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
                avslutningsarsak = BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST.toString()
            )
        )

        val forlengelseDato = LocalDate.now().plusDays(30)

        forlengKandidatForUtmelding(fnr = Fnr.of(fnr), forlengTil = forlengelseDato)

        val kandidat = kandidatForUtmeldingRepository.hentKandidatMedForlengelse(oppfolgingsperiodeUuid)
        val forlengetTil = kandidatForUtmeldingRepository.hentForlengetTil(oppfolgingsperiodeUuid)
        assertThat(forlengetTil?.toLocalDateTime()?.toLocalDate()).isEqualTo(forlengelseDato)
        assertThat(kandidat).isNotNull
        assertThat(kandidat?.utfortAv).isEqualTo("A123456")
        assertThat(kandidat?.type).isEqualTo(ForlengelseHendelseType.FORLENGELSE_OPPRETTET)
        assertThat(kandidat?.hendelseDataJson?.value).isEqualTo(
            JsonUtils.getMapper()
                .writeValueAsString(ForlengelseHendelse.Detaljer(forlengelseDato))
        )
        val filterhendelseId = kandidatForUtmeldingRepository.hentFilterhendelseId(oppfolgingsperiodeUuid)
        assertThat(filterhendelseId).isNotNull
        val filterhendelse = getFilterhendelseRecordsStoredInKafkaOutbox(
            kafkaProperties.portefoljeHendelsesfilterTopic,
            filterhendelseId.toString()
        ).first()
        assertThat(filterhendelse.operasjon).isEqualTo(Operasjon.STOPP)
        assertThat(filterhendelse.kategori).isEqualTo(Kategori.KANDIDAT_FOR_UTMELDING)
        assertThat(filterhendelse.hendelse.beskrivelseEnum).isEqualTo(BeskrivelseEnum.FORLENGELSE_OPPRETTET.name)
    }

    @Test
    fun `skal kunne forlenge forlengelsen`() {
        val veilederId = UUID.randomUUID()
        val enhetId = EnhetId("1234")
        mockInternBrukerAuthOk(veilederId, aktorId, Fnr.of(fnr))
        mockPoaoTilgangHarTilgangTilBruker(veilederId, Fnr.of(fnr), Decision.Permit, TilgangType.SKRIVE)
        mockPoaoTilgangHarTilgangTilEnhet(veilederId, enhetId)
        startOppfolgingSomArbeidsoker(aktorId, Fnr.of(fnr))
        setLocalArenaOppfolging(aktorId, Formidlingsgruppe.ARBS)
        setAoKontor(Fnr.of(fnr), aktorId, enhetId.get())
        mockTiltakshistorikk(Fnr.of(fnr), harAktiveDeltakelser = false)
        mockUngdomsprogram(Fnr.of(fnr), erDeltaker = false)
        mockArbeidssoekerregisteret(Fnr.of(fnr), erArbeidssoeker = false)
        mockAap(Fnr.of(fnr), harAap = false)
        val oppfolgingsperiodeUuid = oppfolgingService.hentGjeldendeOppfolgingsperiode(Fnr.of(fnr)).get().uuid
        kandidatForUtmeldingRepository.lagreKandidat(
            ArbeidssøkerPeriodeAvsluttet(
                oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
                utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
                utfortAv = "A123123",
                kilde = "kilde",
                hendelseTidspunkt = ZonedDateTime.now().toInstant(),
                arbeidssokerperiodeAvsluttetHendelseType = ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
                avslutningsarsak = BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST.toString()
            )
        )

        val forlengelseDato = LocalDate.now().plusDays(30)
        forlengKandidatForUtmelding(fnr = Fnr.of(fnr), forlengTil = forlengelseDato)

        val nyForlengelseDato = LocalDate.now().plusDays(60)
        forlengKandidatForUtmelding(fnr = Fnr.of(fnr), forlengTil = nyForlengelseDato)

        val forlengetTil = kandidatForUtmeldingRepository.hentForlengetTil(oppfolgingsperiodeUuid)
        assertThat(forlengetTil?.toLocalDateTime()?.toLocalDate()).isEqualTo(nyForlengelseDato)

        val kandidat = kandidatForUtmeldingRepository.hentKandidatMedForlengelse(oppfolgingsperiodeUuid)
        assertThat(kandidat?.type).isEqualTo(ForlengelseHendelseType.FORLENGELSE_ENDRET)
    }

    @Test
    fun `skal avslutte oppfølging etter karensperiode er utløpt`() {
        val veilederId = UUID.randomUUID()
        val enhetId = EnhetId("1234")
        mockInternBrukerAuthOk(veilederId, aktorId, Fnr.of(fnr))
        mockPoaoTilgangHarTilgangTilBruker(veilederId, Fnr.of(fnr), Decision.Permit, TilgangType.SKRIVE)
        mockPoaoTilgangHarTilgangTilEnhet(veilederId, enhetId)
        startOppfolgingSomArbeidsoker(aktorId, Fnr.of(fnr))
        setLocalArenaOppfolging(aktorId, Formidlingsgruppe.ARBS)
        setAoKontor(Fnr.of(fnr), aktorId, enhetId.get())
        mockTiltakshistorikk(Fnr.of(fnr), harAktiveDeltakelser = false)
        mockUngdomsprogram(Fnr.of(fnr), erDeltaker = false)
        mockArbeidssoekerregisteret(Fnr.of(fnr), erArbeidssoeker = false)
        mockAap(Fnr.of(fnr), harAap = false)

        val hendelsetidspunkt = ZonedDateTime.now().minusDays(30)
        val oppfolgingsperiodeUuid = oppfolgingService.hentGjeldendeOppfolgingsperiode(Fnr.of(fnr)).get().uuid
        kandidatForUtmeldingRepository.lagreKandidat(
            ArbeidssøkerPeriodeAvsluttet(
                oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
                utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
                utfortAv = "A123123",
                kilde = "kilde",
                hendelseTidspunkt = hendelsetidspunkt.toInstant(),
                arbeidssokerperiodeAvsluttetHendelseType = ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
                avslutningsarsak = BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST.toString()
            )
        )

        val avslutningsDato = hendelsetidspunkt.plusDays(KARENSTID_DAGER)
        assertThat(avslutningsDato).isBefore(ZonedDateTime.now())

        val kandidatHendelse = kandidatForUtmeldingRepository.hentKandidat(oppfolgingsperiodeUuid)
        assertThat(kandidatHendelse?.type).isEqualTo(OppfolgingAvsluttetHendelseType.OPPFOLGING_AVSLUTTET_AUTOMATISK)
    }

    private fun arbeidssokerperiode(
        fodselsnummer: String,
        periodeAvsluttet: Boolean = false,
        periodeStartet: Instant = Instant.now().minusSeconds(1),
        avsluttetAarsakType: AvsluttetAarsakType = AvsluttetAarsakType.BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST
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
            avslutningsInfo = AvslutningsInfo().apply {
                aarsaksinformasjon = Aarsaksinformasjon().apply {
                    type = avsluttetAarsakType
                }
            }
        }
    }
}

