package no.nav.veilarboppfolging.service

import no.nav.common.json.JsonUtils
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SisteOppfolgingsperiodeDtoTest {

    private val testOppfolgingsperiodeUuid = UUID.fromString("12345678-1234-1234-1234-123456789012")
    private val testAktorId = "1234567890123"
    private val testIdent = "12345678901"
    private val testStartTidspunkt = ZonedDateTime.parse("2024-01-15T10:30:00+01:00")
    private val testSluttTidspunkt = ZonedDateTime.parse("2024-06-20T15:45:00+02:00")

    @Test
    fun `GjeldendeOppfolgingsperiode with OPPFOLGING_STARTET serializes correctly`() {
        val kontor = KontorDto(
            kontorNavn = "NAV Oslo",
            kontorId = "0219"
        )

        val dto = GjeldendeOppfolgingsperiode(
            oppfolgingsperiodeId = testOppfolgingsperiodeUuid,
            startTidspunkt = testStartTidspunkt,
            kontor = kontor,
            aktorId = testAktorId,
            ident = testIdent,
            sisteEndringsType = SisteEndringsType.OPPFOLGING_STARTET
        )

        val json = JsonUtils.toJson(dto)
        val jsonNode = JsonUtils.getMapper().readTree(json)

        assertEquals("OPPFOLGING_STARTET", jsonNode["sisteEndringsType"].asText())
        assertEquals(testOppfolgingsperiodeUuid.toString(), jsonNode["oppfolgingsperiodeUuid"].asText())
        assertEquals(testAktorId, jsonNode["aktorId"].asText())
        assertEquals(testIdent, jsonNode["ident"].asText())
        assertEquals(testStartTidspunkt, ZonedDateTime.parse(jsonNode["startTidspunkt"].asText()))
        assertTrue(jsonNode["sluttTidspunkt"].isNull, "sluttTidspunkt should be null for active period")
        assertEquals("NAV Oslo", jsonNode["kontor"]["kontorNavn"].asText())
        assertEquals("0219", jsonNode["kontor"]["kontorId"].asText())
        assertTrue(jsonNode.has("producerTimestamp"), "producerTimestamp should be present")
    }

    @Test
    fun `GjeldendeOppfolgingsperiode with ARBEIDSOPPFOLGINGSKONTOR_ENDRET serializes correctly`() {
        val kontor = KontorDto(
            kontorNavn = "NAV Bergen",
            kontorId = "1201"
        )

        val dto = GjeldendeOppfolgingsperiode(
            oppfolgingsperiodeId = testOppfolgingsperiodeUuid,
            startTidspunkt = testStartTidspunkt,
            kontor = kontor,
            aktorId = testAktorId,
            ident = testIdent,
            sisteEndringsType = SisteEndringsType.ARBEIDSOPPFOLGINGSKONTOR_ENDRET
        )

        val json = JsonUtils.toJson(dto)
        val jsonNode = JsonUtils.getMapper().readTree(json)

        assertEquals("ARBEIDSOPPFOLGINGSKONTOR_ENDRET", jsonNode["sisteEndringsType"].asText())
        assertEquals(testOppfolgingsperiodeUuid.toString(), jsonNode["oppfolgingsperiodeUuid"].asText())
        assertEquals(testAktorId, jsonNode["aktorId"].asText())
        assertEquals(testIdent, jsonNode["ident"].asText())
        assertEquals(testStartTidspunkt, ZonedDateTime.parse(jsonNode["startTidspunkt"].asText()))
        assertTrue(jsonNode["sluttTidspunkt"].isNull, "sluttTidspunkt should be null for active period")
        assertEquals("NAV Bergen", jsonNode["kontor"]["kontorNavn"].asText())
        assertEquals("1201", jsonNode["kontor"]["kontorId"].asText())
        assertTrue(jsonNode.has("producerTimestamp"), "producerTimestamp should be present")
    }

    @Test
    fun `AvsluttetOppfolgingsperiode serializes correctly`() {
        val dto = AvsluttetOppfolgingsperiode(
            oppfolgingsperiodeId = testOppfolgingsperiodeUuid,
            startTidspunkt = testStartTidspunkt,
            sluttTidspunkt = testSluttTidspunkt,
            aktorId = testAktorId,
            ident = testIdent
        )

        val json = JsonUtils.toJson(dto)
        val jsonNode = JsonUtils.getMapper().readTree(json)

        assertEquals("OPPFOLGING_AVSLUTTET", jsonNode["sisteEndringsType"].asText())
        assertEquals(testOppfolgingsperiodeUuid.toString(), jsonNode["oppfolgingsperiodeUuid"].asText())
        assertEquals(testAktorId, jsonNode["aktorId"].asText())
        assertEquals(testIdent, jsonNode["ident"].asText())
        assertEquals(testStartTidspunkt, ZonedDateTime.parse(jsonNode["startTidspunkt"].asText()))
        assertEquals(testSluttTidspunkt, ZonedDateTime.parse(jsonNode["sluttTidspunkt"].asText()))
        assertTrue(jsonNode["kontor"].isNull, "kontor should be null for closed period")
        assertTrue(jsonNode.has("producerTimestamp"), "producerTimestamp should be present")
    }
}