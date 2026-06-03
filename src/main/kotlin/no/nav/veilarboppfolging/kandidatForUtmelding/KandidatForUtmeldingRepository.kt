package no.nav.veilarboppfolging.kandidatForUtmelding

import no.nav.veilarboppfolging.ident.AktorId
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class KandidatForUtmeldingRepository(
    private val db: NamedParameterJdbcTemplate
) {

    fun lagreKandidat(aktorId: AktorId) {
        val sql = """
            INSERT INTO kandidat_for_utmelding(:aktorId)
        """.trimIndent()
        db.update(sql, mapOf("aktorId" to aktorId))
    }
}