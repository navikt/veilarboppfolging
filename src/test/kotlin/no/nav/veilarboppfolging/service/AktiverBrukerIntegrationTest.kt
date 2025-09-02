package no.nav.veilarboppfolging.service

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.IntegrationTest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.util.UUID

class AktiverBrukerIntegrationTest : IntegrationTest() {

    private val FNR: Fnr = Fnr.of("11111111111")
    private val AKTOR_ID: AktorId = AktorId.of("1234523423")

    @Test
    fun skalLagreIDatabaseDersomKallTilArenaErOK() {
        mockAuthOk(AKTOR_ID, FNR)
        startOppfolgingSomArbeidsoker(AKTOR_ID, FNR)
        val oppfolging = oppfolgingService.hentOppfolging(AKTOR_ID)
        Assertions.assertThat(oppfolging.isPresent()).isTrue()
    }

    @Test
    fun skalHaandtereAtOppfolgingstatusAlleredeFinnes() {
        mockAuthOk(AKTOR_ID, FNR)
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID)
        oppfolgingsPeriodeRepository.avslutt(AKTOR_ID, "veilederid", "begrunnelse")
        startOppfolgingSomArbeidsoker(AKTOR_ID, FNR)
        val oppfolging = oppfolgingService.hentOppfolging(AKTOR_ID)
        Assertions.assertThat(oppfolging.get().isUnderOppfolging()).isTrue()
    }

    @Test
    fun aktiver_sykmeldt_skal_starte_oppfolging() {
        mockInternBrukerAuthOk(UUID.randomUUID(), AKTOR_ID, FNR)
        val oppfolgingFør = oppfolgingService.hentOppfolging(AKTOR_ID)
        Assertions.assertThat(oppfolgingFør.isEmpty()).isTrue()
        aktiverBrukerManueltService.aktiverBrukerManuelt(FNR, "1234")
        val oppfolging = oppfolgingService.hentOppfolging(AKTOR_ID)
        Assertions.assertThat(oppfolging.get().isUnderOppfolging()).isTrue()
    }
}
