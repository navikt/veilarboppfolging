package no.nav.veilarboppfolging.kafka

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.paw.arbeidssokerregisteret.api.v1.*
import no.nav.veilarboppfolging.IntegrationTest
import no.nav.veilarboppfolging.service.OppfolgingService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.mockito.Mockito.`when`
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

    @Autowired
    private lateinit var oppfolgingService: OppfolgingService

    @Test
    fun `Melding om ny arbeidssøkerperiode skal starte ny arbeidsrettet oppfølgingsperiode`() {
        val aktørId = AktorId.of("123456789012")
        val fødselsnummer = "01010198765"
        `when`(aktorOppslagClient.hentAktorId(Fnr.of(fødselsnummer))).thenReturn(aktørId)
        val melding = ConsumerRecord("topic", 0, 0, "dummyKey", periode(fødselsnummer))

        arbeidssøkerperiodeConsumer.consumeArbeidssøkerperiode(melding)

        val oppfølgingsperioder = oppfolgingService.hentOppfolgingsperioder(Fnr.of(fødselsnummer))
        assertThat(oppfølgingsperioder).hasSize(1)
    }

    @Test
    fun `Melding om avsluttet arbeidssøkerperiode skal avslutte arbeidsrettet oppfølgingsperiode`() {

    }

    @Test
    fun `Dersom arbeidsrettet oppfølgingsperiode eksisterer skal melding om ny arbeidssøkerperiode ???`() {}

    @Test
    fun `Dersom arbeidsrettet oppfølgingsperiode startes for arbeidssøker som er sykmeldt skal ???`() {}


    private fun periode(fødselsnummer: String) = Periode(
        UUID.randomUUID(),
        fødselsnummer,
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