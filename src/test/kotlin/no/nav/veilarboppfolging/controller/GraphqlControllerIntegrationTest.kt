package no.nav.veilarboppfolging.controller

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.IntegrationTest
import no.nav.veilarboppfolging.controller.response.OppfolgingPeriodeDTO
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class GraphqlControllerIntegrationTest: IntegrationTest() {

    private val fnr: Fnr = Fnr.of("123")
    private val aktørId: AktorId = AktorId.of("3409823")

    @Test
    fun `når man henter sak for oppfølgsingsperiode uten sak skal sak opprettes`() {
        mockAuthOk(aktørId, fnr)

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
}