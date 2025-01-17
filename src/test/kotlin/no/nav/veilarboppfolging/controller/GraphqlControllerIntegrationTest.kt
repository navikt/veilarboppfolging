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

//    @Test
    fun `når man henter sak for oppfølgsingsperiode uten sak skal sak opprettes`() {
        mockAuthOk(aktørId, fnr)
    }
}