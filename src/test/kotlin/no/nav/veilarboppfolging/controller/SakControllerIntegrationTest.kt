package no.nav.veilarboppfolging.controller

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.IntegrationTestUtil
import no.nav.veilarboppfolging.config.ApplicationTestConfig
import no.nav.veilarboppfolging.controller.response.OppfolgingPeriodeDTO
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.assertj.core.data.TemporalOffset
import org.assertj.core.data.TemporalUnitOffset
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal

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

        sakController.hentSakId(forstePeriode.uuid)
        val saker = sakRepository.hentSaker(forstePeriode.uuid)

        assertThat(saker).hasSize(1)
        assertThat(saker[0].id).isEqualTo(1)
        assertThat(saker[0].status).isEqualTo("OPPRETTET")
        assertThat(saker[0].createdAt).isCloseTo(ZonedDateTime.now(), within(500, ChronoUnit.MILLIS))
    }

}