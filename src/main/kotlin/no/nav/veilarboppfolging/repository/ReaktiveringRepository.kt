package no.nav.veilarboppfolging.repository

import no.nav.common.types.identer.AktorId
import no.nav.veilarboppfolging.service.ReaktiverOppfolgingDto
import no.nav.veilarboppfolging.utils.DbUtils.hentZonedDateTime
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.ZonedDateTime

@Repository
class ReaktiveringRepository(val db: NamedParameterJdbcTemplate) {

    fun insertReaktivering(reaktiverOppfolgingDto: ReaktiverOppfolgingDto) {
        db.update(
            """
                INSERT INTO HISTORIKK_FOR_REAKTIVERING (aktor_id, oppfolgingsperiode, reaktivering_tidspunkt, reaktivert_av)
                VALUES(:aktor_id, :oppfolgingsperiode, CURRENT_TIMESTAMP, :reaktivert_av)
            """.trimIndent(),
            mapOf(
                "aktor_id" to reaktiverOppfolgingDto.aktorId.toString(),
                "oppfolgingsperiode" to reaktiverOppfolgingDto.oppfolgingsperiode,
                "reaktivert_av" to reaktiverOppfolgingDto.veilederIdent
            ),
        )
    }

    open fun hentReaktiveringer(aktorId: AktorId): List<ReaktiverOppfolgingHendelseEntity> {
        return db.query(
            """
            SELECT * FROM HISTORIKK_FOR_REAKTIVERING WHERE aktor_id = :aktorId
        """.trimIndent(),
            mapOf("aktorId" to aktorId.toString()),
            ReaktiverOppfolgingHendelseEntity::fromResultSet
        )
    }
}

data class ReaktiverOppfolgingHendelseEntity(
    val aktorId: String,
    val reaktiveringTidspunkt: ZonedDateTime,
    val reaktivertAv: String,
) {
    companion object {
        fun fromResultSet(resultSet: ResultSet, row: Int): ReaktiverOppfolgingHendelseEntity =
            ReaktiverOppfolgingHendelseEntity(
                aktorId = resultSet.getString("aktor_Id"),
                reaktiveringTidspunkt = hentZonedDateTime(resultSet, "reaktivering_tidspunkt"),
                reaktivertAv = resultSet.getString("reaktivert_av"),
            )
    }
}