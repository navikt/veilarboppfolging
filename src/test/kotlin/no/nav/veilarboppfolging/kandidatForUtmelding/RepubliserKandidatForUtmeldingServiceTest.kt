package no.nav.veilarboppfolging.kandidatForUtmelding

import java.time.ZonedDateTime
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.paw.arbeidssokerregisteret.api.v1.AvsluttetAarsakType.BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.veilarboppfolging.IntegrationTest
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.Kategori
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.Operasjon
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RepubliserKandidatForUtmeldingServiceTest : IntegrationTest() {
    private val AKTOR_ID = AktorId.of("2234567811")
    private val FNR = Fnr.of("22345678912")

    @Test
    fun `republiserKandidatForUtmelding sender start-melding for kandidat som skal vises hos OBO`() {
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
                arbeidssokerperiodeAvsluttetHendelseType = KandidatForUtmeldingHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
                avslutningsarsak = BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST.toString(),
                oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
            )
        )
        val filterkategoriPersonId = kandidatForUtmeldingRepository.hentEllerOpprettFilterhendelseId(oppfolgingsperiodeUuid)
        assertThat(kandidatForUtmeldingRepository.hentKandidat(oppfolgingsperiodeUuid)).isNotNull()

        republiserKandidatForUtmeldingService.republiserKandidatForUtmelding(oppfolgingsperiodeUuid)

        val filterhendelse = getFilterhendelseRecordsStoredInKafkaOutbox(kafkaProperties.portefoljeHendelsesfilterTopic, filterkategoriPersonId.toString()).first()
        assertThat(filterhendelse.operasjon).isEqualTo(Operasjon.START)
        assertThat(filterhendelse.kategori).isEqualTo(Kategori.KANDIDAT_FOR_UTMELDING)
    }

    @Test
    fun `republiserKandidatForUtmelding sender stopp-melding for kandidat som ikke vises hos OBO`() {
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
                arbeidssokerperiodeAvsluttetHendelseType = KandidatForUtmeldingHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
                avslutningsarsak = BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST.toString(),
                oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
            )
        )
        val filterkategoriPersonId = kandidatForUtmeldingRepository.hentEllerOpprettFilterhendelseId(oppfolgingsperiodeUuid)
        kandidatForUtmeldingRepository.fjernKandidat(oppfolgingsperiodeUuid)
        assertThat(kandidatForUtmeldingRepository.hentKandidat(oppfolgingsperiodeUuid)).isNull()
        assertThat(kandidatForUtmeldingRepository.hentSisteKandidatForUtmeldingHendelse(oppfolgingsperiodeUuid)).isNotNull()

        republiserKandidatForUtmeldingService.republiserKandidatForUtmelding(oppfolgingsperiodeUuid)

        val filterhendelse = getFilterhendelseRecordsStoredInKafkaOutbox(kafkaProperties.portefoljeHendelsesfilterTopic, filterkategoriPersonId.toString()).first()
        assertThat(filterhendelse.operasjon).isEqualTo(Operasjon.STOPP)
        assertThat(filterhendelse.kategori).isEqualTo(Kategori.KANDIDAT_FOR_UTMELDING)
    }
}