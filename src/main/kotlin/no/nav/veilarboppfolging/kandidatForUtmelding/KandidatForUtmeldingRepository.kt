package no.nav.veilarboppfolging.kandidatForUtmelding

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class KandidatForUtmeldingRepository(
    private val db: NamedParameterJdbcTemplate
) {

    fun lagreKandidat(hendelse: KandidatForUtmeldingHendelse) {
        val sql = """
            INSERT INTO kandidat_for_utmelding(aktor_id, hendelse)
            VALUES (:aktorId, :hendelse)
            ON CONFLICT (aktor_id) DO NOTHING
        """.trimIndent()
        db.update(sql, mapOf(
            "aktorId" to hendelse.aktorId.get(),
            "hendelse" to hendelse.type.name,
        ))
    }
}