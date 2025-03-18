package no.nav.veilarboppfolging.repository

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EnhetId
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class EnhetRepository(val db: NamedParameterJdbcTemplate) {

    fun hentEnhet(aktorId: AktorId): EnhetId? {
        val params = mapOf("aktorId" to aktorId.get())
        val sql = "SELECT oppfolgingsenhet FROM OPPFOLGINGSTATUS WHERE aktor_id = :aktorId"
        return db.query(sql, params) { rs, _ -> rs.getString("oppfolgingsenhet")?.let { EnhetId.of(it) } }.firstOrNull()
    }

}