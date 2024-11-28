package no.nav.veilarboppfolging.oppfolgingsbruker.arena

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.IntegrationTest
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolging
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClient
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.boot.test.mock.mockito.MockBean
import java.util.Optional

class ArenaOppfolgingServiceIntegrationTest: IntegrationTest() {

    @MockBean
    lateinit var veilarbarenaClient: VeilarbarenaClient

    val fnr = Fnr.of("123")
    val aktorId = AktorId.of("123")


    @Test
    fun `getOppfolginsstatus skal retunerer success når bruker finnes i arena`() {
        mockInternBrukerAuthOk(aktorId, fnr)
        `when`(veilarbarenaClient.hentOppfolgingsbruker(fnr)).thenReturn(Optional.of(VeilarbArenaOppfolging()
            .setFodselsnr(fnr.get())
            .setNav_kontor("1010")
            .setFormidlingsgruppekode("ARBS")
            .setKvalifiseringsgruppekode("VURDU")
            .setHovedmaalkode("SKAFFE")
        ))
        assert(arenaOppfolgingService.getOppfolginsstatus(fnr) is GetOppfolginsstatusSuccess)
    }

    @Test
    fun `getOppfolginsstatus skal ikke kaste exception hvis bruker ikke finnes i arena`() {
        mockInternBrukerAuthOk(aktorId, fnr)
        `when`(veilarbarenaClient.hentOppfolgingsbruker(fnr)).thenReturn(Optional.empty())
        assert(arenaOppfolgingService.getOppfolginsstatus(fnr) is GetOppfolginsstatusFailure)
    }

}