package no.nav.veilarboppfolging.repository

import no.nav.veilarboppfolging.utils.DbUtils.hentZonedDateTime
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.support.TransactionTemplate
import java.sql.ResultSet
import java.time.ZonedDateTime
import java.util.UUID

@Repository
class SakRepository(private val db: JdbcTemplate, private val transactor: TransactionTemplate) {

    fun hentSaker(oppfølgingsperiodeUUID: UUID): List<SakEntity> {
        return db.query("""
            SELECT * FROM SAK WHERE OPPFOLGINGSPERIODE_UUID = ?
        """.trimIndent(),
            SakEntity::fromResultSet,
            oppfølgingsperiodeUUID
            )
    }

}

data class SakEntity(
    val id: Long,
    val oppfølgingsperiodeUUID: UUID,
    val createdAt: ZonedDateTime,
    val status: SakStatus
) {
    companion object {
        fun fromResultSet(resultSet: ResultSet, row: Int): SakEntity = SakEntity(
            id = resultSet.getLong("id"),
            oppfølgingsperiodeUUID = UUID.fromString(resultSet.getString("uuid")),
            createdAt = hentZonedDateTime(resultSet, "created_at"),
            status = SakStatus.valueOf(resultSet.getString("status"))
        )
    }
}

enum class SakStatus {
    OPPRETTET
}
