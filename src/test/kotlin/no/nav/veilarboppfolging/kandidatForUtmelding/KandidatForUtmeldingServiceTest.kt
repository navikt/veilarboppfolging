package no.nav.veilarboppfolging.kandidatForUtmelding

import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import no.nav.common.json.JsonUtils
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.paw.arbeidssokerregisteret.api.v1.AvsluttetAarsakType.BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.veilarboppfolging.IntegrationTest
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.BeskrivelseEnum
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.Kategori
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.Operasjon
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test

class KandidatForUtmeldingServiceTest : IntegrationTest() {

    private val AKTOR_ID = AktorId.of("1234567890")
    private val FNR = Fnr.of("12345678901")

    @Test
    fun `lagreKandidatForUtmelding lagrer kandidat i databasen når bruker kan avsluttes`() {
        mockSytemBrukerAuthOk(AKTOR_ID, FNR)
        setBrukerUnderOppfolging(AKTOR_ID, FNR)
        setLocalArenaOppfolging(AKTOR_ID, Formidlingsgruppe.ARBS)
        mockTiltakshistorikk(FNR, harAktiveDeltakelser = false)
        mockUngdomsprogram(FNR, erDeltaker = false)
        mockArbeidssoekerregisteret(FNR, erArbeidssoeker = false)
        mockAap(FNR, harAap = false)
        startOppfolgingSomArbeidsoker(AKTOR_ID, FNR)
        val oppfolgingsperiodeUuid = oppfolgingService.hentGjeldendeOppfolgingsperiode(FNR).get().uuid

        kandidatForUtmeldingService.lagreKandidatForUtmelding(
            FNR,
            ArbeidssøkerPeriodeAvsluttet(
                utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
                utfortAv = "A123123",
                kilde = "kilde",
                hendelseTidspunkt = ZonedDateTime.now().toInstant(),
                oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
                arbeidssokerperiodeAvsluttetHendelseType = ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
                avslutningsarsak = BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST.toString()
            )
        )

        val kandidat = kandidatForUtmeldingRepository.hentKandidat(oppfolgingsperiodeUuid)
        assertThat(kandidat).isNotNull
        assertThat(kandidat?.oppfolgingsperiodeUuid).isEqualTo(oppfolgingsperiodeUuid)
        assertThat(kandidat?.utfortAvType).isEqualTo(KandidatForUtmeldingHendelseUtfortAvType.VEILEDER)
        assertThat(kandidat?.kilde).isEqualTo("kilde")
        assertThat(kandidat?.hendelseDataJson?.value).isEqualTo(
            JsonUtils.getMapper()
                .writeValueAsString(ArbeidssøkerPeriodeAvsluttet.Detaljer(BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST.toString()))
        )
        val filterhendelseId = kandidatForUtmeldingRepository.hentFilterhendelseId(oppfolgingsperiodeUuid)
        assertThat(filterhendelseId).isNotNull
        val filterhendelse = getFilterhendelseRecordsStoredInKafkaOutbox(kafkaProperties.portefoljeHendelsesfilterTopic, filterhendelseId.toString()).first()
        assertThat(filterhendelse.operasjon).isEqualTo(Operasjon.START)
        assertThat(filterhendelse.kategori).isEqualTo(Kategori.KANDIDAT_FOR_UTMELDING)
        assertThat(filterhendelse.hendelse.beskrivelseEnum).isEqualTo(BeskrivelseEnum.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT.name)
        assertThat(filterhendelse.hendelse.datoFrist)
            .isCloseTo(kandidat?.beregnAvsluttesAutomatiskDatoZonedDateTime(), within(1, ChronoUnit.SECONDS))
    }

