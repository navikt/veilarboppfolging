package no.nav.veilarboppfolging.repository

import no.nav.common.types.identer.AktorId
import no.nav.veilarboppfolging.LocalDatabaseSingleton
import no.nav.veilarboppfolging.repository.entity.OppfolgingsenhetEndringEntity
import no.nav.veilarboppfolging.test.DbTestUtils
import org.apache.commons.lang3.RandomStringUtils
import org.junit.Assert
import org.junit.Before
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import kotlin.test.assertEquals

class OppfolgingsenhetHistorikkRepositoryTest {
    @Before
    fun cleanup() {
        DbTestUtils.cleanupTestDb()
    }

    @Test
    fun `skal inserte og hente ut endringer paa enhet som er sortert`() {
        oppfolgingsenhetHistorikkRepository.insertOppfolgingsenhetEndringForAktorId(AKTOR_ID, "5")
        oppfolgingsenhetHistorikkRepository.insertOppfolgingsenhetEndringForAktorId(AKTOR_ID, "4")
        oppfolgingsenhetHistorikkRepository.insertOppfolgingsenhetEndringForAktorId(AKTOR_ID, "3")
        oppfolgingsenhetHistorikkRepository.insertOppfolgingsenhetEndringForAktorId(AKTOR_ID, "2")
        oppfolgingsenhetHistorikkRepository.insertOppfolgingsenhetEndringForAktorId(AKTOR_ID, "1")
        oppfolgingsenhetHistorikkRepository.insertOppfolgingsenhetEndringForAktorId(AKTOR_ID, "0")

        val enhetsHistorikk = oppfolgingsenhetHistorikkRepository.hentOppfolgingsenhetEndringerForAktorId(AKTOR_ID)

        assertEquals(6, enhetsHistorikk.size.toLong())

        assertEquals("0", enhetsHistorikk[0]!!.enhet)
        assertEquals("1", enhetsHistorikk[1]!!.enhet)
        assertEquals("2", enhetsHistorikk[2]!!.enhet)
        assertEquals("3", enhetsHistorikk[3]!!.enhet)
        assertEquals("4", enhetsHistorikk[4]!!.enhet)
        assertEquals("5", enhetsHistorikk[5]!!.enhet)
    }

    companion object {
        private val AKTOR_ID: AktorId = AktorId.of(RandomStringUtils.randomNumeric(10))
        private val oppfolgingsenhetHistorikkRepository =
            OppfolgingsenhetHistorikkRepository(NamedParameterJdbcTemplate(LocalDatabaseSingleton.jdbcTemplate))
    }
}
