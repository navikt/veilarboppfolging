package no.nav.veilarboppfolging.controller

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.IntegrationTestUtil
import no.nav.veilarboppfolging.config.ApplicationTestConfig
import no.nav.veilarboppfolging.controller.response.OppfolgingPeriodeDTO
import no.nav.veilarboppfolging.test.DbTestUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@SpringBootTest(classes = [ApplicationTestConfig::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SakControllerIntegrationTest: IntegrationTestUtil() {

    private val fnr: Fnr = Fnr.of("123")

    private val aktørId: AktorId = AktorId.of("3409823")

    @BeforeEach
    fun beforeEach() {
        DbTestUtils.cleanupTestDb()
    }

    @Test
    fun `når man henter sak for oppfølgsingsperiode uten sak skal sak opprettes`() {
        mockAuthOk(aktørId, fnr)
        val perioder: List<OppfolgingPeriodeDTO> = startOppfolging(fnr)
        val oppfølgingsperiodeUUID = perioder[0].uuid

        val sak = sakController.hentSak(oppfølgingsperiodeUUID)

        val saker = sakRepository.hentSaker(oppfølgingsperiodeUUID)
        assertThat(saker).hasSize(1)
        assertThat(saker[0].id).isGreaterThan(1000)
        assertThat(saker[0].oppfølgingsperiodeUUID).isEqualTo(oppfølgingsperiodeUUID)
        assertThat(saker[0].status.toString()).isEqualTo("OPPRETTET")
        assertThat(saker[0].createdAt).isCloseTo(ZonedDateTime.now(), within(500, ChronoUnit.MILLIS))

        assertThat(sak.sakId).isEqualTo(saker[0].id)
        assertThat(sak.oppfolgingsperiodeId).isEqualTo(saker[0].oppfølgingsperiodeUUID)
    }

    @Test
    fun `når man henter sak for oppfølgingsperiode med åpen sak skal ikke ny sak opprettes`() {
        mockAuthOk(aktørId, fnr)
        val perioder: List<OppfolgingPeriodeDTO> = startOppfolging(fnr)
        val oppfølgingsperiodeUUID = perioder[0].uuid
        sakRepository.opprettSak(oppfølgingsperiodeUUID)
        assertThat(sakRepository.hentSaker(oppfølgingsperiodeUUID)).hasSize(1)

        val sak = sakController.hentSak(oppfølgingsperiodeUUID)

        val sakerIDatabasen = sakRepository.hentSaker(oppfølgingsperiodeUUID)
        assertThat(sakerIDatabasen).hasSize(1)

        assertThat(sak.sakId).isEqualTo(sakerIDatabasen[0].id)
        assertThat(sak.oppfolgingsperiodeId).isEqualTo(sakerIDatabasen[0].oppfølgingsperiodeUUID)
    }

    @Test
    fun `når man henter sak for oppfølgsingsperiode uten sak skal sak opprettes selv om andre saker for andre perioder finnes`() {
        // Given
        mockAuthOk(aktørId, fnr)
        val oppfølgingsperiodeUuidMedSak = startOppfolging(fnr)[0].uuid
        sakRepository.opprettSak(oppfølgingsperiodeUuidMedSak)
        assertThat(sakRepository.hentSaker(oppfølgingsperiodeUuidMedSak)).hasSize(1)

        val annetFnr = Fnr.of(fnr.toString() + "annen")
        val annenAktørId = AktorId.of(aktørId.toString() + "annen")
        mockAuthOk(annenAktørId, annetFnr)
        val oppfølgingsperiodeUuidUtenSak = startOppfolging(annetFnr)[0].uuid
        assertThat(oppfølgingsperiodeUuidMedSak).isNotEqualTo(oppfølgingsperiodeUuidUtenSak)

        // When
        sakController.hentSak(oppfølgingsperiodeUuidUtenSak)

        // Then
        assertThat(sakRepository.hentSaker(oppfølgingsperiodeUuidUtenSak)).hasSize(1)
        assertThat(sakRepository.hentSaker(oppfølgingsperiodeUuidMedSak)).hasSize(1)
    }

    @Test
    fun `når man  henter sak for oppfølgingsperiode som ikke eksisterer skal man få 400`() {

    }
}