    @Test
    fun `lagreKandidatForUtmelding lagrer ikke kandidat i databasen når bruker ikke er under oppfølging`() {
        mockSytemBrukerAuthOk(AKTOR_ID, FNR)
        mockVeilarbArenaOppfolgingsBruker(FNR, Formidlingsgruppe.ARBS)
        mockTiltakshistorikk(FNR, harAktiveDeltakelser = false)
        mockUngdomsprogram(FNR, erDeltaker = false)
        mockArbeidssoekerregisteret(FNR, erArbeidssoeker = false)
        mockAap(FNR, harAap = false)

        val oppfolgingsperiodeId = UUID.randomUUID()

        kandidatForUtmeldingService.lagreKandidatForUtmelding(
            FNR,
            ArbeidssøkerPeriodeAvsluttet(
                utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
                utfortAv = "A123123",
                kilde = "kilde",
                hendelseTidspunkt = ZonedDateTime.now().toInstant(),
                arbeidssokerperiodeAvsluttetHendelseType = ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
                avslutningsarsak = BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST.toString(),
                oppfolgingsperiodeUuid = oppfolgingsperiodeId,
            )
        )

        assertThat(kandidatForUtmeldingRepository.hentKandidat(oppfolgingsperiodeId)).isNull()
        assertThat(kandidatForUtmeldingRepository.hentFilterhendelseId(oppfolgingsperiodeId)).isNull()
    }

    @Test
    fun `lagreKandidatForUtmelding lagrer ikke kandidat i databasen når bruker er registrert som arbeidssøker`() {
        mockSytemBrukerAuthOk(AKTOR_ID, FNR)
        setBrukerUnderOppfolging(AKTOR_ID, FNR)
        setLocalArenaOppfolging(AKTOR_ID, Formidlingsgruppe.ARBS)
        mockTiltakshistorikk(FNR, harAktiveDeltakelser = false)
        mockUngdomsprogram(FNR, erDeltaker = false)
        mockArbeidssoekerregisteret(FNR, erArbeidssoeker = true)
        mockAap(FNR, harAap = false)
        val oppfolgingsperiodeId = UUID.randomUUID()

        kandidatForUtmeldingService.lagreKandidatForUtmelding(
            FNR,
            ArbeidssøkerPeriodeAvsluttet(
                utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
                utfortAv = "A123123",
                kilde = "kilde",
                hendelseTidspunkt = ZonedDateTime.now().toInstant(),
                arbeidssokerperiodeAvsluttetHendelseType = ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
                avslutningsarsak = BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST.toString(),
                oppfolgingsperiodeUuid = oppfolgingsperiodeId,
            )
        )

        assertThat(kandidatForUtmeldingRepository.hentKandidat(oppfolgingsperiodeId)).isNull()
        assertThat(kandidatForUtmeldingRepository.hentFilterhendelseId(oppfolgingsperiodeId)).isNull()
    }

    @Test
    fun `behandleKandidaterMedUtloptForlengelse - forlengelse utløpt, kan avsluttes - sender til OBO`() {
        mockSytemBrukerAuthOk(AKTOR_ID, FNR)
        setBrukerUnderOppfolging(AKTOR_ID, FNR)
        setLocalArenaOppfolging(AKTOR_ID, Formidlingsgruppe.ARBS)
        mockTiltakshistorikk(FNR, harAktiveDeltakelser = false)
        mockUngdomsprogram(FNR, erDeltaker = false)
        mockArbeidssoekerregisteret(FNR, erArbeidssoeker = false)
        mockAap(FNR, harAap = false)
        startOppfolgingSomArbeidsoker(AKTOR_ID, FNR)
        val oppfolgingsperiodeUuid = oppfolgingService.hentGjeldendeOppfolgingsperiode(FNR).get().uuid
        val lagretKandidat = ArbeidssøkerPeriodeAvsluttet(
                utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
                utfortAv = "A123123",
                kilde = "kilde",
                hendelseTidspunkt = ZonedDateTime.now().toInstant(),
                oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
                arbeidssokerperiodeAvsluttetHendelseType = ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
                avslutningsarsak = BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST.toString()
            )
        kandidatForUtmeldingRepository.lagreKandidat(lagretKandidat)
        namedParameterJdbcTemplate.update("""
            UPDATE kandidater_for_utmelding SET forlenget_til = CURRENT_TIMESTAMP - INTERVAL '1 hour' WHERE oppfolgingsperiode_uuid = :oppfolgingsperiodeId
        """.trimIndent(), mapOf("oppfolgingsperiodeId" to oppfolgingsperiodeUuid.toString()))

        kandidatForUtmeldingService.behandleKandidaterMedUtloptForlengelse()

        val kandidat = kandidatForUtmeldingRepository.hentKandidat(oppfolgingsperiodeUuid)
        assertThat(kandidat).isNotNull
        assertThat(kandidat?.type).isEqualTo(ForlengelseHendelseType.FORLENGELSE_UTLOPT)
        assertThat(kandidatForUtmeldingRepository.hentKandidatMedForlengelse(oppfolgingsperiodeUuid)).isNull()
        val filterhendelseId = kandidatForUtmeldingRepository.hentFilterhendelseId(oppfolgingsperiodeUuid)
        assertThat(filterhendelseId).isNotNull
        val filterhendelse = getFilterhendelseRecordsStoredInKafkaOutbox(kafkaProperties.portefoljeHendelsesfilterTopic, filterhendelseId.toString()).first()
        assertThat(filterhendelse.operasjon).isEqualTo(Operasjon.START)
        assertThat(filterhendelse.kategori).isEqualTo(Kategori.KANDIDAT_FOR_UTMELDING)
        assertThat(filterhendelse.hendelse.datoFrist)
            .isCloseTo(kandidat?.beregnAvsluttesAutomatiskDatoZonedDateTime(), within(250, ChronoUnit.MILLIS))
    }

