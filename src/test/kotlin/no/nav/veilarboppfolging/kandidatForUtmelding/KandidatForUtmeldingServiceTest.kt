package no.nav.veilarboppfolging.kandidatForUtmelding

import java.util.UUID
import no.nav.common.json.JsonUtils
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.paw.arbeidssokerregisteret.api.v1.AvsluttetAarsakType.BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.veilarboppfolging.IntegrationTest
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.Kategori
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.Operasjon
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.UtmeldingsKandidatDetaljer
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.UtmeldingsService
import no.nav.veilarboppfolging.repository.UtmeldingRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class KandidatForUtmeldingServiceTest : IntegrationTest() {

    private val AKTOR_ID = AktorId.of("1234567890")
    private val FNR = Fnr.of("12345678901")

    @Autowired
    lateinit var utmeldingsService: UtmeldingsService

    @Autowired
    lateinit var utmeldingRepository: UtmeldingRepository

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
                oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
                kandidatForUtmeldingHendelseType = KandidatForUtmeldingHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
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
        assertThat(filterhendelse.hendelse.utmeldingsKandidatDetaljer).isEqualTo(UtmeldingsKandidatDetaljer.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT)
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
                kandidatForUtmeldingHendelseType = KandidatForUtmeldingHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
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
                kandidatForUtmeldingHendelseType = KandidatForUtmeldingHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
                avslutningsarsak = BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST.toString(),
                oppfolgingsperiodeUuid = oppfolgingsperiodeId,
            )
        )

        assertThat(kandidatForUtmeldingRepository.hentKandidat(oppfolgingsperiodeId)).isNull()
        assertThat(kandidatForUtmeldingRepository.hentFilterhendelseId(oppfolgingsperiodeId)).isNull()
    }

    @Test
    fun `fjernKandidatForUtmelding fjerner kandidat fra databasen`() {
        startOppfolgingSomArbeidsoker(AKTOR_ID, FNR)
        val oppfolgingsperiodeUuid = oppfolgingService.hentGjeldendeOppfolgingsperiode(FNR).get().uuid
        kandidatForUtmeldingRepository.lagreKandidat(
            ArbeidssøkerPeriodeAvsluttet(
                utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
                utfortAv = "A123123",
                kilde = "kilde",
                kandidatForUtmeldingHendelseType = KandidatForUtmeldingHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
                avslutningsarsak = BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST.toString(),
                oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
            )
        )
        assertThat(kandidatForUtmeldingRepository.hentKandidat(oppfolgingsperiodeUuid)).isNotNull()

        kandidatForUtmeldingService.fjernKandidatForUtmelding(oppfolgingsperiodeUuid)

        assertThat(kandidatForUtmeldingRepository.hentKandidat(oppfolgingsperiodeUuid)).isNull()
    }

    @Test
    fun `fjernKandidatForUtmelding feiler ikke når kandidat ikke finnes i databasen`() {
        val oppfolgingsperiodeId = UUID.randomUUID()
        assertThat(kandidatForUtmeldingRepository.hentKandidat(oppfolgingsperiodeId)).isNull()

        kandidatForUtmeldingService.fjernKandidatForUtmelding(oppfolgingsperiodeId)

        assertThat(kandidatForUtmeldingRepository.hentKandidat(oppfolgingsperiodeId)).isNull()
    }
}

