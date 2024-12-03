package no.nav.veilarboppfolging.kafka

import no.nav.common.client.norg2.Enhet
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.pto_schema.enums.arena.Hovedmaal
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe
import no.nav.veilarboppfolging.IntegrationTest
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolgingsBruker
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClient
import no.nav.veilarboppfolging.kafka.TestUtils.oppfølgingsBrukerEndret
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.GetOppfolginsstatusFailure
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.GetOppfolginsstatusSuccess
import no.nav.veilarboppfolging.service.KafkaConsumerService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ActiveProfiles("local")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EndringPaOppfolgingBrukerConsumerTest: IntegrationTest() {

    @Autowired
    private lateinit var kafkaConsumerService: KafkaConsumerService

    @MockBean
    lateinit var veilarbarenaClient: VeilarbarenaClient

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

        val skalVæreEnhetVest = arenaOppfolgingService.hentArenaOppfolgingsEnhet(fnr)
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

        val statusEtterEndring = arenaOppfolgingService.hentArenaOppfolginsstatusMedHovedmaal(fnr) as GetOppfolginsstatusSuccess
        assertEquals(hovedmaal.name, statusEtterEndring.result.hovedmaalkode)
        assertEquals(formidlingsgruppe.name, statusEtterEndring.result.formidlingsgruppe)
        assertEquals(kvalifiseringsgruppe.name, statusEtterEndring.result.servicegruppe)

        val oppfolgingsTilstand = arenaOppfolgingService.hentArenaOppfolgingTilstand(fnr).get()
        assertEquals(formidlingsgruppe.name, oppfolgingsTilstand.formidlingsgruppe)
        assertEquals(kvalifiseringsgruppe.name, oppfolgingsTilstand.servicegruppe)
        assertEquals(kvalifiseringsgruppe.name, oppfolgingsTilstand.servicegruppe)
        assertNull(oppfolgingsTilstand.inaktiveringsdato)
    }

    @Test
    fun `skal lagre arena-oppfølgingsdata på bruker selvom man ikke er under oppfølging ifølge arena-data`() {
        val hovedmaal = Hovedmaal.BEHOLDEA
        val formidlingsgruppe = Formidlingsgruppe.ISERV
        val kvalifiseringsgruppe = Kvalifiseringsgruppe.VURDU
        mockEnhetINorg("8989", "Nav enhet")

        val oppfolgingFørEndring = arenaOppfolgingService.hentArenaOppfolgingTilstand(fnr)
        assert(oppfolgingFørEndring.isEmpty) { "Ny bruker skal IKKE ha oppfølgingsstatus" }

        meldingFraVeilarbArenaPåBrukerMedStatus(
            fnr = fnr,
            enhetId = "8989",
            hovedmaal = hovedmaal,
            formidlingsgruppe = formidlingsgruppe,
            kvalifiseringsgruppe = kvalifiseringsgruppe,
            iservFraDato = LocalDate.of(2024,12,1)
        )

        val statusEtterEndring = arenaOppfolgingService.hentArenaOppfolginsstatusMedHovedmaal(fnr)
        assert(statusEtterEndring is GetOppfolginsstatusSuccess) { "skal ha oppfølgingststatus når dem er kommet inn via topic" }
        assertEquals(hovedmaal.name, (statusEtterEndring as GetOppfolginsstatusSuccess).result.hovedmaalkode)
        assertEquals(formidlingsgruppe.name, (statusEtterEndring as GetOppfolginsstatusSuccess).result.formidlingsgruppe)
        assertEquals(kvalifiseringsgruppe.name, (statusEtterEndring as GetOppfolginsstatusSuccess).result.servicegruppe)
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
        assertEquals(null, statusEtterEndring.get().localArenaOppfolging.get().hovedmaal)
        assertEquals(Formidlingsgruppe.IARBS, statusEtterEndring.get().localArenaOppfolging.get().formidlingsgruppe)
        assertEquals(Kvalifiseringsgruppe.VURDU, statusEtterEndring.get().localArenaOppfolging.get().kvalifiseringsgruppe)
    }

    @Test
    fun `skal håndtere brukere som mangler oppfølgingsstatus`() {
        val oppfolgingsStatus = arenaOppfolgingService.hentArenaOppfolgingTilstand(fnr)
        assert(oppfolgingsStatus.isEmpty)
        val arenaOppfolginsStatus = arenaOppfolgingService.hentArenaOppfolginsstatusMedHovedmaal(fnr)
        assert(arenaOppfolginsStatus is GetOppfolginsstatusFailure)
    }

    @Test
    fun `skal bruke veilarbarena som fallback til oppfølgingsenhet`() {
        val arenaEnhet = "6112"
        val arenaOppfolging = VeilarbArenaOppfolgingsBruker()
            .setNav_kontor(arenaEnhet)
        `when`(veilarbarenaClient.hentOppfolgingsbruker(fnr)).thenReturn(Optional.of(arenaOppfolging))
        val enhet = arenaOppfolgingService.hentArenaOppfolgingsEnhetId(fnr)
        assertEquals(arenaEnhet, enhet?.get())
    }

    @Test
    fun `skal håndtere brukere som har oppfølging men bare ikke fått status fra arena`() {
        startOppfolgingSomArbeidsoker(aktorId)

        val statusEtterEndring = oppfolgingsStatusRepository.hentOppfolging(aktorId)
        assertEquals(true, statusEtterEndring.get().isUnderOppfolging)
        assertTrue(statusEtterEndring.get().localArenaOppfolging.isEmpty)
        assertEquals(null, statusEtterEndring.get().veilederId)
        assertEquals(0, statusEtterEndring.get().gjeldendeKvpId)

        val oppfolgingsTilstand = arenaOppfolgingService.hentArenaOppfolgingTilstand(fnr)
        assert(oppfolgingsTilstand.isEmpty)

        val oppfolgingsStatus = arenaOppfolgingService.hentArenaOppfolginsstatusMedHovedmaal(fnr)
        assert(oppfolgingsStatus is GetOppfolginsstatusFailure)
    }

    fun mockEnhetINorg(id: String, navn: String) {
        val enhet = Enhet().also { it.navn = navn }
        `when`(norg2Client.hentEnhet(id)).thenReturn(enhet)
    }

    fun meldingFraVeilarbArenaPåBrukerMedEnhet(fnr: Fnr, enhetId: String) {
        val record = ConsumerRecord("topic", 0, 0, "key", oppfølgingsBrukerEndret(fnr.get(), enhetId = enhetId))
        kafkaConsumerService.consumeEndringPaOppfolgingBruker(record)
    }

    fun meldingFraVeilarbArenaPåBrukerMedStatus(fnr: Fnr, formidlingsgruppe: Formidlingsgruppe, kvalifiseringsgruppe: Kvalifiseringsgruppe?, hovedmaal: Hovedmaal?, enhetId: String, iservFraDato: LocalDate? = null) {
        val record = ConsumerRecord("topic", 0, 0, "key", oppfølgingsBrukerEndret(fnr.get(), enhetId = enhetId, hovedmaal = hovedmaal, kvalifiseringsgruppe = kvalifiseringsgruppe, formidlingsgruppe = formidlingsgruppe, iservFraDato = iservFraDato))
        kafkaConsumerService.consumeEndringPaOppfolgingBruker(record)
    }

}
