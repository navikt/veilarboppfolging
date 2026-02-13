package no.nav.veilarboppfolging.repository

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class ArbeidsoppfolgingskontorRepository(private val db: NamedParameterJdbcTemplate) {

    fun settNavKontor(fnr: String, aktorId: String, oppfolgingsperiodeId: UUID, kontorId: String): Int {
        val params = MapSqlParameterSource()
            .addValue("ident", fnr)
            .addValue("aktorId", aktorId)
            .addValue("oppfolgingsperiodeId", oppfolgingsperiodeId)
            .addValue("kontorId", kontorId)

        return db.update(
            """
                    INSERT INTO ao_kontor (oppfolgingsperiode_id, ident, aktor_id, kontor_id)
                    VALUES (:oppfolgingsperiodeId, :ident, :aktorId, :kontorId)
                    ON CONFLICT (oppfolgingsperiode_id)
                    DO UPDATE SET ident = EXCLUDED.ident, aktor_id = EXCLUDED.aktor_id, kontor_id = EXCLUDED.kontor_id, updated_at = CURRENT_TIMESTAMP
            """.trimIndent(),
            params
        )
    }

    fun slettNavKontor(oppfolgingsperiodeId: UUID): Int {
        val params = MapSqlParameterSource()
            .addValue("oppfolgingsperiodeId", oppfolgingsperiodeId)

        return db.update(
            """
                    DELETE FROM ao_kontor WHERE oppfolgingsperiode_id = :oppfolgingsperiodeId
                """.trimIndent(), params
        )
    }

    fun hentNavKontor(aktorId: String): String? {
        val params = MapSqlParameterSource()
            .addValue("aktor_id", aktorId)

        return db.query(
            """
                    SELECT kontor_id FROM ao_kontor WHERE aktor_id = :aktor_id
                """.trimIndent(), params
        ) { rs, _ -> rs.getString("kontor_id") }.firstOrNull()
    }
}