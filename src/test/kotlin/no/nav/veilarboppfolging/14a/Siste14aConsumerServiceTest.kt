package no.nav.veilarboppfolging.`14a`

import no.nav.common.types.identer.AktorId
import no.nav.veilarboppfolging.ident.randomAktorId
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import no.nav.veilarboppfolging.test.DbTestUtils
import no.nav.veilarboppfolging.test.IsolatedDatabaseTest
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.support.TransactionTemplate
import java.time.ZonedDateTime
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class Siste14aConsumerServiceTest: IsolatedDatabaseTest() {

    val oppfolgingsStatusRepository = OppfolgingsStatusRepository(NamedParameterJdbcTemplate(db))
    val siste14aConsumerService: Siste14aConsumerService = Siste14aConsumerService(oppfolgingsStatusRepository)


    @Test
    fun `Skal lagre siste 14a vedtak når det kommer melding på siste 14a vedtak topic`() {
        val aktorId = randomAktorId()
        val førsteMelding = ConsumerRecord("topic", 0, 0, "dummyKey", nytt14aVedtak(aktorId, Innsatsgruppe.STANDARD_INNSATS))
        val sisteMelding = ConsumerRecord("topic", 0, 0, "dummyKey", nytt14aVedtak(aktorId, Innsatsgruppe.SITUASJONSBESTEMT_INNSATS))
        oppfolgingsStatusRepository.opprettOppfolging(aktorId)

        siste14aConsumerService.consumeSiste14AVedtak(førsteMelding)
        siste14aConsumerService.consumeSiste14AVedtak(sisteMelding)

        assertEquals(
            oppfolgingsStatusRepository.hentOppfolging(aktorId)
                .get().innsatsgruppe,
            Innsatsgruppe.SITUASJONSBESTEMT_INNSATS
        )
    }

    @Test
    fun `Skal lagre siste 14a vedtak når det kommer melding på siste 14a vedtak topic`() {
        val aktorId = randomAktorId()
        val førsteMelding = ConsumerRecord("topic", 0, 0, "dummyKey", nytt14aVedtak(aktorId, Innsatsgruppe.STANDARD_INNSATS))
        val sisteMelding = ConsumerRecord("topic", 0, 0, "dummyKey", nytt14aVedtak(aktorId, Innsatsgruppe.SITUASJONSBESTEMT_INNSATS))
        oppfolgingsStatusRepository.opprettOppfolging(aktorId)
        oppfolgingsStatusRepository.(aktorId)

        siste14aConsumerService.consumeSiste14AVedtak(førsteMelding)
        siste14aConsumerService.consumeSiste14AVedtak(sisteMelding)

        assertEquals(
            oppfolgingsStatusRepository.hentOppfolging(aktorId)
                .get().innsatsgruppe,
            Innsatsgruppe.SITUASJONSBESTEMT_INNSATS
        )
    }

    private fun nytt14aVedtak(aktorId: AktorId, innsatsgruppe: Innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS): Siste14aVedtakKafkaDto {
        return Siste14aVedtakKafkaDto(
            aktorId = aktorId,
            innsatsgruppe = innsatsgruppe,
            hovedmal = HovedmalMedOkeDeltakelse.SKAFFE_ARBEID,
            fattetDato = ZonedDateTime.now(),
            fraArena = false
        )
    }

}