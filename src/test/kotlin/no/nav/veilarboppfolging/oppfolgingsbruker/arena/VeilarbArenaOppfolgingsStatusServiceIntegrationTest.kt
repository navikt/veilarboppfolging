package no.nav.veilarboppfolging.oppfolgingsbruker.arena

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.pto_schema.enums.arena.Hovedmaal
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe
import no.nav.veilarboppfolging.IntegrationTest
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolgingsBruker
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClient
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.OppfolgingEnhetMedVeilederResponse.Oppfolgingsenhet
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import java.util.*
import kotlin.test.assertEquals

class VeilarbArenaOppfolgingsStatusServiceIntegrationTest: IntegrationTest() {

    @Autowired
    lateinit var veilarbarenaClient: VeilarbarenaClient

    val fnr = Fnr.of("123")
    val aktorId = AktorId.of("123")


    @Test
    fun `getOppfolginsstatus skal retunere success når bruker finnes i arena`() {
        mockNorgEnhetsNavn("1010", "Nav 1010")
        mockInternBrukerAuthOk(UUID.randomUUID(), aktorId, fnr)
        `when`(veilarbarenaClient.hentOppfolgingsbruker(fnr)).thenReturn(Optional.of(
            VeilarbArenaOppfolgingsBruker()
            .setFodselsnr(fnr.get())
            .setNav_kontor("1010")
            .setFormidlingsgruppekode("ARBS")
            .setKvalifiseringsgruppekode("VURDU")
            .setHovedmaalkode("SKAFFEA")
        ))

        val hentarenaResult = arenaOppfolgingService.hentArenaOppfolginsstatusMedHovedmaal(fnr)

        assertEquals(hentarenaResult, GetOppfolginsstatusSuccess(
            OppfolgingEnhetMedVeilederResponse(
                formidlingsgruppe = Formidlingsgruppe.ARBS.name,
                servicegruppe = Kvalifiseringsgruppe.VURDU.name,
                hovedmaalkode = Hovedmaal.SKAFFEA.name,
                veilederId = null,
                oppfolgingsenhet = Oppfolgingsenhet(enhetId = "1010",navn = "Nav 1010")
            )
        ))
    }

    @Test
    fun `getOppfolginsstatus skal ikke kaste exception hvis bruker ikke finnes i arena`() {
        mockInternBrukerAuthOk(UUID.randomUUID(), aktorId, fnr)
        `when`(veilarbarenaClient.hentOppfolgingsbruker(fnr)).thenReturn(Optional.empty())
        assert(arenaOppfolgingService.hentArenaOppfolginsstatusMedHovedmaal(fnr) is GetOppfolginsstatusFailure)
    }

}