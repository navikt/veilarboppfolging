package no.nav.veilarboppfolging.controller

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.BadRequestException
import no.nav.veilarboppfolging.IntegrationTest
import no.nav.veilarboppfolging.controller.response.OppfolgingPeriodeDTO
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class SakControllerIntegrationTest: IntegrationTest() {

    private val fnr: Fnr = Fnr.of("12345678901")

    private val aktørId: AktorId = AktorId.of("09876543210987")

    @Test
    fun `når man henter sak for oppfølgsingsperiode uten sak skal sak opprettes`() {
        mockAuthOk(aktørId, fnr)
        startOppfolgingSomArbeidsoker(aktørId, fnr)
        val perioder: List<OppfolgingPeriodeDTO> = hentOppfolgingsperioder(fnr)
        val oppfølgingsperiodeUUID = perioder[0].uuid

        val sak = sakController.opprettEllerHentSak(oppfølgingsperiodeUUID)

        val saker = sakRepository.hentSaker(oppfølgingsperiodeUUID)
        assertThat(saker).hasSize(1)
        assertThat(saker[0].id).isGreaterThanOrEqualTo(1000)
        assertThat(saker[0].oppfølgingsperiodeUUID).isEqualTo(oppfølgingsperiodeUUID)
        assertThat(saker[0].createdAt).isCloseTo(ZonedDateTime.now(), within(500, ChronoUnit.MILLIS))

        assertThat(sak.sakId).isEqualTo(saker[0].id)
        assertThat(sak.oppfolgingsperiodeId).isEqualTo(saker[0].oppfølgingsperiodeUUID)
        assertThat(sak.fagsaksystem).isEqualTo("ARBEIDSOPPFOLGING")
        assertThat(sak.tema).isEqualTo("OPP")
    }

    @Test
    fun `når man henter sak for oppfølgingsperiode med eksisterende sak skal ikke ny sak opprettes`() {
        mockAuthOk(aktørId, fnr)
        startOppfolgingSomArbeidsoker(aktørId, fnr)
        val perioder: List<OppfolgingPeriodeDTO> = hentOppfolgingsperioder(fnr)
        val oppfølgingsperiodeUUID = perioder[0].uuid
        sakRepository.opprettSak(oppfølgingsperiodeUUID)
        assertThat(sakRepository.hentSaker(oppfølgingsperiodeUUID)).hasSize(1)

        val sak = sakController.opprettEllerHentSak(oppfølgingsperiodeUUID)

        val sakerIDatabasen = sakRepository.hentSaker(oppfølgingsperiodeUUID)
        assertThat(sakerIDatabasen).hasSize(1)

        assertThat(sak.sakId).isEqualTo(sakerIDatabasen[0].id)
        assertThat(sak.oppfolgingsperiodeId).isEqualTo(sakerIDatabasen[0].oppfølgingsperiodeUUID)
    }

    @Test
    fun `når man henter sak for oppfølgsingsperiode uten sak skal sak opprettes selv om andre saker for andre perioder finnes`() {
        // Given
        mockAuthOk(aktørId, fnr)
        startOppfolgingSomArbeidsoker(aktørId, fnr)
        val oppfølgingsperiodeUuidMedSak = hentOppfolgingsperioder(fnr)[0].uuid
        sakRepository.opprettSak(oppfølgingsperiodeUuidMedSak)
        assertThat(sakRepository.hentSaker(oppfølgingsperiodeUuidMedSak)).hasSize(1)

        val annetFnr = Fnr.of("09876543210"    )
        val annenAktørId = AktorId.of("12345678901234")
        mockAuthOk(annenAktørId, annetFnr)
        startOppfolgingSomArbeidsoker(annenAktørId, annetFnr)
        val oppfølgingsperiodeUuidUtenSak = hentOppfolgingsperioder(annetFnr)[0].uuid
        assertThat(oppfølgingsperiodeUuidMedSak).isNotEqualTo(oppfølgingsperiodeUuidUtenSak)

        // When
        sakController.opprettEllerHentSak(oppfølgingsperiodeUuidUtenSak)

        // Then
        assertThat(sakRepository.hentSaker(oppfølgingsperiodeUuidUtenSak)).hasSize(1)
        assertThat(sakRepository.hentSaker(oppfølgingsperiodeUuidMedSak)).hasSize(1)
    }

    @Test
    fun `når man  henter sak for oppfølgingsperiode som ikke eksisterer skal man få BadRequestException`() {
        val oppfølgingsUuuidSomIkkeEksisterer = UUID.randomUUID()

        assertThatExceptionOfType(BadRequestException::class.java).isThrownBy {
            sakController.opprettEllerHentSak(oppfølgingsUuuidSomIkkeEksisterer)
        }

        assertThat(sakRepository.hentSaker(oppfølgingsUuuidSomIkkeEksisterer)).isEmpty()
    }

    @Test
    fun `Skal kunne hente sak for oppfølgingsperiode som er avsluttet`() {
        mockAuthOk(aktørId, fnr)
        startOppfolgingSomArbeidsoker(aktørId, fnr)
        val perioder: List<OppfolgingPeriodeDTO> = hentOppfolgingsperioder(fnr)
        val oppfølgingsperiodeUUID = perioder[0].uuid
        avsluttOppfolging(aktørId)

        val sak = sakController.opprettEllerHentSak(oppfølgingsperiodeUUID)

        val sakerIDatabasen = sakRepository.hentSaker(oppfølgingsperiodeUUID)
        assertThat(sakerIDatabasen).hasSize(1)
        assertThat(sak.sakId).isEqualTo(sakerIDatabasen[0].id)
        assertThat(sak.oppfolgingsperiodeId).isEqualTo(sakerIDatabasen[0].oppfølgingsperiodeUUID)
    }
}