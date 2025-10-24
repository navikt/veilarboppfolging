package no.nav.veilarboppfolging.service

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import kotlin.test.Test


class OppfolgingsperiodeEndretServiceTest {

    @Mock
    val oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository = mock()

    @Mock
    val kafkaProducerService: KafkaProducerService = mock()

    @Mock
    val aktorOppslagClient: AktorOppslagClient = mock()


    private val oppfolgingsperiodeEndretService = OppfolgingsperiodeEndretService(oppfolgingsPeriodeRepository, kafkaProducerService, aktorOppslagClient)

    @Test
    fun `skal ignorere tom melding`() {
        oppfolgingsperiodeEndretService.håndterOppfolgingskontorMelding(null)

        verifyNoInteractions(kafkaProducerService, oppfolgingsPeriodeRepository, aktorOppslagClient)
    }
}