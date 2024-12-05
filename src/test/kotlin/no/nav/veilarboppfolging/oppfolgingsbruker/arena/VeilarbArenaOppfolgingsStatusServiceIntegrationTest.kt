package no.nav.veilarboppfolging.oppfolgingsbruker.arena

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.IntegrationTest
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolgingsBruker
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClient
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.boot.test.mock.mockito.MockBean
import java.util.*

class VeilarbArenaOppfolgingsStatusServiceIntegrationTest: IntegrationTest() {

    @MockBean
    lateinit var veilarbarenaClient: VeilarbarenaClient

    val fnr = Fnr.of("123")
    val aktorId = AktorId.of("123")


    @Test
    fun `getOppfolginsstatus skal retunerer success når bruker finnes i arena`() {
        mockInternBrukerAuthOk(UUID.randomUUID(), aktorId, fnr)
        `when`(veilarbarenaClient.hentOppfolgingsbruker(fnr)).thenReturn(Optional.of(
            VeilarbArenaOppfolgingsBruker()
            .setFodselsnr(fnr.get())
            .setNav_kontor("1010")
            .setFormidlingsgruppekode("ARBS")
            .setKvalifiseringsgruppekode("VURDU")
            .setHovedmaalkode("SKAFFEA")
        ))
        assert(arenaOppfolgingService.hentArenaOppfolginsstatusMedHovedmaal(fnr) is GetOppfolginsstatusSuccess)
    }

    @Test
    fun `getOppfolginsstatus skal ikke kaste exception hvis bruker ikke finnes i arena`() {
        mockInternBrukerAuthOk(UUID.randomUUID(), aktorId, fnr)
        `when`(veilarbarenaClient.hentOppfolgingsbruker(fnr)).thenReturn(Optional.empty())
        assert(arenaOppfolgingService.hentArenaOppfolginsstatusMedHovedmaal(fnr) is GetOppfolginsstatusFailure)
    }

}