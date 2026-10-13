package no.nav.veilarboppfolging.arbeidssokerperiode

import java.sql.Timestamp
import no.nav.common.types.identer.AktorId
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class AktivArbeidssokerperiodeRepository(
    private val db: NamedParameterJdbcTemplate
) {
    fun lagreAktivArbeidssokerperiode(aktivArbeidssokerperiode: AktivArbeidssokerperiode) {
        val sql = """
            INSERT INTO aktive_arbeidssokerperioder(arbeidssokerperiode_id, aktor_id, fnr, startet_tidspunkt)
            VALUES (:arbeidssokerperiodeId, :aktorId, :fnr, :startetTidspunkt)
            ON CONFLICT (arbeidssokerperiode_id) 
            DO UPDATE SET updated_at = current_timestamp, aktor_id = :aktorId, fnr = :fnr, startet_tidspunkt = :startetTidspunkt
        """.trimIndent()
        db.update(
            sql, mapOf(
                "arbeidssokerperiodeId" to aktivArbeidssokerperiode.arbeidssokerperiodeId,
                "aktorId" to aktivArbeidssokerperiode.aktorId.get(),
                "fnr" to aktivArbeidssokerperiode.fnr.get(),
                "startetTidspunkt" to Timestamp.from(aktivArbeidssokerperiode.startetTidspunkt),
            )
        )
    }

    fun slettAktivArbeidssokerperiode(arbeidssokerperiodeId: String) {
        val sql = """
            DELETE FROM aktive_arbeidssokerperioder
            WHERE arbeidssokerperiode_id = :arbeidssokerperiodeId
        """.trimIndent()
        db.update(sql, mapOf("arbeidssokerperiodeId" to arbeidssokerperiodeId))
    }

    fun erArbeidssoker(aktorId: AktorId): Boolean {
        val sql = """
            SELECT 1 FROM aktive_arbeidssokerperioder WHERE aktor_id = :aktorId
        """.trimIndent()
        return db.query(sql, mapOf("aktorId" to aktorId.get()))
        { _, _ -> true }
            .firstOrNull()
            ?: false
    }
}