package no.nav.veilarboppfolging.kafka

import java.time.Instant
import java.time.LocalDate
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.doedsfall.Doedsfall
import no.nav.person.pdl.leesah.falskidentitet.FalskIdentitet
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.veilarboppfolging.IntegrationTest
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PersonhendelseConsumerTest(
    @Autowired
    private val personhendelseConsumer: PersonhendelseConsumer,
) : IntegrationTest() {
    private val fnr = Fnr.of("01010198765")
    private val aktorId = AktorId.of("123456789012")

    @Test
    fun `ikke dødsfall-hendelse - blir ignorert`() {
        val personhendelse = Personhendelse.newBuilder()
            .setHendelseId("dummyHendelseId")
            .setPersonidenter(listOf("12345678901"))
            .setMaster("PDL")
            .setOpprettet(Instant.now())
            .setOpplysningstype("FalskIdentitetV1")
            .setEndringstype(Endringstype.OPPRETTET)
            .setFalskIdentitet(FalskIdentitet(true))
            .build()
        val melding = ConsumerRecord("topic", 0, 0, 1L, personhendelse)

        personhendelseConsumer.consumePersonhendelse(melding)
    }

    @Test
    fun `dødsfall-hendelse, bruker er ikke under oppfølging - blir ignorert`() {
        val melding = ConsumerRecord("topic", 0, 0, 1L, personhendelseForDodsfall(listOf(fnr.get(), aktorId.get())))

        personhendelseConsumer.consumePersonhendelse(melding)
    }

    @Test
    fun `dødsfall-hendelse, bruker er under oppfølging - oppfølging blir avsluttet`() {
        mockSytemBrukerAuthOk(aktorId, fnr)
        startOppfolgingSomArbeidsoker(aktorId, fnr)
        setLocalArenaOppfolging(aktorId, Formidlingsgruppe.ARBS)
        mockTiltakshistorikk(fnr, harAktiveDeltakelser = false)
        mockUngdomsprogram(fnr, erDeltaker = false)
        mockAap(fnr, harAap = false)
        assertThat(oppfolgingService.erUnderOppfolging(aktorId)).isTrue

        val melding = ConsumerRecord("topic", 0, 0, 1L, personhendelseForDodsfall(listOf(fnr.get(), aktorId.get())))

        personhendelseConsumer.consumePersonhendelse(melding)

        assertThat(oppfolgingService.erUnderOppfolging(aktorId)).isFalse
    }

    private fun personhendelseForDodsfall(identer: List<String>): Personhendelse {
        return Personhendelse.newBuilder()
            .setHendelseId("dummyHendelseId")
            .setPersonidenter(identer)
            .setMaster("PDL")
            .setOpprettet(Instant.now())
            .setOpplysningstype("DoedsfallV1")
            .setEndringstype(Endringstype.OPPRETTET)
            .setDoedsfall(Doedsfall(LocalDate.now()))
            .build()
    }
}