    @Test
    fun `behandleKandidaterMedUtloptForlengelse - forlengelse utløpt, kan ikke avsluttes - slettes`() {
        mockSytemBrukerAuthOk(AKTOR_ID, FNR)
        setBrukerUnderOppfolging(AKTOR_ID, FNR)
        setLocalArenaOppfolging(AKTOR_ID, Formidlingsgruppe.ARBS)
        mockTiltakshistorikk(FNR, harAktiveDeltakelser = false)
        mockUngdomsprogram(FNR, erDeltaker = false)
        mockArbeidssoekerregisteret(FNR, erArbeidssoeker = false)
        mockAap(FNR, harAap = true)
        startOppfolgingSomArbeidsoker(AKTOR_ID, FNR)
        val oppfolgingsperiodeUuid = oppfolgingService.hentGjeldendeOppfolgingsperiode(FNR).get().uuid
        val lagretKandidat = ArbeidssøkerPeriodeAvsluttet(
            utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
            utfortAv = "A123123",
            kilde = "kilde",
            hendelseTidspunkt = ZonedDateTime.now().toInstant(),
            oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
            arbeidssokerperiodeAvsluttetHendelseType = ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
            avslutningsarsak = BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST.toString()
        )
        kandidatForUtmeldingRepository.lagreKandidat(lagretKandidat)
        namedParameterJdbcTemplate.update("""
            UPDATE kandidater_for_utmelding SET forlenget_til = CURRENT_TIMESTAMP - INTERVAL '1 hour' WHERE oppfolgingsperiode_uuid = :oppfolgingsperiodeId
        """.trimIndent(), mapOf("oppfolgingsperiodeId" to oppfolgingsperiodeUuid.toString()))

        kandidatForUtmeldingService.behandleKandidaterMedUtloptForlengelse()

        val kandidat = kandidatForUtmeldingRepository.hentKandidat(oppfolgingsperiodeUuid)
        assertThat(kandidat).isNull()
        assertThat(kandidatForUtmeldingRepository.hentKandidatMedForlengelse(oppfolgingsperiodeUuid)).isNull()
        val filterhendelseId = kandidatForUtmeldingRepository.hentFilterhendelseId(oppfolgingsperiodeUuid)
        assertThat(filterhendelseId).isNull()
        val filterhendelse = getFilterhendelseRecordsStoredInKafkaOutbox(kafkaProperties.portefoljeHendelsesfilterTopic, filterhendelseId.toString()).firstOrNull()
        assertThat(filterhendelse).isNull()
    }
}

