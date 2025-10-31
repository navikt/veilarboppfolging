package no.nav.veilarboppfolging.kafka

import no.nav.veilarboppfolging.service.ArbeidsoppfolgingsKontorEndretService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

class ArbeidsoppfolgingskontortilordningConsumerServiceTest {

    val arbeidsoppfolgingsKontorEndretService: ArbeidsoppfolgingsKontorEndretService = mock()

    private val arbeidsoppfolgingskontortilordningConsumerService = ArbeidsoppfolgingskontortilordningConsumerService(arbeidsoppfolgingsKontorEndretService)

    @Test
    fun `Skal kalle service for å håndtere melding`() {
        val kafkamelding: ConsumerRecord<String, OppfolgingskontorMelding?> = mock()
        whenever(kafkamelding.key()).thenReturn(UUID.randomUUID().toString())
        arbeidsoppfolgingskontortilordningConsumerService.consumeKontortilordning(kafkamelding)

        verify(arbeidsoppfolgingsKontorEndretService, times(1)).håndterOppfolgingskontorMelding(
            kafkamelding.key(),
            kafkamelding.value()
        )
    }
}