package no.nav.veilarboppfolging.kandidatForUtmelding

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import no.nav.common.client.aktoroppslag.BrukerIdenter
import no.nav.common.json.JsonUtils
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NavIdent
import no.nav.common.types.identer.NorskIdent
import no.nav.paw.arbeidssokerregisteret.api.v1.Aarsaksinformasjon
import no.nav.paw.arbeidssokerregisteret.api.v1.AvslutningsInfo
import no.nav.paw.arbeidssokerregisteret.api.v1.AvsluttetAarsakType
import no.nav.paw.arbeidssokerregisteret.api.v1.AvsluttetAarsakType.BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST
import no.nav.paw.arbeidssokerregisteret.api.v1.AvviksType
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.TidspunktFraKilde
import no.nav.poao_tilgang.client.Decision
import no.nav.poao_tilgang.client.TilgangType
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe
import no.nav.veilarboppfolging.IntegrationTest
import no.nav.veilarboppfolging.ident.randomAktorId
import no.nav.veilarboppfolging.ident.randomFnr
import no.nav.veilarboppfolging.kafka.ArbeidssøkerperiodeConsumerService
import no.nav.veilarboppfolging.kafka.TestUtils
import no.nav.veilarboppfolging.kandidatForUtmelding.dto.KandidatForUtmeldingTagDto
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.BeskrivelseEnum
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.FilterhendelseRecord
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.Kategori
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.Operasjon
import no.nav.veilarboppfolging.oppfolgingsbruker.VeilederRegistrant
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingsRegistrering
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingsRegistrering.Companion.arbeidssokerRegistrering
import no.nav.veilarboppfolging.repository.UtmeldingRepository
import no.nav.veilarboppfolging.service.KafkaConsumerService
import no.nav.veilarboppfolging.service.ReaktiveringService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
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
    val reaktiveringService: ReaktiveringService,
) : IntegrationTest() {

    private fun mockIdents(fnr: Fnr, aktorId: AktorId)  {
        `when`(aktorOppslagClient.hentAktorId(fnr)).thenReturn(aktorId)
        `when`(aktorOppslagClient.hentFnr(aktorId)).thenReturn(fnr)
        `when`(aktorOppslagClient.hentIdenter(fnr)).thenReturn(
            BrukerIdenter(
                fnr,
                aktorId,
                emptyList(),
                emptyList()
            )
        )
    }

    @Test
    fun `lagreKandidatForUtmelding blir kalt når arbeidssøkerperiode avsluttes og bruker kan avsluttes`() {
        val fnr = randomFnr()
        val aktorId = randomAktorId()
        mockIdents(fnr, aktorId)
        mockSytemBrukerAuthOk(aktorId, fnr)
        startOppfolgingSomArbeidsoker(aktorId, fnr)
        setLocalArenaOppfolging(aktorId, Formidlingsgruppe.ARBS)
        mockTiltakshistorikk(fnr, harAktiveDeltakelser = false)
        mockUngdomsprogram(fnr, erDeltaker = false)
        mockArbeidssoekerregisteret(fnr, erArbeidssoeker = false)
        mockAap(fnr, harAap = false)

        assertThat(kandidatForUtmeldingService.hentKandidatForUtmeldingTag(aktorId)).isNull()

        val sluttMelding = ConsumerRecord("topic", 0, 0, "dummyKey", arbeidssokerperiode(fnr.get(), periodeAvsluttet = true))
        arbeidssoekerperiodeConsumerService.consumeArbeidssøkerperiode(sluttMelding)

        assertThat(kandidatForUtmeldingService.hentKandidatForUtmeldingTag(aktorId)).isNotNull()
    }

    @Test
    fun `lagreKandidatForUtmelding blir kalt når bruker blir ISERV etter arbeidssøkerregistrering`() {
        val fnr = randomFnr()
        val aktorId = randomAktorId()
        mockIdents(fnr, aktorId)
        mockSytemBrukerAuthOk(aktorId, fnr)
        val arbeidsoekerPeriodeStartet = LocalDateTime.of(2024, 10, 1, 23, 59)
        val ISERV_FRA_DATO = LocalDate.of(2024, 10, 2)
        mockVeilarbArenaOppfolgingsBruker(
            fnr,
            Formidlingsgruppe.ISERV,
            iservFraDato = ISERV_FRA_DATO.atStartOfDay(ZoneId.systemDefault())
        )
        mockTiltakshistorikk(fnr, harAktiveDeltakelser = false)
        mockUngdomsprogram(fnr, erDeltaker = false)
        mockArbeidssoekerregisteret(fnr, erArbeidssoeker = false)
        mockAap(fnr, harAap = false)

        val nyPeriode = arbeidssokerperiode(
            fnr.get(),
            periodeStartet = arbeidsoekerPeriodeStartet.atZone(ZoneId.systemDefault()).toInstant()
        )
        val oppfolginsBrukerEndretTilISERV = ConsumerRecord(
            "topic", 0, 0, "key", TestUtils.oppfølgingsBrukerEndret(
                fnr.get(), iservFraDato = ISERV_FRA_DATO, formidlingsgruppe = Formidlingsgruppe.ISERV
            )
        )

        kafkaConsumerService.consumeEndringPaOppfolgingBruker(oppfolginsBrukerEndretTilISERV)

        val sluttMelding = ConsumerRecord(
            "topic",
            0,
            0,
            "dummyKey",
            arbeidssokerperiode(
                fnr.get(),
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
        val fnr = randomFnr()
        val aktorId = randomAktorId()
        mockIdents(fnr, aktorId)
        mockVeilarbArenaOppfolgingsBruker(fnr, Formidlingsgruppe.ISERV)
        startOppfolgingSomArbeidsoker(aktorId, fnr)
        val oppfolgingsperiodeUuid = oppfolgingService.hentGjeldendeOppfolgingsperiode(fnr).get().uuid
        kandidatForUtmeldingRepository.lagreKandidat(
            ArbeidssøkerPeriodeAvsluttet(
                oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
                utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
                utfortAv = "A123123",
                kilde = "arbeidssøkerregisteret",
                hendelseTidspunkt = ZonedDateTime.now().toInstant(),
                arbeidssokerperiodeAvsluttetHendelseType = ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
                avslutningsarsak = BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST.toString()
            ).let { KandidatForUtmelding.fromHendelse(it) }
        )
        avsluttOppfolgingManueltSomVeileder(aktorId)

        val registrering = OppfolgingsRegistrering.manuellRegistreringVeileder(
            fnr,
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
        val fnr = randomFnr()
        val aktorId = randomAktorId()
        mockIdents(fnr, aktorId)
        mockVeilarbArenaOppfolgingsBruker(fnr, Formidlingsgruppe.ISERV)
        startOppfolgingSomArbeidsoker(aktorId, fnr)
        val oppfolgingsperiodeUuid = oppfolgingService.hentGjeldendeOppfolgingsperiode(fnr).get().uuid
        kandidatForUtmeldingRepository.lagreKandidat(
            ArbeidssøkerPeriodeAvsluttet(
                oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
                utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
                utfortAv = "A123123",
                kilde = "kilde",
                hendelseTidspunkt = ZonedDateTime.now().toInstant(),
                arbeidssokerperiodeAvsluttetHendelseType = ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
                avslutningsarsak = BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST.toString()
            ).let { KandidatForUtmelding.fromHendelse(it) }
        )

        avsluttOppfolgingManueltSomVeileder(aktorId)

        val registrering = OppfolgingsRegistrering.manuellRegistreringBruker(fnr, aktorId)
        startOppfolging(aktorId, registrering)

        assertThat(kandidatForUtmeldingService.hentKandidatForUtmeldingTag(aktorId)).isNull()
    }

    @Test
    fun `Sletter kandidat-for-utmelding når ny oppfølgingsperiode avsluttes manuelt av veileder`() {
        val fnr = randomFnr()
        val aktorId = randomAktorId()
        mockIdents(fnr, aktorId)
        mockVeilarbArenaOppfolgingsBruker(fnr, Formidlingsgruppe.ISERV)
        startOppfolgingSomArbeidsoker(aktorId, fnr)
        val oppfolgingsperiodeUuid = oppfolgingService.hentGjeldendeOppfolgingsperiode(fnr).get().uuid
        kandidatForUtmeldingRepository.lagreKandidat(
            ArbeidssøkerPeriodeAvsluttet(
                oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
                utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
                utfortAv = "A123123",
                kilde = "kilde",
                hendelseTidspunkt = ZonedDateTime.now().toInstant(),
                arbeidssokerperiodeAvsluttetHendelseType = ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
                avslutningsarsak = BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST.toString()
            ).let { KandidatForUtmelding.fromHendelse(it) }
        )

        avsluttOppfolgingManueltSomVeileder(aktorId)
        val registrering = arbeidssokerRegistrering(fnr, aktorId, VeilederRegistrant(NavIdent("veileder")))
        startOppfolging(aktorId, registrering)

        assertThat(kandidatForUtmeldingService.hentKandidatForUtmeldingTag(aktorId)).isNull()
    }

    @Test
    fun `Sletter kandidat-for-utmelding hvis bruker er under oppfølging og starter ny arbeidssøkerperiode`() {
        val fnr = randomFnr()
        val aktorId = randomAktorId()
        mockIdents(fnr, aktorId)
        mockVeilarbArenaOppfolgingsBruker(fnr, Formidlingsgruppe.ISERV)
        startOppfolgingSomArbeidsoker(aktorId, fnr)
        val oppfolgingsperiodeUuid = oppfolgingService.hentGjeldendeOppfolgingsperiode(fnr).get().uuid
        kandidatForUtmeldingRepository.lagreKandidat(
            ArbeidssøkerPeriodeAvsluttet(
                oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
                utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
                utfortAv = "A123123",
                kilde = "kilde",
                hendelseTidspunkt = ZonedDateTime.now().toInstant(),
                arbeidssokerperiodeAvsluttetHendelseType = ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
                avslutningsarsak = BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST.toString()
            ).let { KandidatForUtmelding.fromHendelse(it) }
        )

        assertThat(kandidatForUtmeldingService.hentKandidatForUtmeldingTag(aktorId)).isNotNull()

        val nyPeriode = arbeidssokerperiode(
            fnr.get(),
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
        val fnr = randomFnr()
        val aktorId = randomAktorId()
        mockIdents(fnr, aktorId)
        mockVeilarbArenaOppfolgingsBruker(fnr, Formidlingsgruppe.ISERV)
        startOppfolgingSomArbeidsoker(aktorId, fnr)
        val oppfolgingsperiodeUuid = oppfolgingService.hentGjeldendeOppfolgingsperiode(fnr).get().uuid
        kandidatForUtmeldingRepository.lagreKandidat(
            ArbeidssøkerPeriodeAvsluttet(
                oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
                utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
                utfortAv = "A123123",
                kilde = "kilde",
                hendelseTidspunkt = ZonedDateTime.now().toInstant(),
                arbeidssokerperiodeAvsluttetHendelseType = ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
                avslutningsarsak = BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST.toString()
            ).let { KandidatForUtmelding.fromHendelse(it) }
        )
        avsluttOppfolgingManueltSomVeileder(aktorId)

        val registrering = OppfolgingsRegistrering.arenaSyncOppfolgingBrukerRegistrering(
            fnr, aktorId,
            Formidlingsgruppe.IARBS, Kvalifiseringsgruppe.VURDU
        )
        startOppfolging(aktorId, registrering)

        assertThat(kandidatForUtmeldingService.hentKandidatForUtmeldingTag(aktorId)).isNull()
    }

    @Test
    fun `Sletter kandidat-for-utmelding når ny oppfølgingsperiode reaktiveres`() {
        val fnr = randomFnr()
        val aktorId = randomAktorId()
        mockIdents(fnr, aktorId)
        mockVeilarbArenaOppfolgingsBruker(fnr, Formidlingsgruppe.ISERV)
        startOppfolgingSomArbeidsoker(aktorId, fnr)
        mockInternBrukerAuthOk(UUID.randomUUID(), aktorId, fnr)
        mockArenaOppfolgingServiceRegistrerIkkeArbeidssoker(fnr)
        val oppfolgingsperiodeUuid = oppfolgingService.hentGjeldendeOppfolgingsperiode(fnr).get().uuid
        kandidatForUtmeldingRepository.lagreKandidat(
            ArbeidssøkerPeriodeAvsluttet(
                oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
                utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
                utfortAv = "A123123",
                kilde = "kilde",
                hendelseTidspunkt = ZonedDateTime.now().toInstant(),
                arbeidssokerperiodeAvsluttetHendelseType = ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
                avslutningsarsak = BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST.toString()
            ).let { KandidatForUtmelding.fromHendelse(it) }
        )

        assertThat(kandidatForUtmeldingService.hentKandidatForUtmeldingTag(aktorId)).isNotNull()

        reaktiveringService.reaktiverBrukerIArena(fnr)

        assertThat(kandidatForUtmeldingService.hentKandidatForUtmeldingTag(aktorId)).isNull()
    }

    @Test
    fun `AvsluttAarsakType SVARTE_NEI_I_BEKREFTELSE mappes til riktig KandidatForUtmeldingHendelseType ARBEIDSSOKERPERIODE_AVSLUTTET_SVARTE_NEI_I_BEKREFTELSE`() {
        val fnr = randomFnr()
        val aktorId = randomAktorId()
        mockIdents(fnr, aktorId)
        mockSytemBrukerAuthOk(aktorId, fnr)
        startOppfolgingSomArbeidsoker(aktorId, fnr)
        setLocalArenaOppfolging(aktorId, Formidlingsgruppe.ARBS)
        mockTiltakshistorikk(fnr, harAktiveDeltakelser = false)
        mockUngdomsprogram(fnr, erDeltaker = false)
        mockArbeidssoekerregisteret(fnr, erArbeidssoeker = false)
        mockAap(fnr, harAap = false)

        assertThat(kandidatForUtmeldingService.hentKandidatForUtmeldingTag(aktorId)).isNull()

        val sluttMelding = ConsumerRecord(
            "topic",
            0,
            0,
            "dummyKey",
            arbeidssokerperiode(
                fnr.get(),
                periodeAvsluttet = true,
                avsluttetAarsakType = AvsluttetAarsakType.SVARTE_NEI_I_BEKREFTELSE
            )
        )
        arbeidssoekerperiodeConsumerService.consumeArbeidssøkerperiode(sluttMelding)

        assertThat(kandidatForUtmeldingService.hentKandidatForUtmeldingTag(aktorId))!!.isEqualTo(
            KandidatForUtmeldingTagDto.ARBEIDSSOKERPERIODE_AVSLUTTET_SVARTE_NEI_I_BEKREFTELSE
        )
    }

    @Test
    fun `AvsluttAarsakType BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST mappes til riktig KandidatForUtmeldingHendelseType ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT`() {
        val fnr = randomFnr()
        val aktorId = randomAktorId()
        mockIdents(fnr, aktorId)
        mockSytemBrukerAuthOk(aktorId, fnr)
        startOppfolgingSomArbeidsoker(aktorId, fnr)
        setLocalArenaOppfolging(aktorId, Formidlingsgruppe.ARBS)
        mockTiltakshistorikk(fnr, harAktiveDeltakelser = false)
        mockUngdomsprogram(fnr, erDeltaker = false)
        mockArbeidssoekerregisteret(fnr, erArbeidssoeker = false)
        mockAap(fnr, harAap = false)

        assertThat(kandidatForUtmeldingService.hentKandidatForUtmeldingTag(aktorId)).isNull()

        val sluttMelding = ConsumerRecord(
            "topic",
            0,
            0,
            "dummyKey",
            arbeidssokerperiode(
                fnr.get(),
                periodeAvsluttet = true,
                avsluttetAarsakType = BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST
            )
        )
        arbeidssoekerperiodeConsumerService.consumeArbeidssøkerperiode(sluttMelding)

        assertThat(kandidatForUtmeldingService.hentKandidatForUtmeldingTag(aktorId))!!
            .isEqualTo(KandidatForUtmeldingTagDto.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT)
    }

    @Test
    fun `skal kunne opprette forlengelse`() {
        val fnr = randomFnr()
        val veilederId = UUID.randomUUID()
        val enhetId = EnhetId("1234")
        val aktorId = randomAktorId()
        mockIdents(fnr, aktorId)
        mockInternBrukerAuthOk(veilederId, aktorId, fnr)
        mockPoaoTilgangHarTilgangTilBruker(veilederId, fnr, Decision.Permit, TilgangType.SKRIVE)
        mockPoaoTilgangHarTilgangTilEnhet(veilederId, enhetId)
        startOppfolgingSomArbeidsoker(aktorId, fnr)
        setLocalArenaOppfolging(aktorId, Formidlingsgruppe.ARBS)
        setAoKontor(fnr, aktorId, enhetId.get())
        mockTiltakshistorikk(fnr, harAktiveDeltakelser = false)
        mockUngdomsprogram(fnr, erDeltaker = false)
        mockArbeidssoekerregisteret(fnr, erArbeidssoeker = false)
        mockAap(fnr, harAap = false)
        val oppfolgingsperiodeUuid = oppfolgingService.hentGjeldendeOppfolgingsperiode(fnr).get().uuid
        kandidatForUtmeldingRepository.lagreKandidat(
            ArbeidssøkerPeriodeAvsluttet(
                oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
                utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
                utfortAv = "A123123",
                kilde = "kilde",
                hendelseTidspunkt = ZonedDateTime.now().toInstant(),
                arbeidssokerperiodeAvsluttetHendelseType = ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
                avslutningsarsak = BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST.toString()
            ).let { KandidatForUtmelding.fromHendelse(it) }
        )

        val forlengelseDato = LocalDate.now().plusDays(30)

        forlengKandidatForUtmelding(fnr = fnr, forlengTil = forlengelseDato)

        val kandidat = kandidatForUtmeldingRepository.hentKandidatMedForlengelse(oppfolgingsperiodeUuid)
        val forlengetTil = kandidat?.forlengetTil
        val sisteHendelse = kandidat?.sisteHendelse
        assertThat(forlengetTil).isEqualTo(forlengelseDato)
        assertThat(sisteHendelse).isNotNull
        assertThat(sisteHendelse?.utfortAv).isEqualTo("A123456")
        assertThat(sisteHendelse?.type).isEqualTo(ForlengelseHendelseType.FORLENGELSE_OPPRETTET)
        assertThat(sisteHendelse?.hendelseDataJson?.value).isEqualTo(
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
        assertThat(filterhendelse.hendelse?.beskrivelseEnum).isEqualTo(BeskrivelseEnum.FORLENGELSE_OPPRETTET.name)
        assertThat(filterhendelse.hendelse?.datoFrist).isNull()
    }

    @Test
    fun `skal kunne forlenge forlengelsen`() {
        val fnr = randomFnr()
        val veilederId = UUID.randomUUID()
        val enhetId = EnhetId("1234")
        val aktorId = randomAktorId()
        mockIdents(fnr, aktorId)
        mockInternBrukerAuthOk(veilederId, aktorId, fnr)
        mockPoaoTilgangHarTilgangTilBruker(veilederId, fnr, Decision.Permit, TilgangType.SKRIVE)
        mockPoaoTilgangHarTilgangTilEnhet(veilederId, enhetId)
        startOppfolgingSomArbeidsoker(aktorId, fnr)
        setLocalArenaOppfolging(aktorId, Formidlingsgruppe.ARBS)
        setAoKontor(fnr, aktorId, enhetId.get())
        mockTiltakshistorikk(fnr, harAktiveDeltakelser = false)
        mockUngdomsprogram(fnr, erDeltaker = false)
        mockArbeidssoekerregisteret(fnr, erArbeidssoeker = false)
        mockAap(fnr, harAap = false)
        val oppfolgingsperiodeUuid = oppfolgingService.hentGjeldendeOppfolgingsperiode(fnr).get().uuid
        kandidatForUtmeldingRepository.lagreKandidat(
            ArbeidssøkerPeriodeAvsluttet(
                oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
                utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
                utfortAv = "A123123",
                kilde = "kilde",
                hendelseTidspunkt = ZonedDateTime.now().toInstant(),
                arbeidssokerperiodeAvsluttetHendelseType = ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
                avslutningsarsak = BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST.toString()
            ).let { KandidatForUtmelding.fromHendelse(it) }
        )

        val forlengelseDato = LocalDate.now().plusDays(30)
        forlengKandidatForUtmelding(fnr = fnr, forlengTil = forlengelseDato)

        val nyForlengelseDato = LocalDate.now().plusDays(60)
        forlengKandidatForUtmelding(fnr = fnr, forlengTil = nyForlengelseDato)

        val forlengetTil = kandidatForUtmeldingRepository.hentForlengetTil(oppfolgingsperiodeUuid)
        assertThat(forlengetTil?.toLocalDateTime()?.toLocalDate()).isEqualTo(nyForlengelseDato)

        val kandidat = kandidatForUtmeldingRepository.hentKandidatMedForlengelse(oppfolgingsperiodeUuid)
        assertThat(kandidat?.sisteHendelse?.type).isEqualTo(ForlengelseHendelseType.FORLENGELSE_ENDRET)
    }

    @Test
    @Disabled("WIP automatisk avslutning")
    fun `skal avslutte oppfølging etter karensperiode er utløpt`() {
        val fnr = randomFnr()
        val veilederId = UUID.randomUUID()
        val enhetId = EnhetId("1234")
        val aktorId = randomAktorId()
        mockIdents(fnr, aktorId)
        mockInternBrukerAuthOk(veilederId, aktorId, fnr)
        mockPoaoTilgangHarTilgangTilBruker(veilederId, fnr, Decision.Permit, TilgangType.SKRIVE)
        mockPoaoTilgangHarTilgangTilEnhet(veilederId, enhetId)
        startOppfolgingSomArbeidsoker(aktorId, fnr)
        setLocalArenaOppfolging(aktorId, Formidlingsgruppe.ISERV)
        setAoKontor(fnr, aktorId, enhetId.get())
        mockTiltakshistorikk(fnr, harAktiveDeltakelser = false)
        mockUngdomsprogram(fnr, erDeltaker = false)
        mockArbeidssoekerregisteret(fnr, erArbeidssoeker = false)
        mockAap(fnr, harAap = false)

        val hendelsetidspunkt = ZonedDateTime.now().minusDays(30)
        val oppfolgingsperiodeUuid = oppfolgingService.hentGjeldendeOppfolgingsperiode(fnr).get().uuid
        kandidatForUtmeldingRepository.lagreKandidat(
            ArbeidssøkerPeriodeAvsluttet(
                oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
                utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
                utfortAv = "A123123",
                kilde = "kilde",
                hendelseTidspunkt = hendelsetidspunkt.toInstant(),
                arbeidssokerperiodeAvsluttetHendelseType = ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
                avslutningsarsak = BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST.toString()
            ).let { KandidatForUtmelding.fromHendelse(it) }
        )
        val kandidat = kandidatForUtmeldingRepository.hentKandidat(oppfolgingsperiodeUuid)!!
        assertThat(kandidat.avsluttesAutomatiskDato).isBeforeOrEqualTo(ZonedDateTime.now().toLocalDateTime())

        // kandidatForUtmeldingService.kastUtKandidaterSomHarVærtKandidatForLenge()

        // Skal sende riktig hendelse på kafka
        val key = kandidatForUtmeldingRepository.hentFilterhendelseId(oppfolgingsperiodeUuid)
        val kafkaMeldinger = getFilterhendelseRecordsStoredInKafkaOutbox(kafkaProperties.portefoljeHendelsesfilterTopic, key.toString())
        assertThat(kafkaMeldinger).hasSize(1)
        assertThat(kafkaMeldinger.first()).isEqualTo(FilterhendelseRecord(
            NorskIdent.of(fnr.get()),
            "veilarboppfolging",
            Kategori.KANDIDAT_FOR_UTMELDING,
            Operasjon.STOPP,
            null
        ))

        // Skal lagre hendelse på kandidat
        val kandidatHendelse = kandidatForUtmeldingRepository.hentSisteHendelseForKandidat(oppfolgingsperiodeUuid)
        assertThat(kandidatHendelse?.type).isEqualTo(OppfolgingAvsluttetHendelseType.OPPFOLGING_AVSLUTTET_AUTOMATISK)
    }

    private fun arbeidssokerperiode(
        fodselsnummer: String,
        periodeAvsluttet: Boolean = false,
        periodeStartet: Instant = Instant.now().minusSeconds(1),
        avsluttetAarsakType: AvsluttetAarsakType = BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST
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

