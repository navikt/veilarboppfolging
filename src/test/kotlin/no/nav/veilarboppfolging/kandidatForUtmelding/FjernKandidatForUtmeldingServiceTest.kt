package no.nav.veilarboppfolging.kandidatForUtmelding

import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.paw.arbeidssokerregisteret.api.v1.AvsluttetAarsakType.BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.veilarboppfolging.IntegrationTest
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.Kategori
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.Operasjon
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FjernKandidatForUtmeldingServiceTest : IntegrationTest() {

    private val AKTOR_ID = AktorId.of("1234567811")
    private val FNR = Fnr.of("12345678912")

    @Test
    fun `fjernKandidatForUtmelding fjerner kandidat fra databasen og skriver til kafka`() {
        mockSytemBrukerAuthOk(AKTOR_ID, FNR)
        setBrukerUnderOppfolging(AKTOR_ID, FNR)
        setLocalArenaOppfolging(AKTOR_ID, Formidlingsgruppe.ARBS)
        mockTiltakshistorikk(FNR, harAktiveDeltakelser = false)
        mockUngdomsprogram(FNR, erDeltaker = false)
        mockArbeidssoekerregisteret(FNR, erArbeidssoeker = false)
        mockAap(FNR, harAap = false)
        startOppfolgingSomArbeidsoker(AKTOR_ID, FNR)
        val oppfolgingsperiodeUuid = oppfolgingService.hentGjeldendeOppfolgingsperiode(FNR).get().uuid
        kandidatForUtmeldingRepository.lagreKandidat(
            ArbeidssøkerPeriodeAvsluttet(
                utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
                utfortAv = "A123123",
                kilde = "kilde",
                hendelseTidspunkt = ZonedDateTime.now().toInstant(),
                arbeidssokerperiodeAvsluttetHendelseType = ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
                avslutningsarsak = BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST.toString(),
                oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
            )
        )
        val filterkategoriPersonId = kandidatForUtmeldingRepository.hentEllerOpprettFilterhendelseId(oppfolgingsperiodeUuid)
        assertThat(kandidatForUtmeldingRepository.hentKandidat(oppfolgingsperiodeUuid)).isNotNull()

        fjernKandidatForUtmeldingService.fjernKandidatForUtmelding(oppfolgingsperiodeUuid)

        assertThat(kandidatForUtmeldingRepository.hentKandidat(oppfolgingsperiodeUuid)).isNull()
        val filterhendelse = getFilterhendelseRecordsStoredInKafkaOutbox(kafkaProperties.portefoljeHendelsesfilterTopic, filterkategoriPersonId.toString()).first()
        assertThat(filterhendelse.operasjon).isEqualTo(Operasjon.STOPP)
        assertThat(filterhendelse.kategori).isEqualTo(Kategori.KANDIDAT_FOR_UTMELDING)
    }

    @Test
    fun `fjernKandidatForUtmelding feiler ikke når kandidat ikke finnes i databasen`() {
        mockSytemBrukerAuthOk(AKTOR_ID, FNR)
        setBrukerUnderOppfolging(AKTOR_ID, FNR)
        setLocalArenaOppfolging(AKTOR_ID, Formidlingsgruppe.ARBS)
        mockTiltakshistorikk(FNR, harAktiveDeltakelser = false)
        mockUngdomsprogram(FNR, erDeltaker = false)
        mockArbeidssoekerregisteret(FNR, erArbeidssoeker = false)
        mockAap(FNR, harAap = false)
        val oppfolgingsperiodeId = UUID.randomUUID()
        assertThat(kandidatForUtmeldingRepository.hentKandidat(oppfolgingsperiodeId)).isNull()

        fjernKandidatForUtmeldingService.fjernKandidatForUtmelding(oppfolgingsperiodeId)

        assertThat(kandidatForUtmeldingRepository.hentKandidat(oppfolgingsperiodeId)).isNull()
    }

    @Test
    fun `erOppfolgingForlenget - bruker er ikke kandidat - returnerer false`() {
        mockSytemBrukerAuthOk(AKTOR_ID, FNR)
        setBrukerUnderOppfolging(AKTOR_ID, FNR)
        setLocalArenaOppfolging(AKTOR_ID, Formidlingsgruppe.ARBS)
        mockTiltakshistorikk(FNR, harAktiveDeltakelser = false)
        mockUngdomsprogram(FNR, erDeltaker = false)
        mockArbeidssoekerregisteret(FNR, erArbeidssoeker = false)
        mockAap(FNR, harAap = false)
        startOppfolgingSomArbeidsoker(AKTOR_ID, FNR)
        val oppfolgingsperiodeUuid = oppfolgingService.hentGjeldendeOppfolgingsperiode(FNR).get().uuid

        val erForlenget = fjernKandidatForUtmeldingService.erOppfolgingForlenget(oppfolgingsperiodeUuid)

        assertThat(erForlenget).isFalse()
    }

    @Test
    fun `erOppfolgingForlenget - bruker er kandidat, ikke forlenget - returnerer false`() {
        mockSytemBrukerAuthOk(AKTOR_ID, FNR)
        setBrukerUnderOppfolging(AKTOR_ID, FNR)
        setLocalArenaOppfolging(AKTOR_ID, Formidlingsgruppe.ARBS)
        mockTiltakshistorikk(FNR, harAktiveDeltakelser = false)
        mockUngdomsprogram(FNR, erDeltaker = false)
        mockArbeidssoekerregisteret(FNR, erArbeidssoeker = false)
        mockAap(FNR, harAap = false)
        startOppfolgingSomArbeidsoker(AKTOR_ID, FNR)
        val oppfolgingsperiodeUuid = oppfolgingService.hentGjeldendeOppfolgingsperiode(FNR).get().uuid
        kandidatForUtmeldingRepository.lagreKandidat(
            ArbeidssøkerPeriodeAvsluttet(
                utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
                utfortAv = "A123123",
                kilde = "kilde",
                hendelseTidspunkt = ZonedDateTime.now().toInstant(),
                arbeidssokerperiodeAvsluttetHendelseType = ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
                avslutningsarsak = BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST.toString(),
                oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
            )
        )

        val erForlenget = fjernKandidatForUtmeldingService.erOppfolgingForlenget(oppfolgingsperiodeUuid)

        assertThat(erForlenget).isFalse()
    }

    @Test
    fun `erOppfolgingForlenget - bruker er kandidat, forlengelse ikke utløpt - returnerer true`() {
        mockSytemBrukerAuthOk(AKTOR_ID, FNR)
        setBrukerUnderOppfolging(AKTOR_ID, FNR)
        setLocalArenaOppfolging(AKTOR_ID, Formidlingsgruppe.ARBS)
        mockTiltakshistorikk(FNR, harAktiveDeltakelser = false)
        mockUngdomsprogram(FNR, erDeltaker = false)
        mockArbeidssoekerregisteret(FNR, erArbeidssoeker = false)
        mockAap(FNR, harAap = false)
        startOppfolgingSomArbeidsoker(AKTOR_ID, FNR)
        val oppfolgingsperiodeUuid = oppfolgingService.hentGjeldendeOppfolgingsperiode(FNR).get().uuid
        kandidatForUtmeldingRepository.lagreKandidat(
            ForlengelseHendelse(
                utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
                utfortAv = "A123123",
                kilde = "kilde",
                hendelseTidspunkt = ZonedDateTime.now().toInstant(),
                oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
                forlengelseHendelseType = ForlengelseHendelseType.FORLENGELSE_OPPRETTET,
                forlengetTil = LocalDate.now().plusDays(30),
            )
        )

        val erForlenget = fjernKandidatForUtmeldingService.erOppfolgingForlenget(oppfolgingsperiodeUuid)

        assertThat(erForlenget).isTrue()
    }

    @Test
    fun `erOppfolgingForlenget - bruker er kandidat, forlengelse utløpt - returnerer false`() {
        mockSytemBrukerAuthOk(AKTOR_ID, FNR)
        setBrukerUnderOppfolging(AKTOR_ID, FNR)
        setLocalArenaOppfolging(AKTOR_ID, Formidlingsgruppe.ARBS)
        mockTiltakshistorikk(FNR, harAktiveDeltakelser = false)
        mockUngdomsprogram(FNR, erDeltaker = false)
        mockArbeidssoekerregisteret(FNR, erArbeidssoeker = false)
        mockAap(FNR, harAap = false)
        startOppfolgingSomArbeidsoker(AKTOR_ID, FNR)
        val oppfolgingsperiodeUuid = oppfolgingService.hentGjeldendeOppfolgingsperiode(FNR).get().uuid
        kandidatForUtmeldingRepository.lagreKandidat(
            ForlengelseHendelse(
                utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
                utfortAv = "A123123",
                kilde = "kilde",
                hendelseTidspunkt = ZonedDateTime.now().toInstant(),
                oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
                forlengelseHendelseType = ForlengelseHendelseType.FORLENGELSE_OPPRETTET,
                forlengetTil = LocalDate.now().minusDays(3),
            )
        )

        val erForlenget = fjernKandidatForUtmeldingService.erOppfolgingForlenget(oppfolgingsperiodeUuid)

        assertThat(erForlenget).isFalse()
    }
}

