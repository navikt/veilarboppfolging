package no.nav.veilarboppfolging.kafka

import no.nav.common.client.norg2.Enhet
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.pto_schema.enums.arena.Hovedmaal
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe
import no.nav.veilarboppfolging.IntegrationTest
import no.nav.veilarboppfolging.kafka.TestUtils.oppfølgingsBrukerEndret
import no.nav.veilarboppfolging.service.KafkaConsumerService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertEquals

@ActiveProfiles("local")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EndringPaOppfolgingBrukerConsumerTest: IntegrationTest() {

    @Autowired
    private lateinit var kafkaConsumerService: KafkaConsumerService

    @Test
    fun `getArenaOppfolgingsEnhet skal svare med svar siste enhet man har fått på oppfølginsbruker topic`() {
        val fnr = Fnr.of("123")
        val aktorId = AktorId.of("123")
        mockInternBrukerAuthOk(aktorId, fnr)

        val enhetIdVest = "0123"
        val enhetNavnVest = "Nav VEST"
        mockEnhetINorg(enhetIdVest, enhetNavnVest)

        val enhetIdØst = "0122"
        val enhetNavnØst = "Nav ØST"
        mockEnhetINorg(enhetIdØst, enhetNavnØst)

        val ingenEnhet = arenaOppfolgingService.getArenaOppfolgingsEnhet(fnr)
        assert(ingenEnhet == null)

        meldingFraVeilarbArenaPåBrukerMedEnhet(fnr, enhetIdVest)

        val skalVæreEnhetVest = arenaOppfolgingService.getArenaOppfolgingsEnhet(fnr)
        assertEquals(enhetNavnVest, skalVæreEnhetVest?.navn)
        assertEquals(enhetIdVest, skalVæreEnhetVest?.enhetId)

        meldingFraVeilarbArenaPåBrukerMedEnhet(fnr, enhetIdØst)

        val skalVæreEnhetØst = arenaOppfolgingService.getArenaOppfolgingsEnhet(fnr)
        assertEquals(enhetNavnØst, skalVæreEnhetØst?.navn)
        assertEquals(enhetIdØst, skalVæreEnhetØst?.enhetId)
    }

    @Test
    fun `skal lagre hovedmaal, kvalifiseringsgruppe og formidlingsgruppe ved endring på oppfolgingsbruker`() {
        val fnr = Fnr.of("4141")
        val aktorId = AktorId.of("1414")
        mockInternBrukerAuthOk(aktorId, fnr)
        val hovedmaal = Hovedmaal.BEHOLDEA
        val formidlingsgruppe = Formidlingsgruppe.IARBS
        val kvalifiseringsgruppe = Kvalifiseringsgruppe.VURDU
        mockEnhetINorg("8989", "Nav enhet")

        val record = ConsumerRecord("topic", 0, 0, "key", oppfølgingsBrukerEndret(fnr.get(),
            enhetId = "8989",
            hovedmaal = hovedmaal,
            formidlingsgruppe = formidlingsgruppe,
            kvalifiseringsgruppe = kvalifiseringsgruppe
        ))
        kafkaConsumerService.consumeEndringPaOppfolgingBruker(record)

        val statusEtterEndring = oppfolgingsStatusRepository.hentOppfolging(aktorId)
        assert(statusEtterEndring.isPresent)
        assertEquals(hovedmaal, statusEtterEndring.get().hovedmaal)
        assertEquals(formidlingsgruppe, statusEtterEndring.get().formidlingsgruppe)
        assertEquals(kvalifiseringsgruppe, statusEtterEndring.get().kvalifiseringsgruppe)
    }

    fun mockEnhetINorg(id: String, navn: String) {
        val enhet = Enhet().also { it.navn = navn }
        `when`(norg2Client.hentEnhet(id)).thenReturn(enhet)
    }

    fun meldingFraVeilarbArenaPåBrukerMedEnhet(fnr: Fnr, enhetId: String) {
        val record = ConsumerRecord("topic", 0, 0, "key", oppfølgingsBrukerEndret(fnr.get(), enhetId = enhetId))
        kafkaConsumerService.consumeEndringPaOppfolgingBruker(record)
    }

}
