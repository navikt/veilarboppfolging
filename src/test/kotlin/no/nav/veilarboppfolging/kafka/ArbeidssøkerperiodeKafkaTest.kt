package no.nav.veilarboppfolging.kafka

import no.nav.paw.arbeidssokerregisteret.api.v1.*
import no.nav.veilarboppfolging.IntegrationTest
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.util.*
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata as MetaData


@ActiveProfiles("local")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArbeidssøkerperiodeKafkaTest: IntegrationTest() {

    @Autowired
    private lateinit var arbeidssøkerperiodeConsumer: ArbeidssøkerperiodeConsumer

    @Test
    fun `Melding om ny arbeidssøkerperiode skal starte ny arbeidsrettet oppfølgingsperiode`() {
        val melding = ConsumerRecord("topic", 0, 0, "dummyKey", periode());
        arbeidssøkerperiodeConsumer.consumeArbeidssøkerperiode(melding)
    }

    @Test
    fun `Melding om avsluttet arbeidssøkerperiode skal avslutte arbeidsrettet oppfølgingsperiode`() {

    }

    @Test
    fun `Dersom arbeidsrettet oppfølgingsperiode eksisterer skal melding om ny arbeidssøkerperiode ???`() {}

    @Test
    fun `Dersom arbeidsrettet oppfølgingsperiode eksisterer skal melding om ny arbeidssøkerperiode ???`() {}


    private fun periode() = Periode(
        UUID.randomUUID(),
        "dummyIdentitetsnummer",
        MetaData(
            Instant.now(),
            Bruker(
                BrukerType.VEILEDER,
                "dummyId"
            ),
            "dummyKilde",
            "dummyÅrsak",
            TidspunktFraKilde(
                Instant.now(),
                AvviksType.FORSINKELSE
            )
    ),
        null
    )
}