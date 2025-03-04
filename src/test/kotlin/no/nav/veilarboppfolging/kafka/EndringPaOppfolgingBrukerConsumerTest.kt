package no.nav.veilarboppfolging.kafka

import no.nav.common.client.norg2.Enhet
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.pto_schema.enums.arena.Hovedmaal
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe
import no.nav.veilarboppfolging.IntegrationTest
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolgingsBruker
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolgingsStatus
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClient
import no.nav.veilarboppfolging.kafka.TestUtils.oppfølgingsBrukerEndret
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.GetOppfolginsstatusFailure
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.GetOppfolginsstatusSuccess
import no.nav.veilarboppfolging.service.KafkaConsumerService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EndringPaOppfolgingBrukerConsumerTest: IntegrationTest() {

    @Autowired
    private lateinit var kafkaConsumerService: KafkaConsumerService

    @MockitoBean
    lateinit var veilarbarenaClient: VeilarbarenaClient

    val fnr = Fnr.of("12345678910")
    val aktorId = AktorId.of("123")
    val veilederOid = UUID.randomUUID()

    @BeforeAll
    fun beforeAll() {
        mockInternBrukerAuthOk(veilederOid, aktorId, fnr)
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

    private fun arena_sier_KAN_reaktiveres() {
        val arenaOppfolging = VeilarbArenaOppfolgingsStatus()
            .setServicegruppe("VURDU")
            .setFormidlingsgruppe("ISERV")
            .setKanEnkeltReaktiveres(true)
            .setOppfolgingsenhet("8989")
        `when`(veilarbarenaClient.getArenaOppfolgingsstatus(fnr)).thenReturn(Optional.of(arenaOppfolging))
    }

    private fun arena_sier_kan_IKKE_reaktiveres() {
        val arenaOppfolging = VeilarbArenaOppfolgingsStatus()
            .setServicegruppe("VURDU")
            .setFormidlingsgruppe("ISERV")
            .setKanEnkeltReaktiveres(false)
            .setOppfolgingsenhet("8989")
        `when`(veilarbarenaClient.getArenaOppfolgingsstatus(fnr)).thenReturn(Optional.of(arenaOppfolging))
    }

    @Test
    fun `skal starte oppfølging på syfo-bruker når 14a i arena`() {
        mockEnhetINorg("8989", "Nav enhet")

        val localStatus0 = oppfolgingsStatusRepository.hentOppfolging(aktorId)
        assert(localStatus0.isEmpty)

        meldingFraVeilarbArenaPåBrukerMedStatus(
            fnr = fnr,
            enhetId = "8989",
            hovedmaal = null,
            formidlingsgruppe = Formidlingsgruppe.IARBS,
            kvalifiseringsgruppe = Kvalifiseringsgruppe.VURDI // Sykemeldt oppfølging på arbeidsplassen
        )

        val localStatus1 = oppfolgingsStatusRepository.hentOppfolging(aktorId)
        assert(localStatus1.isPresent) { "Oppfolgingsstatus fra arena var null" }
        assertFalse(localStatus1.get().isUnderOppfolging, "Skulle ikke vært under oppfølging")
        meldingFraVeilarbArenaPåBrukerMedStatus(
            fnr = fnr,
            enhetId = "8989",
            hovedmaal = Hovedmaal.BEHOLDEA,
            formidlingsgruppe = Formidlingsgruppe.IARBS,
            kvalifiseringsgruppe = Kvalifiseringsgruppe.BATT
        )
        val localStatus2 = oppfolgingsStatusRepository.hentOppfolging(aktorId)
        assert(localStatus2.isPresent) { "Oppfolgingsstatus fra arena var null" }
        assertTrue(localStatus2.get().isUnderOppfolging, "Skulle vært under oppfølging")
    }

    @Test
    fun `skal ikke utmeldes hvis arena sier kanReaktiveres selv om kanIkkeReaktiveres lokalt skulle tilsi det`() {
        mockEnhetINorg("8989", "Nav enhet")

        meldingFraVeilarbArenaPåBrukerMedStatus(
            fnr = fnr,
            enhetId = "8989",
            hovedmaal = null,
            formidlingsgruppe = Formidlingsgruppe.ARBS,
            kvalifiseringsgruppe = Kvalifiseringsgruppe.VURDU,
        )

        arena_sier_KAN_reaktiveres()

        erSystemBruker()
        meldingFraVeilarbArenaPåBrukerMedStatus(
            fnr = fnr,
            enhetId = "8989",
            hovedmaal = null,
            formidlingsgruppe = Formidlingsgruppe.ISERV,
            kvalifiseringsgruppe = Kvalifiseringsgruppe.VURDU,
            iservFraDato = LocalDate.now().minusDays(1)
        )

        val statusEtterEndring = oppfolgingsStatusRepository.hentOppfolging(aktorId)
        assert(statusEtterEndring.isPresent)
        assertThat(statusEtterEndring.get().isUnderOppfolging).isTrue()
    }

    @Test
    fun `skal utmeldes hvis arena sier ikke kanReaktiveres + ISERV selv om kanIkkeReaktiveres lokalt skulle tilsi det motsatte`() {
        mockEnhetINorg("8989", "Nav enhet")

        meldingFraVeilarbArenaPåBrukerMedStatus(
            fnr = fnr,
            enhetId = "8989",
            hovedmaal = null,
            formidlingsgruppe = Formidlingsgruppe.ARBS,
            kvalifiseringsgruppe = Kvalifiseringsgruppe.VURDU,
        )

        arena_sier_kan_IKKE_reaktiveres()

        erSystemBruker()
        meldingFraVeilarbArenaPåBrukerMedStatus(
            fnr = fnr,
            enhetId = "8989",
            hovedmaal = null,
            formidlingsgruppe = Formidlingsgruppe.ISERV,
            kvalifiseringsgruppe = Kvalifiseringsgruppe.VURDU,
            iservFraDato = LocalDate.now().minusDays(1)
        )

        val statusEtterEndring = oppfolgingsStatusRepository.hentOppfolging(aktorId)
        assert(statusEtterEndring.isPresent)
        assertThat(statusEtterEndring.get().isUnderOppfolging).isFalse()
    }

    private fun erSystemBruker() {
        `when`(authContextHolder.erInternBruker()).thenReturn(false)
        `when`(authContextHolder.erEksternBruker()).thenReturn(false)
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
