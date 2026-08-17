package no.nav.veilarboppfolging.service

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.IntegrationTest
import no.nav.veilarboppfolging.ident.randomFnr
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ArbeidsoppfolgingsKontorServiceTest : IntegrationTest() {
     @Test
     fun `skal hente oppfolgingsenhet for fnr`() {
         val enhetId = "1234"
         val fnr: Fnr = Fnr.of("12345678910")
         val aktorId: AktorId = AktorId.of("1234523423")
         startOppfolgingSomArbeidsoker(aktorId, fnr)
         setAoKontor(fnr, aktorId, enhetId)
         mockHentIdenter(fnr, aktorId)
         val service = ArbeidsoppfolgingsKontorService(arbeidsoppfolgingskontorRepository, aktorOppslagClient)

         val result = service.hentOppfolgingsEnhetId(fnr)

         assertThat(result?.get()).isEqualTo(enhetId)
     }

    @Test
    fun `skal hente oppfolgingsenhet for historisk fnr`() {
        val enhetId = "1234"
        val historiskFnr = randomFnr()
        val nyttFnr: Fnr = randomFnr()
        val aktorId: AktorId = AktorId.of("1234523423")
        startOppfolgingSomArbeidsoker(aktorId, historiskFnr)
        setAoKontor(historiskFnr, aktorId, enhetId)
        mockHentIdenter(nyttFnr, aktorId, listOf(historiskFnr))
        val service = ArbeidsoppfolgingsKontorService(arbeidsoppfolgingskontorRepository, aktorOppslagClient)

        val result = service.hentOppfolgingsEnhetId(nyttFnr)

        assertThat(result?.get()).isEqualTo(enhetId)
    }
}