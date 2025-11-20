package no.nav.veilarboppfolging.repository

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.util.UUID

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
                    INSERT INTO ao_kontor (ident, aktor_id, oppfolgingsperiode_id, kontor_id) VALUES (:ident, :aktorId, :oppfolgingsperiodeId, :kontorId)
                    ON CONFLICT (ident) DO UPDATE SET kontor_id = EXCLUDED.kontor_id, oppfolgingsperiode_id = EXCLUDED.oppfolgingsperiode_id
                
                """.trimIndent(), params
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
}