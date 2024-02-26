package no.nav.veilarboppfolging.controller

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.IntegrationTestUtil
import no.nav.veilarboppfolging.config.ApplicationTestConfig
import no.nav.veilarboppfolging.controller.response.OppfolgingPeriodeDTO
import no.nav.veilarboppfolging.controller.response.OppfolgingPeriodeMinimalDTO
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext

@SpringBootTest(classes = [ApplicationTestConfig::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SakControllerIntegrationTest: IntegrationTestUtil() {

    private val fnr: Fnr = Fnr.of("123")

    private val aktørId: AktorId = AktorId.of("3409823")

    private val token = "token"

    @Test
    fun `når man henter sak-id for oppfølgsingsperiode uten sak skal sak-id opprettes`() {
        mockAuthOk(aktørId, fnr)
        val perioder: List<OppfolgingPeriodeDTO> = startOppfolging(fnr)
        val forstePeriode = perioder[0]

        val sakId = sakController.hentSakId(forstePeriode.uuid)

        assertThat(sakId).isGreaterThan(0)
        // TODO: sjekk at lagret i databasen

    }

}