package no.nav.veilarboppfolging.repository

import no.nav.veilarboppfolging.service.HistorikkForReaktiveringDto
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class ReaktiveringRepository(val db: NamedParameterJdbcTemplate) {

    fun insertReaktivering(historikkForReaktiveringDto: HistorikkForReaktiveringDto) {
        db.update(
            """
                INSERT INTO HISTORIKK_FOR_REAKTIVERING (aktor_id, oppfolgingsperiode, reaktivering_tidspunkt, reaktivert_av)
                VALUES(:aktor_id, :oppfolgingsperiode, CURRENT_TIMESTAMP, :reaktivert_av)
            """.trimIndent(),
            mapOf(
                "aktor_id" to historikkForReaktiveringDto.aktorId,
                "oppfolgingsperiode" to historikkForReaktiveringDto.oppfolgingsperiode,
                "reaktivert_av" to historikkForReaktiveringDto.veilederIdent
            ),
        )
    }
}