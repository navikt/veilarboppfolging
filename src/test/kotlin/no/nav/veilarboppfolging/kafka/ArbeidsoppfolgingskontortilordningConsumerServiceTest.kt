package no.nav.veilarboppfolging.kafka

import no.nav.common.utils.EnvironmentUtils
import no.nav.veilarboppfolging.service.ArbeidsoppfolgingsKontorService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ArbeidsoppfolgingskontortilordningConsumerServiceTest {

    val arbeidsoppfolgingsKontorService: ArbeidsoppfolgingsKontorService = mock()

    private val arbeidsoppfolgingskontortilordningConsumerService = ArbeidsoppfolgingskontortilordningConsumerService(arbeidsoppfolgingsKontorService)

    @Test
    fun `Skal kalle service for å håndtere melding`() {
        val kafkamelding: ConsumerRecord<Long, OppfolgingskontorMelding?> = mock()
        whenever(kafkamelding.key()).thenReturn(8L)
        EnvironmentUtils.setProperty("NAIS_CLUSTER_NAME", "not prod", EnvironmentUtils.Type.PUBLIC)
        arbeidsoppfolgingskontortilordningConsumerService.consumeKontortilordning(kafkamelding)

        verify(arbeidsoppfolgingsKontorService, times(1)).håndterOppfolgingskontorMelding(
            kafkamelding.key(),
            kafkamelding.value()
        )
    }
}