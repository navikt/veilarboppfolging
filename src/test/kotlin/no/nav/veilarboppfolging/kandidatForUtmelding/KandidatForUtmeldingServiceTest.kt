package no.nav.veilarboppfolging.kandidatForUtmelding

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.veilarboppfolging.IntegrationTest
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.OppdateringFraArena_BleIserv
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.UtmeldingsService
import no.nav.veilarboppfolging.repository.UtmeldingRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.ZonedDateTime

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

        kandidatForUtmeldingService.lagreKandidatForUtmelding(
            ArbeidssøkerPeriodeAvsluttet(
                AKTOR_ID, FNR, avsluttetAv = KandidatForUtmeldingHendelseAvsluttetAv.VEILEDER,
                kilde = "kilde",
                aarsak = "aarsak",
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
            )
        )

        assertThat(kandidatForUtmeldingRepository.hentKandidat(AKTOR_ID)).isNull()
    }

    @Test
    fun `fjernKandidatForUtmelding fjerner kandidat fra databasen`() {
        kandidatForUtmeldingRepository.lagreKandidat(
            ArbeidssøkerPeriodeAvsluttet(
                AKTOR_ID, FNR, avsluttetAv = KandidatForUtmeldingHendelseAvsluttetAv.VEILEDER,
                kilde = "kilde",
                aarsak = "aarsak",
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

    @Test
    fun `UtmeldingsService fjerner kandidat fra databasen ved automatisk avslutning etter 28 dager ISERV`() {
        mockSytemBrukerAuthOk(AKTOR_ID, FNR)
        setBrukerUnderOppfolging(AKTOR_ID, FNR)
        setLocalArenaOppfolging(AKTOR_ID, Formidlingsgruppe.ISERV)
        mockTiltakshistorikk(FNR, harAktiveDeltakelser = false)
        mockUngdomsprogram(FNR, erDeltaker = false)
        mockArbeidssoekerregisteret(FNR, erArbeidssoeker = false)
        mockAap(FNR, harAap = false)

        utmeldingRepository.insertUtmeldingTabell(
            OppdateringFraArena_BleIserv(
                AKTOR_ID,
                ZonedDateTime.now().minusDays(29)
            )
        )
        kandidatForUtmeldingRepository.lagreKandidat(
            ArbeidssøkerPeriodeAvsluttet(
                AKTOR_ID, FNR, avsluttetAv = KandidatForUtmeldingHendelseAvsluttetAv.VEILEDER,
                kilde = "kilde",
                aarsak = "aarsak",
            )
        )
        assertThat(kandidatForUtmeldingRepository.hentKandidat(AKTOR_ID)).isNotNull()

        utmeldingsService.avsluttOppfolgingOgFjernFraUtmeldingsTabell(AKTOR_ID)

        assertThat(kandidatForUtmeldingRepository.hentKandidat(AKTOR_ID)).isNull()
    }
}

