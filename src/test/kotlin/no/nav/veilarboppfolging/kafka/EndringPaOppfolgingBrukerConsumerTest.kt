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
import org.junit.jupiter.api.BeforeAll
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

    val fnr = Fnr.of("123")
    val aktorId = AktorId.of("123")

    @BeforeAll
    fun beforeAll() {
        mockInternBrukerAuthOk(aktorId, fnr)
    }

    @Test
    fun `getArenaOppfolgingsEnhet skal svare med svar siste enhet man har fått på oppfølginsbruker topic`() {
        val enhetIdVest = "0123"
        val enhetNavnVest = "Nav VEST"
        mockEnhetINorg(enhetIdVest, enhetNavnVest)

        val enhetIdØst = "0122"
        val enhetNavnØst = "Nav ØST"
        mockEnhetINorg(enhetIdØst, enhetNavnØst)

        val ingenEnhet = arenaOppfolgingService.hentArenaOppfolgingsEnhet(fnr)
        assert(ingenEnhet == null)

        meldingFraVeilarbArenaPåBrukerMedEnhet(fnr, enhetIdVest)

//        val skalVæreEnhetVest = arenaOppfolgingService.hentArenaOppfolgingsEnhet(fnr)
        val skalVæreEnhetVest = oppfolgingsStatusRepository.hentArenaOppfolgingsEnhet(fnr)
        assertEquals(enhetNavnVest, skalVæreEnhetVest?.navn)
        assertEquals(enhetIdVest, skalVæreEnhetVest?.enhetId)

        meldingFraVeilarbArenaPåBrukerMedEnhet(fnr, enhetIdØst)

        val skalVæreEnhetØst = arenaOppfolgingService.hentArenaOppfolgingsEnhet(fnr)
        assertEquals(enhetNavnØst, skalVæreEnhetØst?.navn)
        assertEquals(enhetIdØst, skalVæreEnhetØst?.enhetId)
    }

    @Test
    fun `skal lagre hovedmaal, kvalifiseringsgruppe og formidlingsgruppe ved endring på oppfolgingsbruker`() {
        val hovedmaal = Hovedmaal.BEHOLDEA
        val formidlingsgruppe = Formidlingsgruppe.IARBS
        val kvalifiseringsgruppe = Kvalifiseringsgruppe.VURDU
        mockEnhetINorg("8989", "Nav enhet")

        meldingFraVeilarbArenaPåBrukerMedStatus(
            fnr = fnr,
            enhetId = "8989",
            hovedmaal = hovedmaal,
            formidlingsgruppe = formidlingsgruppe,
            kvalifiseringsgruppe = kvalifiseringsgruppe
        )

        val statusEtterEndring = oppfolgingsStatusRepository.hentOppfolging(aktorId)
        assert(statusEtterEndring.isPresent)
        assertEquals(hovedmaal, statusEtterEndring.get().hovedmaal)
        assertEquals(formidlingsgruppe, statusEtterEndring.get().formidlingsgruppe)
        assertEquals(kvalifiseringsgruppe, statusEtterEndring.get().kvalifiseringsgruppe)
    }

    @Test
    fun `skal håndtere at hovedmaal er null`() {
        mockEnhetINorg("8989", "Nav enhet")

        meldingFraVeilarbArenaPåBrukerMedStatus(
            fnr = fnr,
            enhetId = "8989",
            hovedmaal = null,
            formidlingsgruppe = Formidlingsgruppe.IARBS,
            kvalifiseringsgruppe = Kvalifiseringsgruppe.VURDU
        )

        val statusEtterEndring = oppfolgingsStatusRepository.hentOppfolging(aktorId)
        assert(statusEtterEndring.isPresent) { "Oppfolgingsstatus fra arena var null" }
        assertEquals(null, statusEtterEndring.get().hovedmaal)
        assertEquals(Formidlingsgruppe.IARBS, statusEtterEndring.get().formidlingsgruppe)
        assertEquals(Kvalifiseringsgruppe.VURDU, statusEtterEndring.get().kvalifiseringsgruppe)
    }

    @Test
    fun `skal håndtere brukere som mangler oppfølgingsstatus`() {
        val statusEtterEndring = oppfolgingsStatusRepository.hentOppfolging(aktorId)
        assert(statusEtterEndring.isEmpty)
    }

    @Test
    fun `skal håndtere brukere som har oppfølging men bare ikke fått status fra arena`() {
        startOppfolgingSomArbeidsoker(aktorId)
        val statusEtterEndring = oppfolgingsStatusRepository.hentOppfolging(aktorId)
        assertEquals(true, statusEtterEndring.get().isUnderOppfolging)
        assertEquals(null, statusEtterEndring.get().hovedmaal)
        assertEquals(null, statusEtterEndring.get().formidlingsgruppe)
        assertEquals(null, statusEtterEndring.get().kvalifiseringsgruppe)
        assertEquals(null, statusEtterEndring.get().veilederId)
        assertEquals(0, statusEtterEndring.get().gjeldendeKvpId)
    }

    fun mockEnhetINorg(id: String, navn: String) {
        val enhet = Enhet().also { it.navn = navn }
        `when`(norg2Client.hentEnhet(id)).thenReturn(enhet)
    }

    fun meldingFraVeilarbArenaPåBrukerMedEnhet(fnr: Fnr, enhetId: String) {
        val record = ConsumerRecord("topic", 0, 0, "key", oppfølgingsBrukerEndret(fnr.get(), enhetId = enhetId))
        kafkaConsumerService.consumeEndringPaOppfolgingBruker(record)
    }

    fun meldingFraVeilarbArenaPåBrukerMedStatus(fnr: Fnr, formidlingsgruppe: Formidlingsgruppe, kvalifiseringsgruppe: Kvalifiseringsgruppe?, hovedmaal: Hovedmaal?, enhetId: String) {
        val record = ConsumerRecord("topic", 0, 0, "key", oppfølgingsBrukerEndret(fnr.get(), enhetId = enhetId, hovedmaal = hovedmaal, kvalifiseringsgruppe = kvalifiseringsgruppe, formidlingsgruppe = formidlingsgruppe))
        kafkaConsumerService.consumeEndringPaOppfolgingBruker(record)
    }

}
