package no.nav.veilarboppfolging.kandidatForUtmelding

import java.time.ZonedDateTime
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NorskIdent
import no.nav.paw.arbeidssokerregisteret.api.v1.AvsluttetAarsakType.BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.veilarboppfolging.IntegrationTest
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.FilterhendelseRecord
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.Kategori
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.Operasjon
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URI
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

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
            arbeidssokerPeriodeAvsluttet(oppfolgingsperiodeUuid)
                .let { KandidatForUtmelding.fromHendelse(it) }
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
        kandidatForUtmeldingService.handterUtmeldingsHendelse(FNR, arbeidssokerPeriodeAvsluttet(oppfolgingsperiodeUuid))

        kandidatForUtmeldingService.handterUtmeldingsHendelse(FNR,
            OppfolgingAvsluttetHendelse(oppfolgingsperiodeUuid, oppfolgingAvsluttetHendelseType = OppfolgingAvsluttetHendelseType.OPPFOLGING_AVSLUTTET_AUTOMATISK)
        )
        assertThat(kandidatForUtmeldingRepository.hentKandidat(oppfolgingsperiodeUuid)).isNull()
        assertThat(kandidatForUtmeldingRepository.hentSisteHendelseForKandidat(oppfolgingsperiodeUuid)?.type)
            .isEqualTo(ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT)

        republiserKandidatForUtmeldingService.republiserKandidatForUtmelding(oppfolgingsperiodeUuid)

        val filterkategoriPersonId = kandidatForUtmeldingRepository.hentFilterhendelseId(oppfolgingsperiodeUuid)
        val filterhendelse = getFilterhendelseRecordsStoredInKafkaOutbox(kafkaProperties.portefoljeHendelsesfilterTopic, filterkategoriPersonId.toString()).first()
        assertThat(filterhendelse.hendelse?.beskrivelse).isEqualTo("Arbeidssøkerperiode avsluttet: Ikke levert meldekort")
        assertThat(filterhendelse.operasjon).isEqualTo(Operasjon.STOPP)
        assertThat(filterhendelse.kategori).isEqualTo(Kategori.KANDIDAT_FOR_UTMELDING)
    }

    @Test
    fun `republiserKandidatForUtmelding skal republisere STOPP-melding hvis siste hendelse er FORLENGELSE_ENDRET`() {
        mockSytemBrukerAuthOk(AKTOR_ID, FNR)
        setBrukerUnderOppfolging(AKTOR_ID, FNR)
        setLocalArenaOppfolging(AKTOR_ID, Formidlingsgruppe.ARBS)
        mockTiltakshistorikk(FNR, harAktiveDeltakelser = false)
        mockUngdomsprogram(FNR, erDeltaker = false)
        mockArbeidssoekerregisteret(FNR, erArbeidssoeker = false)
        mockAap(FNR, harAap = false)
        startOppfolgingSomArbeidsoker(AKTOR_ID, FNR)
        val oppfolgingsperiodeUuid = oppfolgingService.hentGjeldendeOppfolgingsperiode(FNR).get().uuid
        val hendelseTidspunkt = ZonedDateTime.now().toInstant()
        kandidatForUtmeldingService.handterUtmeldingsHendelse(FNR,
            ForlengelseHendelse(
                utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
                utfortAv = "A123123",
                kilde = "kilde",
                hendelseTidspunkt = hendelseTidspunkt,
                oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
                forlengelseHendelseType = ForlengelseHendelseType.FORLENGELSE_ENDRET,
                forlengetTil = LocalDateTime.now().plusDays(30).toLocalDate()
            )
        )
        assertThat(kandidatForUtmeldingRepository.hentKandidat(oppfolgingsperiodeUuid)).isNull()
        assertThat(kandidatForUtmeldingRepository.hentSisteHendelseForKandidat(oppfolgingsperiodeUuid)).isNotNull()

        republiserKandidatForUtmeldingService.republiserKandidatForUtmelding(oppfolgingsperiodeUuid)

        val filterkategoriPersonId = kandidatForUtmeldingRepository.hentEllerOpprettFilterhendelseId(oppfolgingsperiodeUuid)
        val filterhendelse = getFilterhendelseRecordsStoredInKafkaOutbox(kafkaProperties.portefoljeHendelsesfilterTopic, filterkategoriPersonId.toString()).firstOrNull()
        assertThat(filterhendelse).isEqualTo(FilterhendelseRecord(
            personID = NorskIdent(FNR.get()),
            avsender = "veilarboppfolging",
            kategori = Kategori.KANDIDAT_FOR_UTMELDING,
            operasjon = Operasjon.STOPP,
            hendelse = FilterhendelseRecord.HendelseInnhold(
                beskrivelse = "Forlengelse opprettet",
                beskrivelseEnum = "FORLENGELSE_ENDRET",
                dato = hendelseTidspunkt.atZone(ZoneId.systemDefault()),
                lenke = URI("https://veilarbpersonflate.ansatt.dev.nav.no/aktivitetsplan").toURL(),
                detaljer = null,
                datoFrist = null
            )
        ))
    }

    private fun arbeidssokerPeriodeAvsluttet(oppfolgingsperiodeId: UUID): ArbeidssøkerPeriodeAvsluttet {
        return ArbeidssøkerPeriodeAvsluttet(
            utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
            utfortAv = "A123123",
            kilde = "kilde",
            hendelseTidspunkt = ZonedDateTime.now().toInstant(),
            arbeidssokerperiodeAvsluttetHendelseType = ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
            avslutningsarsak = BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST.toString(),
            oppfolgingsperiodeUuid = oppfolgingsperiodeId,
        )
    }
}