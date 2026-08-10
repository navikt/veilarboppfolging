package no.nav.veilarboppfolging.service

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`

class ArbeidsoppfolgingsKontorServiceTest : IntegrationTest() {
     @Test
     fun `skal hente oppfolgingsenhet for fnr`() {
         val enhetId = "1234"
         val fnr: Fnr = Fnr.of("12345678910")
         val aktorId: AktorId = AktorId.of("1234523423")
         `when`(aktorOppslagClient.hentAktorId(fnr)).thenReturn(aktorId)
         startOppfolgingSomArbeidsoker(aktorId, fnr)
         setAoKontor(fnr, aktorId, enhetId)
         val service = ArbeidsoppfolgingsKontorService(arbeidsoppfolgingskontorRepository, aktorOppslagClient)

         val result = service.hentOppfolgingsEnhetId(fnr)

         assertThat(result?.get()).isEqualTo(enhetId)
     }
}