package no.nav.veilarboppfolging.repository

import no.nav.veilarboppfolging.utils.DbUtils.hentZonedDateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.ZonedDateTime
import java.util.UUID

@Repository
open class SakRepository() {

    @field:Autowired
    lateinit var db: JdbcTemplate

    fun hentSaker(oppfølgingsperiodeUUID: UUID): List<SakEntity> {
        return db.query("""
            SELECT * FROM SAK WHERE OPPFOLGINGSPERIODE_UUID = ?
        """.trimIndent(),
            SakEntity::fromResultSet,
            oppfølgingsperiodeUUID
            )
    }

    fun opprettSak(oppfølgingsperiodeUUID: UUID) {
         db.update(
            """
                INSERT INTO SAK (oppfolgingsperiode_uuid, created_at)
                VALUES(?, CURRENT_TIMESTAMP)
            """.trimIndent(),
            oppfølgingsperiodeUUID,
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