package no.nav.veilarboppfolging.repository

import no.nav.veilarboppfolging.utils.DbUtils.hentZonedDateTime
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.jdbc.support.KeyHolder
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.ZonedDateTime
import java.util.*


@Repository
open class SakRepository(val db: NamedParameterJdbcTemplate) {


    open fun hentSaker(oppfølgingsperiodeUUID: UUID): List<SakEntity> {
        return db.query("""
            SELECT * FROM SAK WHERE OPPFOLGINGSPERIODE_UUID = :oppfølgingsperiodeUUID
        """.trimIndent(),
            mapOf("oppfølgingsperiodeUUID" to oppfølgingsperiodeUUID.toString()),
            SakEntity::fromResultSet
            )
    }

    open fun opprettSak(oppfølgingsperiodeUUID: UUID) {
        db.update(
            """
                INSERT INTO SAK (oppfolgingsperiode_uuid, created_at)
                VALUES(:oppfølgingsperiodeUUID, CURRENT_TIMESTAMP)
            """.trimIndent(),
            mapOf("oppfølgingsperiodeUUID" to oppfølgingsperiodeUUID.toString()),
        )
    }

}

data class SakEntity(
    val id: Long,
    val oppfølgingsperiodeUUID: UUID,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun fromResultSet(resultSet: ResultSet, row: Int): SakEntity = SakEntity(
            id = resultSet.getLong("id"),
            oppfølgingsperiodeUUID = UUID.fromString(resultSet.getString("oppfolgingsperiode_uuid")),
            createdAt = hentZonedDateTime(resultSet, "created_at"),
        )
    }
}