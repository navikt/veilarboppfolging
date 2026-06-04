package no.nav.veilarboppfolging.kandidatForUtmelding

import no.nav.common.types.identer.AktorId
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

    fun fjernKandidat(aktorId: AktorId) {
        val sql = """
            DELETE FROM kandidat_for_utmelding
            WHERE aktor_id = :aktorId
        """.trimIndent()
        db.update(sql, mapOf("aktorId" to aktorId.get()))
    }
}