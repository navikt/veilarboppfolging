package no.nav.veilarboppfolging.kafka

import no.nav.veilarboppfolging.service.OppfolgingsperiodeEndretService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class ArbeidsoppfolgingskontortilordningConsumerServiceTest {

    val oppfolgingsperiodeEndretService: OppfolgingsperiodeEndretService = mock()

    private val arbeidsoppfolgingskontortilordningConsumerService = ArbeidsoppfolgingskontortilordningConsumerService(oppfolgingsperiodeEndretService)

    @Test
    fun `Skal kalle service for å håndtere melding`() {
        val kafkamelding: ConsumerRecord<String, OppfolgingskontorMelding?> = mock()
        arbeidsoppfolgingskontortilordningConsumerService.consumeKontortilordning(kafkamelding)
        verify(oppfolgingsperiodeEndretService, times(1)).håndterOppfolgingskontorMelding(kafkamelding.value())
    }
}