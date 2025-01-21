package no.nav.veilarboppfolging.repository

import no.nav.common.types.identer.AktorId
import no.nav.veilarboppfolging.utils.SecureLog.secureLog
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class EnhetRepository(val db: NamedParameterJdbcTemplate) {

    private val logger = LoggerFactory.getLogger(EnhetRepository::class.java)
    
    fun hentEnhet(aktorId: AktorId): String? {
        val params = mapOf("aktorId" to aktorId.get())
        val sql = "SELECT enhet FROM OPPFOLGINGSTATUS WHERE aktor_id = :aktorId"
        return db.query(sql, params) { rs, _ -> rs.getString("enhet") }.firstOrNull()
    }

    fun setEnhet(aktorId: AktorId, enhet: String) {
        val params = mapOf(
            "aktorId" to aktorId.get(),
            "enhet" to enhet
        )
        val sql = "UPDATE OPPFOLGINGSTATUS SET enhet = :enhet WHERE aktor_id = :aktorId"

        val wasEffected = db.update(sql, params)
        if(wasEffected == 0) {
            logger.warn("Oppfølgingsenhet ble ikke oppdatert")
            secureLog.warn("Oppfølgingsenhet ble ikke oppdatert for aktorId=$aktorId")
        }
    }

}