package no.nav.veilarboppfolging.kandidatForUtmelding

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.veilarboppfolging.IntegrationTest
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.AvregistreringsType
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.OppdateringFraArena_BleIserv
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.UtmeldingsService
import no.nav.veilarboppfolging.repository.UtmeldingRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.springframework.beans.factory.annotation.Autowired
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

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
            ArbeidssøkerPeriodeAvsluttet(
                AKTOR_ID, FNR, avsluttetAv = KandidatForUtmeldingHendelseAvsluttetAv.VEILEDER,
                kilde = "kilde",
                aarsak = "aarsak",
                oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
            )
        )

        val kandidat = kandidatForUtmeldingRepository.hentKandidat(AKTOR_ID)
        assertThat(kandidat).isNotNull
        assertThat(kandidat?.fnr).isEqualTo(FNR)
        assertThat(kandidat?.aktorId).isEqualTo(AKTOR_ID)
        assertThat(kandidat?.avsluttetAv).isEqualTo(KandidatForUtmeldingHendelseAvsluttetAv.VEILEDER)
        assertThat(kandidat?.kilde).isEqualTo("kilde")
        assertThat(kandidat?.aarsak).isEqualTo("aarsak")
    }

    @Test
    fun `lagreKandidatForUtmelding lagrer ikke kandidat i databasen når bruker ikke er under oppfølging`() {
        mockSytemBrukerAuthOk(AKTOR_ID, FNR)
        mockVeilarbArenaOppfolgingsBruker(FNR, Formidlingsgruppe.ARBS)
        mockTiltakshistorikk(FNR, harAktiveDeltakelser = false)
        mockUngdomsprogram(FNR, erDeltaker = false)
        mockArbeidssoekerregisteret(FNR, erArbeidssoeker = false)
        mockAap(FNR, harAap = false)

        kandidatForUtmeldingService.lagreKandidatForUtmelding(
            ArbeidssøkerPeriodeAvsluttet(
                AKTOR_ID, FNR, avsluttetAv = KandidatForUtmeldingHendelseAvsluttetAv.VEILEDER,
                kilde = "kilde",
                aarsak = "aarsak",
                oppfolgingsperiodeUuid = UUID.randomUUID(),
            )
        )

        assertThat(kandidatForUtmeldingRepository.hentKandidat(AKTOR_ID)).isNull()
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

        kandidatForUtmeldingService.lagreKandidatForUtmelding(
            ArbeidssøkerPeriodeAvsluttet(
                AKTOR_ID, FNR, avsluttetAv = KandidatForUtmeldingHendelseAvsluttetAv.VEILEDER,
                kilde = "kilde",
                aarsak = "aarsak",
                oppfolgingsperiodeUuid = UUID.randomUUID(),
            )
        )

        assertThat(kandidatForUtmeldingRepository.hentKandidat(AKTOR_ID)).isNull()
    }

    @Test
    fun `fjernKandidatForUtmelding fjerner kandidat fra databasen`() {
        startOppfolgingSomArbeidsoker(AKTOR_ID, FNR)
        val oppfolgingsperiodeUuid = oppfolgingService.hentGjeldendeOppfolgingsperiode(FNR).get().uuid
        kandidatForUtmeldingRepository.lagreKandidat(
            ArbeidssøkerPeriodeAvsluttet(
                AKTOR_ID, FNR, avsluttetAv = KandidatForUtmeldingHendelseAvsluttetAv.VEILEDER,
                kilde = "kilde",
                aarsak = "aarsak",
                oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
            )
        )
        assertThat(kandidatForUtmeldingRepository.hentKandidat(AKTOR_ID)).isNotNull()

        kandidatForUtmeldingService.fjernKandidatForUtmelding(AKTOR_ID)

        assertThat(kandidatForUtmeldingRepository.hentKandidat(AKTOR_ID)).isNull()
    }

    @Test
    fun `fjernKandidatForUtmelding feiler ikke når kandidat ikke finnes i databasen`() {
        assertThat(kandidatForUtmeldingRepository.hentKandidat(AKTOR_ID)).isNull()

        kandidatForUtmeldingService.fjernKandidatForUtmelding(AKTOR_ID)

        assertThat(kandidatForUtmeldingRepository.hentKandidat(AKTOR_ID)).isNull()
    }
}

